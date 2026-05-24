package com.kstrinadka.securebankapi.service.impl;

import com.kstrinadka.securebankapi.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BalanceGrowthBatchProcessor {

    private final AccountRepository accountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int increaseNextBatch(
            BigDecimal maxBalanceMultiplier,
            BigDecimal growthMultiplier,
            int moneyScale,
            LocalDateTime growthThreshold,
            int batchSize,
            int growthIntervalSeconds
    ) {
        return accountRepository.increaseNextBalanceGrowthBatch(
                maxBalanceMultiplier,
                growthMultiplier,
                moneyScale,
                growthThreshold,
                batchSize,
                growthIntervalSeconds
        );
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean hasDueAccounts(LocalDateTime growthThreshold) {
        return accountRepository.existsDueForBalanceGrowth(growthThreshold);
    }
}
