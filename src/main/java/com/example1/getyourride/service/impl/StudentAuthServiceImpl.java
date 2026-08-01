package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.request.StudentLoginRequest;
import com.example1.getyourride.dto.request.StudentRegisterRequest;
import com.example1.getyourride.dto.response.AuthResponse;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.security.JwtUtil;
import com.example1.getyourride.service.StudentAuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class StudentAuthServiceImpl implements StudentAuthService {

    private final StudentRepository studentRepo;
    private final DriverRepository driverRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public StudentAuthServiceImpl(
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

    @Override
    @Transactional
    public AuthResponse register(StudentRegisterRequest request) {
        // Check if email is already taken in Student table
        if (studentRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email is already registered as a student.");
        }

        // Check if email is already taken in Driver table
        if (driverRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email is already registered as a driver.");
        }

        // Check if student number is already taken
        if (studentRepo.existsByStudentNumber(request.getStudentNumber())) {
            throw new IllegalStateException("Student number " + request.getStudentNumber() + " is already registered.");
        }

        // Build & save student entity
        Student student = new Student();
        student.setStudentNumber(request.getStudentNumber());
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setIsFunded(request.getIsFunded() != null ? request.getIsFunded() : false);
        student.setPassword(passwordEncoder.encode(request.getPassword()));

        Student savedStudent = studentRepo.save(student);

        // Generate JWT token
        String token = jwtUtil.generateToken(
                savedStudent.getStudentId(),
                savedStudent.getEmail(),
                "STUDENT",
                Map.of()
        );

        return AuthResponse.builder()
                .token(token)
                .type("STUDENT")
                .id(savedStudent.getStudentId())
                .firstName(savedStudent.getFirstName())
                .lastName(savedStudent.getLastName())
                .email(savedStudent.getEmail())
                .studentNumber(savedStudent.getStudentNumber())
                .phone(savedStudent.getPhone())
                .isFunded(savedStudent.getIsFunded())
                .role(null)
                .isVerified(null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(StudentLoginRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();

        // 1. Try finding account in Student table first
        Optional<Student> studentOpt = studentRepo.findByEmail(email);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();

            if (!passwordEncoder.matches(password, student.getPassword()) && !password.equals(student.getPassword())) {
                throw new IllegalArgumentException("Invalid email or password.");
            }

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
                    .studentNumber(student.getStudentNumber())
                    .phone(student.getPhone())
                    .isFunded(student.getIsFunded())
                    .role(null)
                    .isVerified(null)
                    .build();
        }

        // 2. Fallback: Check Driver table if not found in Student table
        Optional<Driver> driverOpt = driverRepo.findByEmail(email);
        if (driverOpt.isPresent()) {
            Driver driver = driverOpt.get();

            // BLOCK DEACTIVATED ACCOUNTS HERE
            if ("DEACTIVATED".equalsIgnoreCase(driver.getRole())) {
                throw new IllegalArgumentException("This account has been deactivated. Contact support if you need help.");
            }

            if (!passwordEncoder.matches(password, driver.getPassword()) && !password.equals(driver.getPassword())) {
                throw new IllegalArgumentException("Invalid email or password.");
            }

            boolean isVerified = Boolean.TRUE.equals(driver.getIsVerified());
            String role = isVerified ? "DRIVER_APPROVED" : "DRIVER_PENDING";

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
                    .phone(driver.getPhone())
                    .isFunded(null)
                    .role(role)
                    .isVerified(isVerified)
                    .build();
        }

        // 3. User email not found in either table
        throw new IllegalArgumentException("Account does not exist.");
    }
}