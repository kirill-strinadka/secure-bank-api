package com.kstrinadka.securebankapi.repository;

import com.kstrinadka.securebankapi.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByUserId(Long userId);

    @Query(value = "SELECT * FROM account WHERE user_id = :userId FOR UPDATE", nativeQuery = true)
    Optional<AccountEntity> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM account ORDER BY user_id FOR UPDATE", nativeQuery = true)
    List<AccountEntity> findAllForUpdate();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = "WITH locked_accounts AS ( " +
                    "    SELECT id " +
                    "    FROM account " +
                    "    WHERE balance < ROUND(initial_balance * CAST(:maxBalanceMultiplier AS numeric), :moneyScale) " +
                    "    ORDER BY user_id " +
                    "    FOR UPDATE " +
                    ") " +
                    "UPDATE account a " +
                    "SET balance = LEAST( " +
                    "        ROUND(a.balance * CAST(:growthMultiplier AS numeric), :moneyScale), " +
                    "        ROUND(a.initial_balance * CAST(:maxBalanceMultiplier AS numeric), :moneyScale) " +
                    "    ), " +
                    "    updated_at = now(), " +
                    "    version = a.version + 1 " +
                    "FROM locked_accounts locked " +
                    "WHERE a.id = locked.id",
            nativeQuery = true
    )
    int increaseBalancesByTenPercentWithCap(
            @Param("maxBalanceMultiplier") BigDecimal maxBalanceMultiplier,
            @Param("growthMultiplier") BigDecimal growthMultiplier,
            @Param("moneyScale") int moneyScale
    );
}
