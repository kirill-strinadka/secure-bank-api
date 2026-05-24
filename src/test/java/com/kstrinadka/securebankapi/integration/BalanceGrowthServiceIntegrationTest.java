package com.kstrinadka.securebankapi.integration;

import com.kstrinadka.securebankapi.service.BalanceGrowthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceGrowthServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BalanceGrowthService balanceGrowthService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpAccounts() {
        prepareAccounts();
    }

    @AfterEach
    void tearDownAccounts() {
        resetAccountsToMigrationState();
    }

    @Test
    void increaseBalancesUpdatesAccountsOnRealPostgres() {
        balanceGrowthService.increaseBalances();

        assertThat(balanceByUserId(1L)).isEqualByComparingTo(new BigDecimal("110.00"));
        assertThat(balanceByUserId(2L)).isEqualByComparingTo(new BigDecimal("207.00"));
        assertThat(balanceByUserId(3L)).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    private void prepareAccounts() {
        jdbcTemplate.update("UPDATE account SET initial_balance = 100.00, balance = 100.00, version = 0 WHERE user_id = 1");
        jdbcTemplate.update("UPDATE account SET initial_balance = 100.00, balance = 200.00, version = 0 WHERE user_id = 2");
        jdbcTemplate.update("UPDATE account SET initial_balance = 100.00, balance = 500.00, version = 0 WHERE user_id = 3");
    }

    private void resetAccountsToMigrationState() {
        jdbcTemplate.update("UPDATE account SET initial_balance = 1000.00, balance = 1000.00, version = 0 WHERE user_id = 1");
        jdbcTemplate.update("UPDATE account SET initial_balance = 500.00, balance = 500.00, version = 0 WHERE user_id = 2");
        jdbcTemplate.update("UPDATE account SET initial_balance = 2000.00, balance = 2000.00, version = 0 WHERE user_id = 3");
    }

    private BigDecimal balanceByUserId(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?",
                BigDecimal.class,
                userId
        );
    }
}
