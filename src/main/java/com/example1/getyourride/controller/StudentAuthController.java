package com.example1.getyourride.controller;

import com.example1.getyourride.dto.request.StudentLoginRequest;
import com.example1.getyourride.dto.request.StudentRegisterRequest;
import com.example1.getyourride.dto.response.AuthResponse;
import com.example1.getyourride.service.StudentAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/student")
public class StudentAuthController {

    private final StudentAuthService studentAuthService;

    public StudentAuthController(StudentAuthService studentAuthService) {
        this.studentAuthService = studentAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody StudentRegisterRequest request) {
        return ResponseEntity.ok(studentAuthService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody StudentLoginRequest request) {
        return ResponseEntity.ok(studentAuthService.login(request));
    }
}