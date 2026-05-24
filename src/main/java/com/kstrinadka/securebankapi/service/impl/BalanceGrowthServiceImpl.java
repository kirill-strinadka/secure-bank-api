package com.kstrinadka.securebankapi.service.impl;

import com.kstrinadka.securebankapi.entity.AccountEntity;
import com.kstrinadka.securebankapi.repository.AccountRepository;
import com.kstrinadka.securebankapi.service.BalanceGrowthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
        accountRepository.findAllForUpdate()
                .forEach(this::increaseBalance);
    }

    private void increaseBalance(AccountEntity account) {
        BigDecimal maxBalance = scale(account.getInitialBalance().multiply(MAX_BALANCE_MULTIPLIER));
        BigDecimal currentBalance = account.getBalance();

        if (currentBalance.compareTo(maxBalance) >= 0) {
            return;
        }

        BigDecimal increasedBalance = scale(currentBalance.multiply(GROWTH_MULTIPLIER));
        if (increasedBalance.compareTo(maxBalance) > 0) {
            account.setBalance(maxBalance);
            return;
        }

        account.setBalance(increasedBalance);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
