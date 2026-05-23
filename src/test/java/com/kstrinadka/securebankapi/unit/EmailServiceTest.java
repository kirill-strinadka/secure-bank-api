package com.kstrinadka.securebankapi.unit;

import com.kstrinadka.securebankapi.dto.request.EmailCreateRequest;
import com.kstrinadka.securebankapi.dto.request.EmailUpdateRequest;
import com.kstrinadka.securebankapi.dto.response.EmailResponse;
import com.kstrinadka.securebankapi.entity.EmailDataEntity;
import com.kstrinadka.securebankapi.entity.UserEntity;
import com.kstrinadka.securebankapi.exception.ConflictException;
import com.kstrinadka.securebankapi.exception.NotFoundException;
import com.kstrinadka.securebankapi.mapper.EmailMapper;
import com.kstrinadka.securebankapi.repository.EmailDataRepository;
import com.kstrinadka.securebankapi.repository.UserRepository;
import com.kstrinadka.securebankapi.security.CurrentUserProvider;
import com.kstrinadka.securebankapi.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest extends AbstractUnitTest {

    private static final Long CURRENT_USER_ID = 1L;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailDataRepository emailDataRepository;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(
                currentUserProvider,
                userRepository,
                emailDataRepository,
                new EmailMapper()
        );
        when(currentUserProvider.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
    }

    @Test
    void addEmailAddsEmailSuccessfully() {
        UserEntity user = user(CURRENT_USER_ID);
        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user));
        when(emailDataRepository.existsByEmail("new@mail.com")).thenReturn(false);
        when(emailDataRepository.save(any(EmailDataEntity.class))).thenAnswer(invocation -> {
            EmailDataEntity emailData = invocation.getArgument(0);
            emailData.setId(10L);
            return emailData;
        });

        EmailResponse response = emailService.addEmail(new EmailCreateRequest("new@mail.com"));

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("new@mail.com");
        verify(emailDataRepository).save(any(EmailDataEntity.class));
    }

    @Test
    void addEmailFailsWhenEmailIsAlreadyUsed() {
        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user(CURRENT_USER_ID)));
        when(emailDataRepository.existsByEmail("used@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> emailService.addEmail(new EmailCreateRequest("used@mail.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email is already used");

        verify(emailDataRepository, never()).save(any(EmailDataEntity.class));
    }

    @Test
    void updateEmailUpdatesEmailSuccessfully() {
        EmailDataEntity existingEmail = emailData(10L, CURRENT_USER_ID, "old@mail.com");
        when(emailDataRepository.findByIdAndUserId(10L, CURRENT_USER_ID)).thenReturn(Optional.of(existingEmail));
        when(emailDataRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(emailDataRepository.save(existingEmail)).thenReturn(existingEmail);

        EmailResponse response = emailService.updateEmail(10L, new EmailUpdateRequest("new@mail.com"));

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("new@mail.com");
    }

    @Test
    void updateEmailAllowsSameEmail() {
        EmailDataEntity existingEmail = emailData(10L, CURRENT_USER_ID, "same@mail.com");
        when(emailDataRepository.findByIdAndUserId(10L, CURRENT_USER_ID)).thenReturn(Optional.of(existingEmail));
        when(emailDataRepository.findByEmail("same@mail.com")).thenReturn(Optional.of(existingEmail));
        when(emailDataRepository.save(existingEmail)).thenReturn(existingEmail);

        EmailResponse response = emailService.updateEmail(10L, new EmailUpdateRequest("same@mail.com"));

        assertThat(response.getEmail()).isEqualTo("same@mail.com");
    }

    @Test
    void updateEmailFailsWhenEmailBelongsToAnotherUser() {
        when(emailDataRepository.findByIdAndUserId(20L, CURRENT_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailService.updateEmail(20L, new EmailUpdateRequest("new@mail.com")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Email not found");

        verify(emailDataRepository, never()).save(any(EmailDataEntity.class));
    }

    @Test
    void updateEmailFailsWhenNewEmailIsUsedByAnotherEmailData() {
        EmailDataEntity existingEmail = emailData(10L, CURRENT_USER_ID, "old@mail.com");
        EmailDataEntity usedEmail = emailData(20L, 2L, "used@mail.com");
        when(emailDataRepository.findByIdAndUserId(10L, CURRENT_USER_ID)).thenReturn(Optional.of(existingEmail));
        when(emailDataRepository.findByEmail("used@mail.com")).thenReturn(Optional.of(usedEmail));

        assertThatThrownBy(() -> emailService.updateEmail(10L, new EmailUpdateRequest("used@mail.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email is already used");

        verify(emailDataRepository, never()).save(any(EmailDataEntity.class));
    }

    @Test
    void deleteEmailDeletesEmailSuccessfully() {
        EmailDataEntity emailData = emailData(10L, CURRENT_USER_ID, "delete@mail.com");
        when(emailDataRepository.findByIdAndUserId(10L, CURRENT_USER_ID)).thenReturn(Optional.of(emailData));
        when(emailDataRepository.countByUserId(CURRENT_USER_ID)).thenReturn(2L);

        emailService.deleteEmail(10L);

        verify(emailDataRepository).delete(emailData);
    }

    @Test
    void deleteEmailFailsWhenItIsLastEmail() {
        EmailDataEntity emailData = emailData(10L, CURRENT_USER_ID, "last@mail.com");
        when(emailDataRepository.findByIdAndUserId(10L, CURRENT_USER_ID)).thenReturn(Optional.of(emailData));
        when(emailDataRepository.countByUserId(CURRENT_USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> emailService.deleteEmail(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Last email cannot be deleted");

        verify(emailDataRepository, never()).delete(any(EmailDataEntity.class));
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        return user;
    }

    private EmailDataEntity emailData(Long id, Long userId, String email) {
        EmailDataEntity emailData = new EmailDataEntity();
        emailData.setId(id);
        emailData.setUser(user(userId));
        emailData.setEmail(email);
        return emailData;
    }
}
