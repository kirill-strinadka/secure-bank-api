package com.kstrinadka.securebankapi.integration;

import com.kstrinadka.securebankapi.service.BalanceGrowthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceGrowthConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final long LOCKED_USER_ID_FROM = 3000L;
    private static final long LOCKED_USER_ID_TO = 3004L;
    private static final long UNLOCKED_USER_ID_FROM = 3005L;
    private static final long UNLOCKED_USER_ID_TO = 3064L;
    private static final int LOCKED_ACCOUNTS_COUNT = 5;
    private static final int UNLOCKED_ACCOUNTS_COUNT = 60;
    private static final int PARALLEL_BALANCE_GROWTH_RUNS = 3;

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100.00");
    private static final BigDecimal UNLOCKED_EXPECTED_BALANCE = new BigDecimal("121.00");
    private static final BigDecimal LOCKED_EXPECTED_BALANCE = new BigDecimal("133.10");

    @Autowired
    private BalanceGrowthService balanceGrowthService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUpAccounts() {
        deleteTestUsers();
        LocalDateTime cursorReference = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        insertAccounts(LOCKED_USER_ID_FROM, LOCKED_USER_ID_TO, cursorReference.minusSeconds(91));
        insertAccounts(UNLOCKED_USER_ID_FROM, UNLOCKED_USER_ID_TO, cursorReference.minusSeconds(61));
    }

    @AfterEach
    void tearDownAccounts() {
        deleteTestUsers();
    }

    @Test
    void parallelGrowthRunsSkipLockedAccountsAndCatchThemUpAfterLockIsReleased() throws Exception {
        Connection lockedConnection = lockAccounts(LOCKED_USER_ID_FROM, LOCKED_USER_ID_TO);
        ExecutorService executorService = Executors.newFixedThreadPool(PARALLEL_BALANCE_GROWTH_RUNS);
        boolean lockReleased = false;

        try {
            CountDownLatch readyLatch = new CountDownLatch(PARALLEL_BALANCE_GROWTH_RUNS);
            CountDownLatch startLatch = new CountDownLatch(1);

            List<CompletableFuture<Void>> futures = IntStream.range(0, PARALLEL_BALANCE_GROWTH_RUNS)
                    .mapToObj(index -> CompletableFuture.runAsync(
                            () -> runBalanceGrowthAfterStartSignal(readyLatch, startLatch),
                            executorService
                    ))
                    .collect(Collectors.toList());

            assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            waitUntil(() -> countAccountsWithBalance(
                    UNLOCKED_USER_ID_FROM,
                    UNLOCKED_USER_ID_TO,
                    UNLOCKED_EXPECTED_BALANCE
            ) == UNLOCKED_ACCOUNTS_COUNT);

            // заблокированные аккаунты не обновили баланс
            assertThat(countAccountsWithBalance(
                    LOCKED_USER_ID_FROM,
                    LOCKED_USER_ID_TO,
                    INITIAL_BALANCE
            )).isEqualTo(LOCKED_ACCOUNTS_COUNT);

            lockedConnection.commit();
            lockReleased = true;

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(20, TimeUnit.SECONDS);

            assertUnlockedAccountsReachedExpectedBalance();
            // заблокированные аккаунты обновили баланс позже
            assertLockedAccountsCaughtUpToExpectedBalance();

            // Повторный ручной запуск без ожидания нового 30-секундного периода не должен начислить лишние 10%.
            balanceGrowthService.increaseBalances();

            assertUnlockedAccountsReachedExpectedBalance();
            assertLockedAccountsCaughtUpToExpectedBalance();
        } finally {
            if (!lockReleased) {
                lockedConnection.rollback();
            }
            lockedConnection.close();
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void assertUnlockedAccountsReachedExpectedBalance() {
        assertThat(countAccountsWithBalance(
                UNLOCKED_USER_ID_FROM,
                UNLOCKED_USER_ID_TO,
                UNLOCKED_EXPECTED_BALANCE
        )).isEqualTo(UNLOCKED_ACCOUNTS_COUNT);
    }

    private void assertLockedAccountsCaughtUpToExpectedBalance() {
        assertThat(countAccountsWithBalance(
                LOCKED_USER_ID_FROM,
                LOCKED_USER_ID_TO,
                LOCKED_EXPECTED_BALANCE
        )).isEqualTo(LOCKED_ACCOUNTS_COUNT);
    }

    private void runBalanceGrowthAfterStartSignal(CountDownLatch readyLatch, CountDownLatch startLatch) {
        readyLatch.countDown();
        try {
            assertThat(startLatch.await(10, TimeUnit.SECONDS)).isTrue();
            balanceGrowthService.increaseBalances();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Balance growth task was interrupted", exception);
        }
    }

    private void insertAccounts(long userIdFrom, long userIdTo, LocalDateTime lastBalanceGrowthAt) {
        for (long userId = userIdFrom; userId <= userIdTo; userId++) {
            jdbcTemplate.update(
                    "INSERT INTO users (id, name, date_of_birth, password) VALUES (?, ?, '1990-01-01', 'password')",
                    userId,
                    "Concurrent Growth User " + userId
            );
            jdbcTemplate.update(
                    "INSERT INTO account (user_id, balance, initial_balance, last_balance_growth_at) " +
                            "VALUES (?, ?, ?, ?)",
                    userId,
                    INITIAL_BALANCE,
                    INITIAL_BALANCE,
                    lastBalanceGrowthAt
            );
        }
    }

    private Connection lockAccounts(long userIdFrom, long userIdTo) throws Exception {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM account WHERE user_id BETWEEN ? AND ? ORDER BY user_id FOR UPDATE"
        )) {
            statement.setLong(1, userIdFrom);
            statement.setLong(2, userIdTo);
            statement.executeQuery().close();
        }

        return connection;
    }

    private int countAccountsWithBalance(long userIdFrom, long userIdTo, BigDecimal balance) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account WHERE user_id BETWEEN ? AND ? AND balance = ?",
                Integer.class,
                userIdFrom,
                userIdTo,
                balance
        );
        return count == null ? 0 : count;
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

    private void deleteTestUsers() {
        jdbcTemplate.update(
                "DELETE FROM transfer WHERE " +
                        "(from_user_id BETWEEN ? AND ? OR to_user_id BETWEEN ? AND ?) OR " +
                        "(from_user_id BETWEEN ? AND ? OR to_user_id BETWEEN ? AND ?)",
                LOCKED_USER_ID_FROM,
                LOCKED_USER_ID_TO,
                LOCKED_USER_ID_FROM,
                LOCKED_USER_ID_TO,
                UNLOCKED_USER_ID_FROM,
                UNLOCKED_USER_ID_TO,
                UNLOCKED_USER_ID_FROM,
                UNLOCKED_USER_ID_TO
        );
        jdbcTemplate.update(
                "DELETE FROM account WHERE user_id BETWEEN ? AND ? OR user_id BETWEEN ? AND ?",
                LOCKED_USER_ID_FROM,
                LOCKED_USER_ID_TO,
                UNLOCKED_USER_ID_FROM,
                UNLOCKED_USER_ID_TO
        );
        jdbcTemplate.update(
                "DELETE FROM users WHERE id BETWEEN ? AND ? OR id BETWEEN ? AND ?",
                LOCKED_USER_ID_FROM,
                LOCKED_USER_ID_TO,
                UNLOCKED_USER_ID_FROM,
                UNLOCKED_USER_ID_TO
        );
    }
}
