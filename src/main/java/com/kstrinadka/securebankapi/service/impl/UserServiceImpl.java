package com.kstrinadka.securebankapi.service.impl;

import com.kstrinadka.securebankapi.dto.response.PageResponse;
import com.kstrinadka.securebankapi.dto.response.UserResponse;
import com.kstrinadka.securebankapi.entity.UserEntity;
import com.kstrinadka.securebankapi.mapper.UserMapper;
import com.kstrinadka.securebankapi.repository.UserRepository;
import com.kstrinadka.securebankapi.repository.UserSpecification;
import com.kstrinadka.securebankapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(
            LocalDate dateOfBirth,
            String phone,
            String name,
            String email,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<UserEntity> users = userRepository.findAll(
                UserSpecification.search(dateOfBirth, phone, name, email),
                pageRequest
        );

        List<UserResponse> items = users.getContent()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                items,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }
}
