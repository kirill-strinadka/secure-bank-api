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
        assertThat(balanceByUserId(2L)).isEqualByComparingTo(new BigDecimal("121.00"));
        assertThat(balanceByUserId(3L)).isEqualByComparingTo(new BigDecimal("207.00"));
        assertThat(balanceByUserId(101L)).isEqualByComparingTo(new BigDecimal("207.00"));
        assertThat(balanceByUserId(102L)).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(balanceByUserId(103L)).isEqualByComparingTo(new BigDecimal("110.06"));
    }

    private void prepareAccounts() {
        deleteTemporaryUsers();
        jdbcTemplate.update("UPDATE account SET initial_balance = 100.00, balance = 100.00, version = 0 WHERE user_id = 1");
        jdbcTemplate.update("UPDATE account SET initial_balance = 100.00, balance = 110.00, version = 0 WHERE user_id = 2");
        jdbcTemplate.update("UPDATE account SET initial_balance = 100.00, balance = 200.00, version = 0 WHERE user_id = 3");
        insertTemporaryUserWithAccount(101L, "Limit User", "207.00", "100.00");
        insertTemporaryUserWithAccount(102L, "High Balance User", "500.00", "100.00");
        insertTemporaryUserWithAccount(103L, "Rounding User", "100.05", "1000.00");
    }

    private void resetAccountsToMigrationState() {
        deleteTemporaryUsers();
        jdbcTemplate.update("UPDATE account SET initial_balance = 1000.00, balance = 1000.00, version = 0 WHERE user_id = 1");
        jdbcTemplate.update("UPDATE account SET initial_balance = 500.00, balance = 500.00, version = 0 WHERE user_id = 2");
        jdbcTemplate.update("UPDATE account SET initial_balance = 2000.00, balance = 2000.00, version = 0 WHERE user_id = 3");
    }

    private void insertTemporaryUserWithAccount(Long userId, String name, String balance, String initialBalance) {
        jdbcTemplate.update(
                "INSERT INTO users (id, name, date_of_birth, password) VALUES (?, ?, '1990-01-01', 'password')",
                userId,
                name
        );
        jdbcTemplate.update(
                "INSERT INTO account (user_id, balance, initial_balance) VALUES (?, ?::numeric, ?::numeric)",
                userId,
                balance,
                initialBalance
        );
    }

    private void deleteTemporaryUsers() {
        jdbcTemplate.update("DELETE FROM transfer WHERE from_user_id IN (101, 102, 103) OR to_user_id IN (101, 102, 103)");
        jdbcTemplate.update("DELETE FROM account WHERE user_id IN (101, 102, 103)");
        jdbcTemplate.update("DELETE FROM users WHERE id IN (101, 102, 103)");
    }

    private BigDecimal balanceByUserId(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?",
                BigDecimal.class,
                userId
        );
    }
}
