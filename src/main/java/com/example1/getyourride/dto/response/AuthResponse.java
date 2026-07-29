package com.example1.getyourride.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type;       // "STUDENT" or "DRIVER"
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String studentNumber;
    private String phone;

    // student-specific (null for drivers)
    private Boolean isFunded;

    // driver-specific (null for students)
    private String role;
    private Boolean isVerified;
}