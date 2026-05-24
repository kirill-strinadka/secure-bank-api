package com.kstrinadka.securebankapi.repository;

import com.kstrinadka.securebankapi.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            value = "UPDATE account " +
                    "SET balance = LEAST(ROUND(balance * 1.10, 2), ROUND(initial_balance * 2.07, 2)), " +
                    "updated_at = now(), " +
                    "version = version + 1 " +
                    "WHERE balance < ROUND(initial_balance * 2.07, 2)",
            nativeQuery = true
    )
    int increaseBalancesByTenPercentWithCap();
}
