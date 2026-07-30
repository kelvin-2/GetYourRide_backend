package com.example1.getyourride.service;

import com.example1.getyourride.dto.request.DriverLoginRequest;
import com.example1.getyourride.dto.request.DriverRegisterRequest;
import com.example1.getyourride.dto.response.AuthResponse;

/**
 * Service interface handling driver-specific registration and direct driver logins.
 */
public interface DriverAuthService {
    AuthResponse register(DriverRegisterRequest request);
    AuthResponse login(DriverLoginRequest request);
}