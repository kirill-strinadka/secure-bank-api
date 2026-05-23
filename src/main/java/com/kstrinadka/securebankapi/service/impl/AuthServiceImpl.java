package com.kstrinadka.securebankapi.service.impl;

import com.kstrinadka.securebankapi.dto.request.LoginRequest;
import com.kstrinadka.securebankapi.dto.response.LoginResponse;
import com.kstrinadka.securebankapi.entity.EmailDataEntity;
import com.kstrinadka.securebankapi.entity.UserEntity;
import com.kstrinadka.securebankapi.exception.UnauthorizedException;
import com.kstrinadka.securebankapi.repository.EmailDataRepository;
import com.kstrinadka.securebankapi.security.JwtService;
import com.kstrinadka.securebankapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final EmailDataRepository emailDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        EmailDataEntity emailData = emailDataRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> invalidCredentials());

        UserEntity user = emailData.getUser();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw invalidCredentials();
        }

        String token = jwtService.generateToken(user.getId());
        return new LoginResponse(token);
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("INVALID_CREDENTIALS", INVALID_CREDENTIALS_MESSAGE);
    }
}
