package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.request.DriverLoginRequest;
import com.example1.getyourride.dto.request.DriverRegisterRequest;
import com.example1.getyourride.dto.response.AuthResponse;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.security.JwtUtil;
import com.example1.getyourride.service.DriverAuthService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DriverAuthServiceImpl implements DriverAuthService {

    private final DriverRepository driverRepository;
    private final JwtUtil jwtUtil;

    public DriverAuthServiceImpl(DriverRepository driverRepository, JwtUtil jwtUtil) {
        this.driverRepository = driverRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthResponse register(DriverRegisterRequest request) {
        if (driverRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        Driver driver = new Driver();
        driver.setFirstName(request.getFirstName());
        driver.setLastName(request.getLastName());
        driver.setEmail(request.getEmail());
        driver.setPhone(request.getPhone());
        driver.setPassword(request.getPassword()); // plain text, by project decision
        driver.setRole(request.getRole());
        driver.setIsVerified(false);
        driver.setTotalTrips(0);

        Driver saved = driverRepository.save(driver);
        return buildAuthResponse(saved);
    }

    @Override
    public AuthResponse login(DriverLoginRequest request) {
        Driver driver = driverRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!driver.getPassword().equals(request.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        return buildAuthResponse(driver);
    }

    private AuthResponse buildAuthResponse(Driver driver) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", driver.getRole());
        claims.put("isVerified", driver.getIsVerified());

        String token = jwtUtil.generateToken(driver.getDriverId(), driver.getEmail(), "DRIVER", claims);

        return AuthResponse.builder()
                .token(token)
                .type("DRIVER")
                .id(driver.getDriverId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail())
                .role(driver.getRole())
                .isVerified(driver.getIsVerified())
                .build();
    }
}