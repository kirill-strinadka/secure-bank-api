package com.kstrinadka.securebankapi.unit;

import com.kstrinadka.securebankapi.entity.AccountEntity;
import com.kstrinadka.securebankapi.repository.AccountRepository;
import com.kstrinadka.securebankapi.service.impl.BalanceGrowthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BalanceGrowthServiceTest extends AbstractUnitTest {

    @Mock
    private AccountRepository accountRepository;

    private BalanceGrowthServiceImpl balanceGrowthService;

    @BeforeEach
    void setUp() {
        balanceGrowthService = new BalanceGrowthServiceImpl(accountRepository);
    }

    @Test
    void balance100IncreasesTo110() {
        AccountEntity account = account("100.00", "100.00");
        when(accountRepository.findAllForUpdate()).thenReturn(Collections.singletonList(account));

        balanceGrowthService.increaseBalances();

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("110.00"));
    }

    @Test
    void balance110IncreasesTo121() {
        AccountEntity account = account("100.00", "110.00");
        when(accountRepository.findAllForUpdate()).thenReturn(Collections.singletonList(account));

        balanceGrowthService.increaseBalances();

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("121.00"));
    }

    @Test
    void balance200IsCappedAt207ForInitial100() {
        AccountEntity account = account("100.00", "200.00");
        when(accountRepository.findAllForUpdate()).thenReturn(Collections.singletonList(account));

        balanceGrowthService.increaseBalances();

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("207.00"));
    }

    @Test
    void balance207Stays207ForInitial100() {
        AccountEntity account = account("100.00", "207.00");
        when(accountRepository.findAllForUpdate()).thenReturn(Collections.singletonList(account));

        balanceGrowthService.increaseBalances();

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("207.00"));
    }

    @Test
    void balanceAboveMaxIsNotDecreased() {
        AccountEntity account = account("100.00", "500.00");
        when(accountRepository.findAllForUpdate()).thenReturn(Collections.singletonList(account));

        balanceGrowthService.increaseBalances();

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void roundingUsesHalfUp() {
        AccountEntity account = account("1000.00", "100.05");
        when(accountRepository.findAllForUpdate()).thenReturn(Collections.singletonList(account));

        balanceGrowthService.increaseBalances();

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("110.06"));
    }

    private AccountEntity account(String initialBalance, String balance) {
        AccountEntity account = new AccountEntity();
        account.setInitialBalance(new BigDecimal(initialBalance));
        account.setBalance(new BigDecimal(balance));
        account.setVersion(0L);
        return account;
    }
}
