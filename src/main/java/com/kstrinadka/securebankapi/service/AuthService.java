package com.kstrinadka.securebankapi.service;

import com.kstrinadka.securebankapi.dto.request.LoginRequest;
import com.kstrinadka.securebankapi.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
