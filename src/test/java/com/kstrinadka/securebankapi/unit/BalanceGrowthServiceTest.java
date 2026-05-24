package com.kstrinadka.securebankapi.unit;

import com.kstrinadka.securebankapi.service.impl.BalanceGrowthBatchProcessor;
import com.kstrinadka.securebankapi.service.impl.BalanceGrowthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class BalanceGrowthServiceTest extends AbstractUnitTest {

    @Mock
    private BalanceGrowthBatchProcessor batchProcessor;

    private BalanceGrowthServiceImpl balanceGrowthService;

    @BeforeEach
    void setUp() {
        balanceGrowthService = new BalanceGrowthServiceImpl(batchProcessor);
    }

    @Test
    void increaseBalancesExecutesBatchesUntilNoDueAccountsRemain() {
        when(batchProcessor.increaseNextBatch(
                any(BigDecimal.class),
                any(BigDecimal.class),
                anyInt(),
                any(LocalDateTime.class),
                anyInt(),
                anyInt()
        )).thenReturn(50, 12, 0);
        when(batchProcessor.hasDueAccounts(any(LocalDateTime.class))).thenReturn(false);

        int processedCount = balanceGrowthService.increaseBalances();

        assertThat(processedCount).isEqualTo(62);

        verify(batchProcessor, times(3)).increaseNextBatch(
                eq(new BigDecimal("2.07")),
                eq(new BigDecimal("1.10")),
                eq(2),
                any(LocalDateTime.class),
                eq(50),
                eq(30)
        );
        verify(batchProcessor).hasDueAccounts(any(LocalDateTime.class));
        verifyNoMoreInteractions(batchProcessor);
    }
}
