package com.kstrinadka.securebankapi.scheduler;

import com.kstrinadka.securebankapi.service.BalanceGrowthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "app.balance-growth.scheduler",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BalanceGrowthScheduler {

    private final BalanceGrowthService balanceGrowthService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedRateString = "${app.balance-growth.scheduler.fixed-rate:30000}")
    public void increaseBalances() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Previous balance growth run is still in progress. Skipping this scheduler tick.");
            return;
        }

        try {
            balanceGrowthService.increaseBalances();
        } catch (RuntimeException exception) {
            log.error("Balance growth scheduler run failed", exception);
        } finally {
            running.set(false);
        }
    }
}
