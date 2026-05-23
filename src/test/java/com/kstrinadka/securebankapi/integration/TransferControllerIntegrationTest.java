package com.kstrinadka.securebankapi.integration;

import com.kstrinadka.securebankapi.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class TransferControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void transferMovesMoneyFromCurrentJwtUser() throws Exception {
        mockMvc.perform(authenticatedTransfer(1L, "{\"fromUserId\":3,\"toUserId\":2,\"amount\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fromUserId").value(1))
                .andExpect(jsonPath("$.toUserId").value(2))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        flushPersistenceContext();

        assertThat(balanceByUserId(1L)).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(balanceByUserId(2L)).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(successTransferCount(1L, 2L, "100.00")).isEqualTo(1);
        assertThat(successTransferCount(3L, 2L, "100.00")).isZero();
    }

    @Test
    void transferWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUserId\":2,\"amount\":100}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void transferToSelfReturnsBadRequest() throws Exception {
        mockMvc.perform(authenticatedTransfer(1L, "{\"toUserId\":1,\"amount\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRANSFER_TO_SELF_NOT_ALLOWED"));
    }

    @Test
    void transferWithInsufficientFundsReturnsConflict() throws Exception {
        mockMvc.perform(authenticatedTransfer(1L, "{\"toUserId\":2,\"amount\":100000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));

        assertThat(balanceByUserId(1L)).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(balanceByUserId(2L)).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(allTransferCount()).isZero();
    }

    @Test
    void transferWithZeroAmountReturnsBadRequest() throws Exception {
        mockMvc.perform(authenticatedTransfer(1L, "{\"toUserId\":2,\"amount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void transferWithNegativeAmountReturnsBadRequest() throws Exception {
        mockMvc.perform(authenticatedTransfer(1L, "{\"toUserId\":2,\"amount\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void transferWithTooManyFractionDigitsReturnsBadRequest() throws Exception {
        mockMvc.perform(authenticatedTransfer(1L, "{\"toUserId\":2,\"amount\":10.123}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void transferWithNullAmountReturnsBadRequest() throws Exception {
        mockMvc.perform(authenticatedTransfer(1L, "{\"toUserId\":2,\"amount\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void transferToUnknownReceiverReturnsNotFound() throws Exception {
        mockMvc.perform(authenticatedTransfer(1L, "{\"toUserId\":999,\"amount\":10}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECEIVER_NOT_FOUND"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedTransfer(
            Long userId,
            String json
    ) {
        return post("/api/v1/transfers")
                .header("Authorization", "Bearer " + jwtService.generateToken(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    private BigDecimal balanceByUserId(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?",
                BigDecimal.class,
                userId
        );
    }

    private Integer successTransferCount(Long fromUserId, Long toUserId, String amount) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfer WHERE from_user_id = ? AND to_user_id = ? AND amount = ?::numeric AND status = 'SUCCESS'",
                Integer.class,
                fromUserId,
                toUserId,
                amount
        );
    }

    private Integer allTransferCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfer",
                Integer.class
        );
    }

    private void flushPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
