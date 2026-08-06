package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.request.DriverLoginRequest;
import com.example1.getyourride.dto.request.DriverRegisterRequest;
import com.example1.getyourride.dto.response.AuthResponse;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.security.JwtUtil;
import com.example1.getyourride.service.DriverAuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * Implementation of {@link DriverAuthService} managing driver accounts and auth tokens.
 */
@Service
public class DriverAuthServiceImpl implements DriverAuthService {

    private final DriverRepository driverRepo;
    private final StudentRepository studentRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public DriverAuthServiceImpl(
            DriverRepository driverRepo,
            StudentRepository studentRepo,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.driverRepo = driverRepo;
        this.studentRepo = studentRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registers a new driver directly.
     */
    @Override
    @Transactional
    public AuthResponse register(DriverRegisterRequest request) {
        // Check if email is already taken in Driver table
        if (driverRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email is already registered as a driver.");
        }

        // Check if email is already taken in Student table
        if (studentRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email is already registered as a student.");
        }

        // Build & save driver entity
        Driver driver = Driver.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role("STUDENT_DRIVER")
                .isVerified(false)
                .joinDate(LocalDate.now())
                .password(request.getPassword())
                .totalTrips(0)
                .build();

        Driver savedDriver = driverRepo.save(driver);

        // Generate JWT token with DRIVER_PENDING status
        String token = jwtUtil.generateToken(
                savedDriver.getDriverId(),
                savedDriver.getEmail(),
                "DRIVER",
                Map.of("role", "DRIVER_PENDING")
        );

        return AuthResponse.builder()
                .token(token)
                .type("DRIVER")
                .id(savedDriver.getDriverId())
                .firstName(savedDriver.getFirstName())
                .lastName(savedDriver.getLastName())
                .email(savedDriver.getEmail())
                .phone(savedDriver.getPhone())
                .isFunded(null)
                .role("DRIVER_PENDING")
                .isVerified(false)
                .build();
    }

    /**
     * Authenticates a driver directly via driver-specific login endpoint.
     * If the driver's role is SHUTTLE_DRIVER, returns type = "SHUTTLE_DRIVER"
     * so the Android app routes to the shuttle/boarding home screen.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(DriverLoginRequest request) {
        Driver driver = driverRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Driver account does not exist."));

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), driver.getPassword()) && !request.getPassword().equals(driver.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        // Null-safe boolean verification check
        boolean isVerified = Boolean.TRUE.equals(driver.getIsVerified());

        // Determine type and role based on the driver's actual role in the database
        String type;
        String role;
        if ("SHUTTLE_DRIVER".equals(driver.getRole())) {
            if (!isVerified) {
                throw new SecurityException("Account not verified. Contact admin.");
            }
            type = "SHUTTLE_DRIVER";
            role = "SHUTTLE_DRIVER";
        } else {
            type = "DRIVER";
            role = isVerified ? "DRIVER_APPROVED" : "DRIVER_PENDING";
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(
                driver.getDriverId(),
                driver.getEmail(),
                type,
                Map.of("role", role)
        );

        return AuthResponse.builder()
                .token(token)
                .type(type)
                .id(driver.getDriverId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail())
                .phone(driver.getPhone())
                .studentNumber("")
                .isFunded(null)
                .role(role)
                .isVerified(isVerified)
                .build();
    }
}