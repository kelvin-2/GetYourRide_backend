package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.request.StudentLoginRequest;
import com.example1.getyourride.dto.request.StudentRegisterRequest;
import com.example1.getyourride.dto.response.AuthResponse;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.security.JwtUtil;
import com.example1.getyourride.service.StudentAuthService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StudentAuthServiceImpl implements StudentAuthService {

    private final StudentRepository studentRepository;
    private final JwtUtil jwtUtil;

    public StudentAuthServiceImpl(StudentRepository studentRepository, JwtUtil jwtUtil) {
        this.studentRepository = studentRepository;
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
}