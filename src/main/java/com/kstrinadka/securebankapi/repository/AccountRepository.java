package com.kstrinadka.securebankapi.repository;

import com.kstrinadka.securebankapi.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByUserId(Long userId);

    @Query(value = "SELECT * FROM account WHERE user_id = :userId FOR UPDATE", nativeQuery = true)
    Optional<AccountEntity> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM account ORDER BY user_id FOR UPDATE", nativeQuery = true)
    List<AccountEntity> findAllForUpdate();

    /**
     * Обрабатывает один batch аккаунтов, которым пора начислить рост баланса.
     * <p>
     * Аккаунт считается готовым к начислению, если его {@code last_balance_growth_at}
     * меньше или равен {@code growthThreshold}. Запрос берет не больше {@code batchSize}
     * таких аккаунтов, начиная с самых отстающих, и пытается заблокировать их через
     * {@code FOR UPDATE SKIP LOCKED}. Если аккаунт сейчас занят другой транзакцией,
     * например переводом денег, PostgreSQL не ждет его, а пропускает и берет следующий.
     * Благодаря этому scheduler не блокирует пользовательские переводы.
     * <p>
     * Для выбранных аккаунтов применяется ровно один шаг роста: баланс увеличивается
     * на {@code growthMultiplier}, но не выше {@code initial_balance * maxBalanceMultiplier}.
     * Если баланс уже выше лимита, например из-за входящего перевода, он не уменьшается.
     * {@code last_balance_growth_at} сдвигается ровно на один интервал, чтобы аккаунт,
     * который отстал на несколько периодов, мог догнать их последовательно.
     * <p>
     * {@code version} увеличивается вручную, потому что native update обходит обычный
     * Hibernate dirty checking и автоматическое обновление {@code @Version}.
     *
     * @return количество обновленных аккаунт-периодов в текущем batch
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = "WITH locked_accounts AS ( " +
                    "    SELECT id " +
                    "    FROM account " +
                    "    WHERE last_balance_growth_at <= :growthThreshold " +
                    "    ORDER BY last_balance_growth_at, user_id " +
                    "    LIMIT :batchSize " +
                    "    FOR UPDATE SKIP LOCKED " +
                    ") " +
                    "UPDATE account a " +
                    "SET balance = CASE " +
                    "        WHEN a.balance >= ROUND(a.initial_balance * CAST(:maxBalanceMultiplier AS numeric), :moneyScale) " +
                    "            THEN a.balance " +
                    "        ELSE LEAST( " +
                    "            ROUND(a.balance * CAST(:growthMultiplier AS numeric), :moneyScale), " +
                    "            ROUND(a.initial_balance * CAST(:maxBalanceMultiplier AS numeric), :moneyScale) " +
                    "        ) " +
                    "    END, " +
                    "    last_balance_growth_at = a.last_balance_growth_at + " +
                    "        (CAST(:growthIntervalSeconds AS integer) * INTERVAL '1 second'), " +
                    "    updated_at = now(), " +
                    "    version = a.version + 1 " +
                    "FROM locked_accounts locked " +
                    "WHERE a.id = locked.id",
            nativeQuery = true
    )
    int increaseNextBalanceGrowthBatch(
            @Param("maxBalanceMultiplier") BigDecimal maxBalanceMultiplier,
            @Param("growthMultiplier") BigDecimal growthMultiplier,
            @Param("moneyScale") int moneyScale,
            @Param("growthThreshold") LocalDateTime growthThreshold,
            @Param("batchSize") int batchSize,
            @Param("growthIntervalSeconds") int growthIntervalSeconds
    );

    /**
     * Проверяет, остались ли аккаунты, которым еще нужно начислить рост баланса.
     * <p>
     * Используется после batch update, когда обновилось {@code 0} строк. Если due-аккаунты
     * все еще есть, но batch ничего не обновил, значит подходящие строки, скорее всего,
     * сейчас заблокированы другими транзакциями. В этом случае сервис может подождать
     * и повторить попытку. Если due-аккаунтов больше нет, текущий запуск роста баланса
     * можно завершать.
     */
    @Query(
            value = "SELECT EXISTS ( " +
                    "    SELECT 1 " +
                    "    FROM account " +
                    "    WHERE last_balance_growth_at <= :growthThreshold " +
                    ")",
            nativeQuery = true
    )
    boolean existsDueForBalanceGrowth(@Param("growthThreshold") LocalDateTime growthThreshold);
}
