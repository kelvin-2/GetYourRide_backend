package com.example1.getyourride.controller;

import com.example1.getyourride.dto.request.DriverLoginRequest;
import com.example1.getyourride.dto.request.DriverRegisterRequest;
import com.example1.getyourride.dto.response.AuthResponse;
import com.example1.getyourride.service.DriverAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/driver")
public class DriverAuthController {

    private final DriverAuthService driverAuthService;

    public DriverAuthController(DriverAuthService driverAuthService) {
        this.driverAuthService = driverAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody DriverRegisterRequest request) {
        return ResponseEntity.ok(driverAuthService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody DriverLoginRequest request) {
        return ResponseEntity.ok(driverAuthService.login(request));
    }
}