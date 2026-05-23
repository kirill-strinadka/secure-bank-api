package com.kstrinadka.securebankapi.integration;

import com.kstrinadka.securebankapi.entity.AccountEntity;
import com.kstrinadka.securebankapi.entity.EmailDataEntity;
import com.kstrinadka.securebankapi.entity.PhoneDataEntity;
import com.kstrinadka.securebankapi.entity.UserEntity;
import com.kstrinadka.securebankapi.repository.AccountRepository;
import com.kstrinadka.securebankapi.repository.EmailDataRepository;
import com.kstrinadka.securebankapi.repository.PhoneDataRepository;
import com.kstrinadka.securebankapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailDataRepository emailDataRepository;

    @Autowired
    private PhoneDataRepository phoneDataRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void userRepositoryFindsUserFromMigration() {
        Optional<UserEntity> user = userRepository.findById(1L);

        assertThat(user)
                .isPresent()
                .get()
                .satisfies(value -> {
                    assertThat(value.getName()).isEqualTo("Ivan Petrov");
                    assertThat(value.getDateOfBirth()).isEqualTo(LocalDate.of(1993, 5, 1));
                });
    }

    @Test
    void emailDataRepositoryFindsEmailFromMigration() {
        Optional<EmailDataEntity> emailData = emailDataRepository.findByEmail("ivan@mail.com");

        assertThat(emailData)
                .isPresent()
                .get()
                .satisfies(value -> {
                    assertThat(value.getEmail()).isEqualTo("ivan@mail.com");
                    assertThat(value.getUser().getId()).isEqualTo(1L);
                });
        assertThat(emailDataRepository.existsByEmail("ivan@mail.com")).isTrue();
        assertThat(emailDataRepository.countByUserId(1L)).isEqualTo(1);
        assertThat(emailDataRepository.findByIdAndUserId(emailData.get().getId(), 1L)).isPresent();
    }

    @Test
    void phoneDataRepositoryFindsPhoneFromMigration() {
        Optional<PhoneDataEntity> phoneData = phoneDataRepository.findByPhone("79207865431");

        assertThat(phoneData)
                .isPresent()
                .get()
                .satisfies(value -> {
                    assertThat(value.getPhone()).isEqualTo("79207865431");
                    assertThat(value.getUser().getId()).isEqualTo(1L);
                });
        assertThat(phoneDataRepository.existsByPhone("79207865431")).isTrue();
        assertThat(phoneDataRepository.countByUserId(1L)).isEqualTo(1);
        assertThat(phoneDataRepository.findByIdAndUserId(phoneData.get().getId(), 1L)).isPresent();
    }

    @Test
    void accountRepositoryFindsAccountByUserId() {
        Optional<AccountEntity> account = accountRepository.findByUserId(1L);

        assertThat(account)
                .isPresent()
                .get()
                .satisfies(value -> {
                    assertThat(value.getUser().getId()).isEqualTo(1L);
                    assertThat(value.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
                    assertThat(value.getInitialBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
                    assertThat(value.getVersion()).isEqualTo(0L);
                });
    }

    @Test
    @Transactional
    void accountRepositoryFindsAccountByUserIdForUpdateInsideTransaction() {
        Optional<AccountEntity> account = accountRepository.findByUserIdForUpdate(1L);

        assertThat(account)
                .isPresent()
                .get()
                .satisfies(value -> {
                    assertThat(value.getUser().getId()).isEqualTo(1L);
                    assertThat(value.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
                });
    }
}
