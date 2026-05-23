package com.kstrinadka.securebankapi.unit;

import com.kstrinadka.securebankapi.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest extends AbstractUnitTest {

    private static final String SECRET = "very-long-secret-for-test-environment-32-bytes";
    private static final long EXPIRATION = 3600000L;

    private final JwtService jwtService = new JwtService(SECRET, EXPIRATION);

    @Test
    void generateTokenCreatesToken() {
        String token = jwtService.generateToken(1L);

        assertThat(token).isNotBlank();
    }

    @Test
    void extractUserIdReturnsSameUserId() {
        String token = jwtService.generateToken(42L);

        Long userId = jwtService.extractUserId(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void isTokenValidReturnsTrueForValidToken() {
        String token = jwtService.generateToken(1L);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void brokenTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("broken.token.value")).isFalse();
    }
}
