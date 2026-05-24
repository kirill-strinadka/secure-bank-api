package com.kstrinadka.securebankapi.service.impl;

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
import com.kstrinadka.securebankapi.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final EmailDataRepository emailDataRepository;
    private final EmailMapper emailMapper;

    @Override
    @Transactional(readOnly = true)
    public List<EmailResponse> getCurrentUserEmails() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        ensureUserExists(currentUserId);

        return emailDataRepository.findAllByUserIdOrderByEmailAsc(currentUserId)
                .stream()
                .map(emailMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EmailResponse addEmail(EmailCreateRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        UserEntity user = findCurrentUser(currentUserId);
        String email = request.getEmail();

        if (emailDataRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "Email is already used");
        }

        EmailDataEntity emailData = new EmailDataEntity();
        emailData.setUser(user);
        emailData.setEmail(email);

        return emailMapper.toResponse(emailDataRepository.save(emailData));
    }

    @Override
    @Transactional
    public EmailResponse updateEmail(Long emailId, EmailUpdateRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        EmailDataEntity emailData = findCurrentUserEmail(emailId, currentUserId);
        String newEmail = request.getEmail();

        emailDataRepository.findByEmail(newEmail)
                .filter(existingEmail -> !existingEmail.getId().equals(emailData.getId()))
                .ifPresent(existingEmail -> {
                    throw new ConflictException("EMAIL_ALREADY_EXISTS", "Email is already used");
                });

        emailData.setEmail(newEmail);
        return emailMapper.toResponse(emailDataRepository.save(emailData));
    }

    @Override
    @Transactional
    public void deleteEmail(Long emailId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        EmailDataEntity emailData = findCurrentUserEmail(emailId, currentUserId);

        if (emailDataRepository.countByUserId(currentUserId) <= 1) {
            throw new ConflictException("LAST_EMAIL_CANNOT_BE_DELETED", "Last email cannot be deleted");
        }

        emailDataRepository.delete(emailData);
    }

    private UserEntity findCurrentUser(Long currentUserId) {
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private void ensureUserExists(Long currentUserId) {
        if (!userRepository.existsById(currentUserId)) {
            throw new NotFoundException("USER_NOT_FOUND", "User not found");
        }
    }

    private EmailDataEntity findCurrentUserEmail(Long emailId, Long currentUserId) {
        return emailDataRepository.findByIdAndUserId(emailId, currentUserId)
                .orElseThrow(() -> new NotFoundException("EMAIL_NOT_FOUND", "Email not found"));
    }
}
