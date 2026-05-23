package com.kstrinadka.securebankapi.integration;

import com.kstrinadka.securebankapi.dto.response.TransferResponse;
import com.kstrinadka.securebankapi.entity.TransferStatus;
import com.kstrinadka.securebankapi.exception.InsufficientFundsException;
import com.kstrinadka.securebankapi.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TransferServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void transferMovesMoneyOnRealPostgres() {
        TransferResponse response = transferService.transfer(1L, 2L, new BigDecimal("100.00"));
        flushPersistenceContext();

        assertThat(response.getId()).isNotNull();
        assertThat(response.getFromUserId()).isEqualTo(1L);
        assertThat(response.getToUserId()).isEqualTo(2L);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getStatus()).isEqualTo(TransferStatus.SUCCESS);

        assertThat(balanceByUserId(1L)).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(balanceByUserId(2L)).isEqualByComparingTo(new BigDecimal("600.00"));

        Integer transferCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfer WHERE from_user_id = 1 AND to_user_id = 2 AND amount = 100.00 AND status = 'SUCCESS'",
                Integer.class
        );
        assertThat(transferCount).isEqualTo(1);
    }

    @Test
    void transferWithInsufficientFundsDoesNotChangeDatabase() {
        assertThatThrownBy(() -> transferService.transfer(1L, 2L, new BigDecimal("100000.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Insufficient funds");

        assertThat(balanceByUserId(1L)).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(balanceByUserId(2L)).isEqualByComparingTo(new BigDecimal("500.00"));

        Integer transferCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfer",
                Integer.class
        );
        assertThat(transferCount).isZero();
    }

    private BigDecimal balanceByUserId(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?",
                BigDecimal.class,
                userId
        );
    }

    private void flushPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
