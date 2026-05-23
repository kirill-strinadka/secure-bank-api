package com.kstrinadka.securebankapi.mapper;

import com.kstrinadka.securebankapi.dto.response.UserResponse;
import com.kstrinadka.securebankapi.entity.EmailDataEntity;
import com.kstrinadka.securebankapi.entity.PhoneDataEntity;
import com.kstrinadka.securebankapi.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getDateOfBirth(),
                user.getEmails().stream()
                        .map(EmailDataEntity::getEmail)
                        .sorted()
                        .collect(Collectors.toList()),
                user.getPhones().stream()
                        .map(PhoneDataEntity::getPhone)
                        .sorted()
                        .collect(Collectors.toList())
        );
    }
}
