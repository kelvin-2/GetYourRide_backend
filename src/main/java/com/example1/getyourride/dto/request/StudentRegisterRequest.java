package com.example1.getyourride.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentRegisterRequest {

    @NotBlank(message = "Student number is required")
    private String studentNumber;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@mandela\\.ac\\.za$",
            message = "Email must be a valid @mandela.ac.za address"
    )
    private String email;

    private String phone;

    @NotBlank(message = "Password is required")
    private String password;
}