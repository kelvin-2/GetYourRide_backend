package com.example1.getyourride.service;

import com.example1.getyourride.dto.request.StudentLoginRequest; // Or rename to LoginRequest
import com.example1.getyourride.dto.request.StudentRegisterRequest;
import com.example1.getyourride.dto.response.AuthResponse;

/**
 * Unified Authentication Service interface for Student registration 
 * and dual Student/Driver login processing.
 */
public interface AuthService {

    /**
     * Registers a new student account.
     */
    AuthResponse register(StudentRegisterRequest request);

    /**
     * Unified login endpoint for both Students and Drivers.
     * Evaluates Student table first, falling back to Driver table.
     */
    AuthResponse login(StudentLoginRequest request);
}