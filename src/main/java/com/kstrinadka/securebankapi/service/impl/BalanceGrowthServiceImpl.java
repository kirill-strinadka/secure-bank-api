package com.kstrinadka.securebankapi.service.impl;

import com.kstrinadka.securebankapi.service.BalanceGrowthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceGrowthServiceImpl implements BalanceGrowthService {

    private static final BigDecimal MAX_BALANCE_MULTIPLIER = new BigDecimal("2.07");
    private static final BigDecimal GROWTH_MULTIPLIER = new BigDecimal("1.10");
    private static final int MONEY_SCALE = 2;
    private static final int BATCH_SIZE = 50;
    private static final Duration GROWTH_INTERVAL = Duration.ofSeconds(30);
    private static final Duration LOCK_RETRY_DELAY = Duration.ofMillis(500);
    private static final int MAX_NO_PROGRESS_ATTEMPTS = 60;

    private final BalanceGrowthBatchProcessor batchProcessor;

    /**
     * Запускает полный проход начисления роста балансов.
     * <p>
     * Метод не открывает одну большую транзакцию на весь проход. Вместо этого он
     * вызывает batch processor, где каждый batch выполняется в отдельной короткой
     * транзакции. Так мы держим блокировки недолго и меньше мешаем переводам денег.
     * <p>
     * За один batch обновляется максимум {@code BATCH_SIZE} due-аккаунтов. Если batch
     * ничего не обновил, но due-аккаунты еще есть, значит они, скорее всего, сейчас
     * заблокированы другими транзакциями. Тогда метод ждет небольшую паузу и пробует
     * снова. Если попытки без прогресса закончились, текущий запуск останавливается,
     * а оставшиеся аккаунты будут догнаны следующим запуском scheduler.
     */
    @Override
    public void increaseBalances() {
        LocalDateTime runStartedAt = LocalDateTime.now();
        // Если last_balance_growth_at <= growthThreshold, значит аккаунт должен получить рост баланса на 10%
        LocalDateTime growthThreshold = runStartedAt.minus(GROWTH_INTERVAL);
        int noProgressAttempts = 0;
        int totalUpdated = 0;

        while (true) {
            int updated = increaseNextBatch(growthThreshold);

            if (updated > 0) {
                totalUpdated += updated;
                noProgressAttempts = 0;
                continue;
            }

            if (!batchProcessor.hasDueAccounts(growthThreshold)) {
                log.info("Balance growth finished. Updated account-periods: {}", totalUpdated);
                return;
            }

            noProgressAttempts++;
            if (noProgressAttempts >= MAX_NO_PROGRESS_ATTEMPTS) {
                log.warn(
                        "Balance growth stopped after {} no-progress attempts. " +
                                "Some due accounts are probably locked and will be retried on the next scheduler run.",
                        noProgressAttempts
                );
                return;
            }

            waitBeforeRetry();
        }
    }

    private int increaseNextBatch(LocalDateTime growthThreshold) {
        try {
            return batchProcessor.increaseNextBatch(
                    MAX_BALANCE_MULTIPLIER,
                    GROWTH_MULTIPLIER,
                    MONEY_SCALE,
                    growthThreshold,
                    BATCH_SIZE,
                    (int) GROWTH_INTERVAL.getSeconds()
            );
        } catch (TransientDataAccessException exception) {
            log.warn("Transient database error during balance growth batch. The batch will be retried.", exception);
            return 0;
        }
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(LOCK_RETRY_DELAY.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Balance growth was interrupted while waiting to retry locked accounts", exception);
        }
    }
}
