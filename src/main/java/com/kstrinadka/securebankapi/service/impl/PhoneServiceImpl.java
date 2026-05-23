package com.kstrinadka.securebankapi.service.impl;

import com.kstrinadka.securebankapi.dto.request.PhoneCreateRequest;
import com.kstrinadka.securebankapi.dto.request.PhoneUpdateRequest;
import com.kstrinadka.securebankapi.dto.response.PhoneResponse;
import com.kstrinadka.securebankapi.entity.PhoneDataEntity;
import com.kstrinadka.securebankapi.entity.UserEntity;
import com.kstrinadka.securebankapi.exception.ConflictException;
import com.kstrinadka.securebankapi.exception.NotFoundException;
import com.kstrinadka.securebankapi.mapper.PhoneMapper;
import com.kstrinadka.securebankapi.repository.PhoneDataRepository;
import com.kstrinadka.securebankapi.repository.UserRepository;
import com.kstrinadka.securebankapi.security.CurrentUserProvider;
import com.kstrinadka.securebankapi.service.PhoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhoneServiceImpl implements PhoneService {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final PhoneDataRepository phoneDataRepository;
    private final PhoneMapper phoneMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PhoneResponse> getCurrentUserPhones() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        ensureUserExists(currentUserId);

        return phoneDataRepository.findAllByUserIdOrderByPhoneAsc(currentUserId)
                .stream()
                .map(phoneMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PhoneResponse addPhone(PhoneCreateRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        UserEntity user = findCurrentUser(currentUserId);
        String phone = request.getPhone();

        if (phoneDataRepository.existsByPhone(phone)) {
            throw new ConflictException("PHONE_ALREADY_EXISTS", "Phone is already used");
        }

        PhoneDataEntity phoneData = new PhoneDataEntity();
        phoneData.setUser(user);
        phoneData.setPhone(phone);

        return phoneMapper.toResponse(phoneDataRepository.save(phoneData));
    }

    @Override
    @Transactional
    public PhoneResponse updatePhone(Long phoneId, PhoneUpdateRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        PhoneDataEntity phoneData = findCurrentUserPhone(phoneId, currentUserId);
        String newPhone = request.getPhone();

        phoneDataRepository.findByPhone(newPhone)
                .filter(existingPhone -> !existingPhone.getId().equals(phoneData.getId()))
                .ifPresent(existingPhone -> {
                    throw new ConflictException("PHONE_ALREADY_EXISTS", "Phone is already used");
                });

        phoneData.setPhone(newPhone);
        return phoneMapper.toResponse(phoneDataRepository.save(phoneData));
    }

    @Override
    @Transactional
    public void deletePhone(Long phoneId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        PhoneDataEntity phoneData = findCurrentUserPhone(phoneId, currentUserId);

        if (phoneDataRepository.countByUserId(currentUserId) <= 1) {
            throw new ConflictException("LAST_PHONE_CANNOT_BE_DELETED", "Last phone cannot be deleted");
        }

        phoneDataRepository.delete(phoneData);
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

    private PhoneDataEntity findCurrentUserPhone(Long phoneId, Long currentUserId) {
        return phoneDataRepository.findByIdAndUserId(phoneId, currentUserId)
                .orElseThrow(() -> new NotFoundException("PHONE_NOT_FOUND", "Phone not found"));
    }
}
