package com.kstrinadka.securebankapi.integration;

import com.kstrinadka.securebankapi.exception.InsufficientFundsException;
import com.kstrinadka.securebankapi.service.TransferService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class TransferConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final Long FROM_USER_ID = 1L;
    private static final Long TO_USER_ID = 2L;
    private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("200.00");
    private static final int TASKS_COUNT = 10;

    @Autowired
    private TransferService transferService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpBalances() {
        resetDatabaseState();
    }

    @AfterEach
    void tearDownBalances() {
        resetDatabaseState();
    }

    // 10 пользователей одновременно пытаются перевести деньги
    // только 5 попыток должны быть успешными
    // баланс не должен уходить в минус
    @Test
    void parallelTransfersDoNotMakeSenderBalanceNegative() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(TASKS_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(TASKS_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<CompletableFuture<Boolean>> futures = IntStream.range(0, TASKS_COUNT)
                    .mapToObj(index -> CompletableFuture.supplyAsync(() -> executeTransfer(readyLatch, startLatch), executorService))
                    .collect(Collectors.toList());

            assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

            long successfulTransfers = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Boolean::booleanValue)
                    .count();
            long failedTransfers = TASKS_COUNT - successfulTransfers;

            assertThat(successfulTransfers).isEqualTo(5);
            assertThat(failedTransfers).isEqualTo(5);
            assertThat(balanceByUserId(FROM_USER_ID)).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(balanceByUserId(TO_USER_ID)).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(balanceByUserId(FROM_USER_ID)).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(successfulTransferCount()).isEqualTo(5);
        } finally {
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Boolean executeTransfer(CountDownLatch readyLatch, CountDownLatch startLatch) {
        readyLatch.countDown();
        try {
            startLatch.await();
            transferService.transfer(FROM_USER_ID, TO_USER_ID, TRANSFER_AMOUNT);
            return true;
        } catch (InsufficientFundsException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Transfer task was interrupted", exception);
        }
    }

    private void resetDatabaseState() {
        jdbcTemplate.update("DELETE FROM transfer");
        jdbcTemplate.update("UPDATE account SET balance = 1000.00, version = 0 WHERE user_id = 1");
        jdbcTemplate.update("UPDATE account SET balance = 500.00, version = 0 WHERE user_id = 2");
        jdbcTemplate.update("UPDATE account SET balance = 2000.00, version = 0 WHERE user_id = 3");
    }

    private BigDecimal balanceByUserId(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?",
                BigDecimal.class,
                userId
        );
    }

    private Integer successfulTransferCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfer WHERE from_user_id = ? AND to_user_id = ? AND amount = ? AND status = 'SUCCESS'",
                Integer.class,
                FROM_USER_ID,
                TO_USER_ID,
                TRANSFER_AMOUNT
        );
    }
}
