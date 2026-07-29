package com.example1.getyourride.config;

import com.example1.getyourride.security.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void passwordEncoderBeanShouldBeAvailable() {
        SecurityConfig config = new SecurityConfig(mock(JwtAuthFilter.class));

        PasswordEncoder passwordEncoder = config.passwordEncoder();

        assertNotNull(passwordEncoder);
        assertTrue(passwordEncoder.matches("secret", passwordEncoder.encode("secret")));
    }
}
