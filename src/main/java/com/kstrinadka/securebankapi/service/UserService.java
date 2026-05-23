package com.kstrinadka.securebankapi.service;

import com.kstrinadka.securebankapi.dto.response.PageResponse;
import com.kstrinadka.securebankapi.dto.response.UserResponse;

import java.time.LocalDate;

public interface UserService {

    PageResponse<UserResponse> searchUsers(
            LocalDate dateOfBirth,
            String phone,
            String name,
            String email,
            int page,
            int size
    );
}
