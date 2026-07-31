package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.AuthResponse;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Service implementation handling unified authentication for both Students and Drivers.
 */
@Service
public class AuthServiceImp {

    private final StudentRepository studentRepo;
    private final DriverRepository driverRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // Constructor Injection
    public AuthServiceImp(
            StudentRepository studentRepo,
            DriverRepository driverRepo,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.studentRepo = studentRepo;
        this.driverRepo = driverRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    //TODO: complete logic for the student to be able to register 
     @Transactional
    public AuthResponse register(String email, String password, String firstName, String lastName) {
        // Registration logic can be implemented here if needed
        throw new UnsupportedOperationException("Registration is not implemented in this unified service.");
    }

    /**
     * Unified Login: Checks Student table first. If absent, falls back to Driver table.
     *
     * @param email User's university or registered email.
     * @param password Raw plain text password.
     * @return AuthResponse containing JWT token, role, and user details.
     */
   
    @Transactional(readOnly = true)
    public AuthResponse login(String email, String password) {
        
        // 1. Check Student Table First
        Optional<Student> studentOpt = studentRepo.findByEmail(email);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();

            // Validate password against hashed password in DB
            if (!passwordEncoder.matches(password, student.getPassword())) {
                throw new IllegalArgumentException("Invalid email or password.");
            }

            // Generate JWT Token for Student
            String token = jwtUtil.generateToken(
                    student.getStudentId(),
                    student.getEmail(),
                    "STUDENT",
                    Map.of()
            );

            return AuthResponse.builder()
                    .token(token)
                    .type("STUDENT")
                    .id(student.getStudentId())
                    .firstName(student.getFirstName())
                    .lastName(student.getLastName())
                    .email(student.getEmail())
                    .isFunded(student.getIsFunded())
                    .role(null)
                    .isVerified(null)
                    .build();
        }

        // 2. Check Driver Table if not found in Student table
        Optional<Driver> driverOpt = driverRepo.findByEmail(email);
        if (driverOpt.isPresent()) {
            Driver driver = driverOpt.get();

            // Validate password
            if (!passwordEncoder.matches(password, driver.getPassword())) {
                throw new IllegalArgumentException("Invalid email or password.");
            }

            // Determine driver status (Boolean null-safe check)
            boolean isVerified = Boolean.TRUE.equals(driver.getIsVerified());
            String role = isVerified ? "DRIVER_APPROVED" : "DRIVER_PENDING";

            // Generate JWT Token for Driver
            String token = jwtUtil.generateToken(
                    driver.getDriverId(),
                    driver.getEmail(),
                    "DRIVER",
                    Map.of("role", role)
            );

            return AuthResponse.builder()
                    .token(token)
                    .type("DRIVER")
                    .id(driver.getDriverId())
                    .firstName(driver.getFirstName())
                    .lastName(driver.getLastName())
                    .email(driver.getEmail())
                    .isFunded(null)
                    .role(role)
                    .isVerified(isVerified)
                    .build();
        }

        // 3. Email not found in either table
        throw new IllegalArgumentException("Account does not exist.");
    }
}