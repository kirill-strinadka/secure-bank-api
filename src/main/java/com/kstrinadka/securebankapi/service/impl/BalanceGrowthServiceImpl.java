package com.kstrinadka.securebankapi.service.impl;

import com.kstrinadka.securebankapi.repository.AccountRepository;
import com.kstrinadka.securebankapi.service.BalanceGrowthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BalanceGrowthServiceImpl implements BalanceGrowthService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void increaseBalances() {
        accountRepository.increaseBalancesByTenPercentWithCap();
    }
}
