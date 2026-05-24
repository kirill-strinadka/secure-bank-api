package com.kstrinadka.securebankapi.integration;

import com.kstrinadka.securebankapi.service.BalanceGrowthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceGrowthServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BalanceGrowthService balanceGrowthService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUpAccounts() {
        prepareAccounts();
    }

    @AfterEach
    void tearDownAccounts() {
        resetAccountsToMigrationState();
    }

    @Test
    void increaseBalancesUpdatesAccountsOnRealPostgres() {
        balanceGrowthService.increaseBalances();

        assertThat(balanceByUserId(1L)).isEqualByComparingTo(new BigDecimal("110.00"));
        assertThat(balanceByUserId(2L)).isEqualByComparingTo(new BigDecimal("121.00"));
        assertThat(balanceByUserId(3L)).isEqualByComparingTo(new BigDecimal("207.00"));
        assertThat(balanceByUserId(101L)).isEqualByComparingTo(new BigDecimal("207.00"));
        assertThat(balanceByUserId(102L)).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(balanceByUserId(103L)).isEqualByComparingTo(new BigDecimal("110.06"));
    }

    @Test
    void increaseBalancesProcessesMoreThanOneBatch() {
        insertBatchUsers(2000L, 55);

        balanceGrowthService.increaseBalances();

        Integer updatedAccounts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account WHERE user_id BETWEEN 2000 AND 2054 AND balance = 110.00",
                Integer.class
        );

        assertThat(updatedAccounts).isEqualTo(55);
    }

    @Test
    void increaseBalancesCatchesUpMissedGrowthPeriods() {
        jdbcTemplate.update(
                "UPDATE account " +
                        "SET initial_balance = 100.00, balance = 100.00, version = 0, " +
                        "last_balance_growth_at = now() - INTERVAL '91 seconds' " +
                        "WHERE user_id = 1"
        );

        balanceGrowthService.increaseBalances();

        assertThat(balanceByUserId(1L)).isEqualByComparingTo(new BigDecimal("133.10"));
    }

    @Test
    void increaseBalancesSkipsLockedAccountThenCatchesItUpAfterLockIsReleased() throws Exception {
        Connection lockedConnection = lockAccountByUserId(1L);
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            CompletableFuture<Void> balanceGrowthFuture = CompletableFuture.runAsync(
                    balanceGrowthService::increaseBalances,
                    executorService
            );

            waitUntil(() -> balanceByUserId(2L).compareTo(new BigDecimal("121.00")) == 0);
            assertThat(balanceByUserId(1L)).isEqualByComparingTo(new BigDecimal("100.00"));

            lockedConnection.commit();
            balanceGrowthFuture.get(10, TimeUnit.SECONDS);

            assertThat(balanceByUserId(1L)).isEqualByComparingTo(new BigDecimal("110.00"));
        } finally {
            lockedConnection.close();
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void prepareAccounts() {
        deleteTemporaryUsers();
        deleteBatchUsers();
        jdbcTemplate.update(
                "UPDATE account SET initial_balance = 100.00, balance = 100.00, version = 0, " +
                        "last_balance_growth_at = now() - INTERVAL '31 seconds' WHERE user_id = 1"
        );
        jdbcTemplate.update(
                "UPDATE account SET initial_balance = 100.00, balance = 110.00, version = 0, " +
                        "last_balance_growth_at = now() - INTERVAL '31 seconds' WHERE user_id = 2"
        );
        jdbcTemplate.update(
                "UPDATE account SET initial_balance = 100.00, balance = 200.00, version = 0, " +
                        "last_balance_growth_at = now() - INTERVAL '31 seconds' WHERE user_id = 3"
        );
        insertTemporaryUserWithAccount(101L, "Limit User", "207.00", "100.00");
        insertTemporaryUserWithAccount(102L, "High Balance User", "500.00", "100.00");
        insertTemporaryUserWithAccount(103L, "Rounding User", "100.05", "1000.00");
    }

    private void resetAccountsToMigrationState() {
        deleteTemporaryUsers();
        deleteBatchUsers();
        jdbcTemplate.update(
                "UPDATE account SET initial_balance = 1000.00, balance = 1000.00, version = 0, " +
                        "last_balance_growth_at = now() WHERE user_id = 1"
        );
        jdbcTemplate.update(
                "UPDATE account SET initial_balance = 500.00, balance = 500.00, version = 0, " +
                        "last_balance_growth_at = now() WHERE user_id = 2"
        );
        jdbcTemplate.update(
                "UPDATE account SET initial_balance = 2000.00, balance = 2000.00, version = 0, " +
                        "last_balance_growth_at = now() WHERE user_id = 3"
        );
    }

    private void insertTemporaryUserWithAccount(Long userId, String name, String balance, String initialBalance) {
        jdbcTemplate.update(
                "INSERT INTO users (id, name, date_of_birth, password) VALUES (?, ?, '1990-01-01', 'password')",
                userId,
                name
        );
        jdbcTemplate.update(
                "INSERT INTO account (user_id, balance, initial_balance, last_balance_growth_at) " +
                        "VALUES (?, ?::numeric, ?::numeric, now() - INTERVAL '31 seconds')",
                userId,
                balance,
                initialBalance
        );
    }

    private void insertBatchUsers(Long firstUserId, int count) {
        for (long userId = firstUserId; userId < firstUserId + count; userId++) {
            jdbcTemplate.update(
                    "INSERT INTO users (id, name, date_of_birth, password) VALUES (?, ?, '1990-01-01', 'password')",
                    userId,
                    "Batch User " + userId
            );
            jdbcTemplate.update(
                    "INSERT INTO account (user_id, balance, initial_balance, last_balance_growth_at) " +
                            "VALUES (?, 100.00, 100.00, now() - INTERVAL '31 seconds')",
                    userId
            );
        }
    }

    private void deleteTemporaryUsers() {
        jdbcTemplate.update("DELETE FROM transfer WHERE from_user_id IN (101, 102, 103) OR to_user_id IN (101, 102, 103)");
        jdbcTemplate.update("DELETE FROM account WHERE user_id IN (101, 102, 103)");
        jdbcTemplate.update("DELETE FROM users WHERE id IN (101, 102, 103)");
    }

    private void deleteBatchUsers() {
        jdbcTemplate.update("DELETE FROM transfer WHERE from_user_id BETWEEN 2000 AND 2054 OR to_user_id BETWEEN 2000 AND 2054");
        jdbcTemplate.update("DELETE FROM account WHERE user_id BETWEEN 2000 AND 2054");
        jdbcTemplate.update("DELETE FROM users WHERE id BETWEEN 2000 AND 2054");
    }

    private BigDecimal balanceByUserId(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?",
                BigDecimal.class,
                userId
        );
    }

    private Connection lockAccountByUserId(Long userId) throws Exception {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);

        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM account WHERE user_id = ? FOR UPDATE")) {
            statement.setLong(1, userId);
            statement.executeQuery().close();
        }

        return connection;
    }

    private void waitUntil(BooleanSupplier condition) throws InterruptedException {
        LocalDateTime deadline = LocalDateTime.now().plusSeconds(10);
        while (LocalDateTime.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Condition was not met before timeout");
    }
}
