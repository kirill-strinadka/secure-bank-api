package com.kstrinadka.securebankapi.service.impl;

import com.kstrinadka.securebankapi.dto.response.TransferResponse;
import com.kstrinadka.securebankapi.entity.AccountEntity;
import com.kstrinadka.securebankapi.entity.TransferEntity;
import com.kstrinadka.securebankapi.entity.TransferStatus;
import com.kstrinadka.securebankapi.exception.BadRequestException;
import com.kstrinadka.securebankapi.exception.InsufficientFundsException;
import com.kstrinadka.securebankapi.exception.NotFoundException;
import com.kstrinadka.securebankapi.mapper.TransferMapper;
import com.kstrinadka.securebankapi.repository.AccountRepository;
import com.kstrinadka.securebankapi.repository.TransferRepository;
import com.kstrinadka.securebankapi.repository.UserRepository;
import com.kstrinadka.securebankapi.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;

    @Override
    @Transactional
    public TransferResponse transfer(Long fromUserId, Long toUserId, BigDecimal amount) {
        validateRequest(fromUserId, toUserId, amount);

        ensureUserExists(fromUserId, "SENDER_NOT_FOUND");
        ensureUserExists(toUserId, "RECEIVER_NOT_FOUND");

        LockedAccounts lockedAccounts = lockAccounts(fromUserId, toUserId);
        AccountEntity fromAccount = lockedAccounts.accountForUser(fromUserId);
        AccountEntity toAccount = lockedAccounts.accountForUser(toUserId);

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("INSUFFICIENT_FUNDS", "Insufficient funds");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        TransferEntity transfer = new TransferEntity();
        transfer.setFromUser(fromAccount.getUser());
        transfer.setToUser(toAccount.getUser());
        transfer.setAmount(amount);
        transfer.setStatus(TransferStatus.SUCCESS);

        return transferMapper.toResponse(transferRepository.save(transfer));
    }

    private void validateRequest(Long fromUserId, Long toUserId, BigDecimal amount) {
        if (fromUserId == null) {
            throw new BadRequestException("FROM_USER_ID_REQUIRED", "Sender user id is required");
        }
        if (toUserId == null) {
            throw new BadRequestException("TO_USER_ID_REQUIRED", "Receiver user id is required");
        }
        if (amount == null) {
            throw new BadRequestException("AMOUNT_REQUIRED", "Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("AMOUNT_MUST_BE_POSITIVE", "Amount must be positive");
        }
        if (amount.scale() > 2) {
            throw new BadRequestException("AMOUNT_SCALE_INVALID", "Amount cannot have more than 2 decimal places");
        }
        if (Objects.equals(fromUserId, toUserId)) {
            throw new BadRequestException("TRANSFER_TO_SELF_NOT_ALLOWED", "Cannot transfer money to yourself");
        }
    }

    private void ensureUserExists(Long userId, String code) {
        String message = "SENDER_NOT_FOUND".equals(code) ? "Sender not found" : "Receiver not found";
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(code, message);
        }
    }

    private LockedAccounts lockAccounts(Long fromUserId, Long toUserId) {
        Long firstUserId = Math.min(fromUserId, toUserId);
        Long secondUserId = Math.max(fromUserId, toUserId);

        AccountEntity firstAccount = findAccountForUpdate(firstUserId, fromUserId);
        AccountEntity secondAccount = findAccountForUpdate(secondUserId, fromUserId);

        return new LockedAccounts(firstAccount, secondAccount);
    }

    private AccountEntity findAccountForUpdate(Long userId, Long fromUserId) {
        return accountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> accountNotFound(userId, fromUserId));
    }

    private NotFoundException accountNotFound(Long userId, Long fromUserId) {
        if (userId.equals(fromUserId)) {
            return new NotFoundException("SENDER_ACCOUNT_NOT_FOUND", "Sender account not found");
        }
        return new NotFoundException(
                "RECEIVER_ACCOUNT_NOT_FOUND",
                "Receiver account not found"
        );
    }

    private static class LockedAccounts {

        private final AccountEntity firstAccount;
        private final AccountEntity secondAccount;

        private LockedAccounts(AccountEntity firstAccount, AccountEntity secondAccount) {
            this.firstAccount = firstAccount;
            this.secondAccount = secondAccount;
        }

        private AccountEntity accountForUser(Long userId) {
            if (firstAccount.getUser().getId().equals(userId)) {
                return firstAccount;
            }
            return secondAccount;
        }
    }
}
