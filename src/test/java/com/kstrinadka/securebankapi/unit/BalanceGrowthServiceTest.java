package com.kstrinadka.securebankapi.unit;

import com.kstrinadka.securebankapi.repository.AccountRepository;
import com.kstrinadka.securebankapi.service.impl.BalanceGrowthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class BalanceGrowthServiceTest extends AbstractUnitTest {

    @Mock
    private AccountRepository accountRepository;

    private BalanceGrowthServiceImpl balanceGrowthService;

    @BeforeEach
    void setUp() {
        balanceGrowthService = new BalanceGrowthServiceImpl(accountRepository);
    }

    @Test
    void increaseBalancesExecutesBulkUpdate() {
        balanceGrowthService.increaseBalances();

        verify(accountRepository).increaseBalancesByTenPercentWithCap();
        verifyNoMoreInteractions(accountRepository);
    }
}
