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
    public AuthResponse register(StudentRegisterRequest request) {
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        if (studentRepository.existsByStudentNumber(request.getStudentNumber())) {
            throw new BadRequestException("Student number already registered");
        }

        Student student = new Student();
        student.setStudentNumber(request.getStudentNumber());
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setPassword(request.getPassword()); // plain text, by project decision
        student.setIsFunded(request.getIsFunded());

        Student saved = studentRepository.save(student);
        return buildAuthResponse(saved);
    }

    @Override
    public AuthResponse login(StudentLoginRequest request) {
        Student student = studentRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // Add these lines
        System.out.println("ID: " + student.getStudentId());
        System.out.println("Student Number: " + student.getStudentNumber());
        System.out.println("First Name: " + student.getFirstName());
        System.out.println("Email: " + student.getEmail());

        if (!student.getPassword().equals(request.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        return buildAuthResponse(student);
    }

    private AuthResponse buildAuthResponse(Student student) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("isFunded", student.getIsFunded());
        claims.put("role", "STUDENT");

        String token = jwtUtil.generateToken(student.getStudentId(), student.getEmail(), "STUDENT", claims);

        return AuthResponse.builder()
                .token(token)
                .type("STUDENT")
                .id(student.getStudentId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .isFunded(student.getIsFunded())
                .role("STUDENT")
                .studentNumber(student.getStudentNumber())
                .phone(student.getPhone())
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

            if (!passwordEncoder.matches(password, student.getPassword())) {
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

            if (!passwordEncoder.matches(password, driver.getPassword())) {
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
                    .isFunded(null)
                    .role(role)
                    .isVerified(isVerified)
                    .build();
        }

        // 3. User email not found in either table
        throw new IllegalArgumentException("Account does not exist.");
    }
}
