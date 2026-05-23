package com.kstrinadka.securebankapi.unit;

import com.kstrinadka.securebankapi.dto.response.TransferResponse;
import com.kstrinadka.securebankapi.entity.AccountEntity;
import com.kstrinadka.securebankapi.entity.TransferEntity;
import com.kstrinadka.securebankapi.entity.TransferStatus;
import com.kstrinadka.securebankapi.entity.UserEntity;
import com.kstrinadka.securebankapi.exception.ApiException;
import com.kstrinadka.securebankapi.exception.BadRequestException;
import com.kstrinadka.securebankapi.exception.InsufficientFundsException;
import com.kstrinadka.securebankapi.exception.NotFoundException;
import com.kstrinadka.securebankapi.mapper.TransferMapper;
import com.kstrinadka.securebankapi.repository.AccountRepository;
import com.kstrinadka.securebankapi.repository.TransferRepository;
import com.kstrinadka.securebankapi.repository.UserRepository;
import com.kstrinadka.securebankapi.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferServiceTest extends AbstractUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    private TransferServiceImpl transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferServiceImpl(
                userRepository,
                accountRepository,
                transferRepository,
                new TransferMapper()
        );
    }

    @Test
    void transferMovesMoneySuccessfully() {
        UserEntity fromUser = user(2L);
        UserEntity toUser = user(1L);
        AccountEntity fromAccount = account(20L, fromUser, "1000.00");
        AccountEntity toAccount = account(10L, toUser, "500.00");
        when(userRepository.existsById(2L)).thenReturn(true);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(accountRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(fromAccount));
        when(transferRepository.save(any(TransferEntity.class))).thenAnswer(invocation -> {
            TransferEntity transfer = invocation.getArgument(0);
            transfer.setId(100L);
            transfer.setCreatedAt(LocalDateTime.now());
            return transfer;
        });

        TransferResponse response = transferService.transfer(2L, 1L, new BigDecimal("200.00"));

        assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(toAccount.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getFromUserId()).isEqualTo(2L);
        assertThat(response.getToUserId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.getStatus()).isEqualTo(TransferStatus.SUCCESS);

        InOrder inOrder = inOrder(accountRepository, transferRepository);
        inOrder.verify(accountRepository).findByUserIdForUpdate(1L);
        inOrder.verify(accountRepository).findByUserIdForUpdate(2L);
        inOrder.verify(transferRepository).save(any(TransferEntity.class));
        verify(accountRepository, never()).save(any(AccountEntity.class));
    }

    @Test
    void transferFailsWhenFundsAreInsufficient() {
        UserEntity fromUser = user(1L);
        UserEntity toUser = user(2L);
        AccountEntity fromAccount = account(10L, fromUser, "100.00");
        AccountEntity toAccount = account(20L, toUser, "500.00");
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(accountRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(() -> transferService.transfer(1L, 2L, new BigDecimal("200.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Insufficient funds");

        assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(toAccount.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(transferRepository, never()).save(any(TransferEntity.class));
    }

    @Test
    void transferToSelfFails() {
        assertBadRequest(
                () -> transferService.transfer(1L, 1L, new BigDecimal("10.00")),
                "TRANSFER_TO_SELF_NOT_ALLOWED"
        );
        verifyNoInteractions(userRepository, accountRepository, transferRepository);
    }

    @Test
    void zeroAmountFails() {
        assertBadRequest(
                () -> transferService.transfer(1L, 2L, BigDecimal.ZERO),
                "AMOUNT_MUST_BE_POSITIVE"
        );
        verifyNoInteractions(userRepository, accountRepository, transferRepository);
    }

    @Test
    void negativeAmountFails() {
        assertBadRequest(
                () -> transferService.transfer(1L, 2L, new BigDecimal("-10.00")),
                "AMOUNT_MUST_BE_POSITIVE"
        );
        verifyNoInteractions(userRepository, accountRepository, transferRepository);
    }

    @Test
    void amountWithMoreThanTwoFractionDigitsFails() {
        assertBadRequest(
                () -> transferService.transfer(1L, 2L, new BigDecimal("10.123")),
                "AMOUNT_SCALE_INVALID"
        );
        verifyNoInteractions(userRepository, accountRepository, transferRepository);
    }

    @Test
    void senderNotFoundFails() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertNotFound(
                () -> transferService.transfer(1L, 2L, new BigDecimal("10.00")),
                "SENDER_NOT_FOUND"
        );

        verifyNoInteractions(accountRepository, transferRepository);
    }

    @Test
    void receiverNotFoundFails() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(2L)).thenReturn(false);

        assertNotFound(
                () -> transferService.transfer(1L, 2L, new BigDecimal("10.00")),
                "RECEIVER_NOT_FOUND"
        );

        verifyNoInteractions(accountRepository, transferRepository);
    }

    @Test
    void senderAccountNotFoundFails() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(accountRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.empty());

        assertNotFound(
                () -> transferService.transfer(1L, 2L, new BigDecimal("10.00")),
                "SENDER_ACCOUNT_NOT_FOUND"
        );

        verify(transferRepository, never()).save(any(TransferEntity.class));
    }

    @Test
    void receiverAccountNotFoundFails() {
        UserEntity fromUser = user(1L);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(accountRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(account(10L, fromUser, "100.00")));
        when(accountRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.empty());

        assertNotFound(
                () -> transferService.transfer(1L, 2L, new BigDecimal("10.00")),
                "RECEIVER_ACCOUNT_NOT_FOUND"
        );

        verify(transferRepository, never()).save(any(TransferEntity.class));
    }

    private void assertBadRequest(ThrowingOperation operation, String expectedCode) {
        assertThatThrownBy(operation::execute)
                .isInstanceOf(BadRequestException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getCode()).isEqualTo(expectedCode));
    }

    private void assertNotFound(ThrowingOperation operation, String expectedCode) {
        assertThatThrownBy(operation::execute)
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getCode()).isEqualTo(expectedCode));
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        return user;
    }

    private AccountEntity account(Long id, UserEntity user, String balance) {
        AccountEntity account = new AccountEntity();
        account.setId(id);
        account.setUser(user);
        account.setBalance(new BigDecimal(balance));
        account.setInitialBalance(new BigDecimal(balance));
        account.setVersion(0L);
        return account;
    }

    private interface ThrowingOperation {

        void execute();
    }
}
