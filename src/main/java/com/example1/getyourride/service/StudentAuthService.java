package com.example1.getyourride.service;

import com.example1.getyourride.dto.request.StudentLoginRequest;
import com.example1.getyourride.dto.request.StudentRegisterRequest;
import com.example1.getyourride.dto.response.AuthResponse;

public interface StudentAuthService {
    AuthResponse register(StudentRegisterRequest request);
    AuthResponse login(StudentLoginRequest request);
}