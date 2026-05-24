package com.kstrinadka.securebankapi.service.impl;

import com.kstrinadka.securebankapi.repository.AccountRepository;
import com.kstrinadka.securebankapi.service.BalanceGrowthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BalanceGrowthServiceImpl implements BalanceGrowthService {

    private static final BigDecimal MAX_BALANCE_MULTIPLIER = new BigDecimal("2.07");
    private static final BigDecimal GROWTH_MULTIPLIER = new BigDecimal("1.10");
    private static final int MONEY_SCALE = 2;

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void increaseBalances() {
        accountRepository.increaseBalancesByTenPercentWithCap(
                MAX_BALANCE_MULTIPLIER,
                GROWTH_MULTIPLIER,
                MONEY_SCALE
        );
    }
}
