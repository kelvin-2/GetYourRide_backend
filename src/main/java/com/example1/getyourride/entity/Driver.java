package com.example1.getyourride.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entity mapping for the 'driver' table in MySQL shuttle_db.
 */
@Entity
@Table(name = "driver")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;
    
    @Column(name = "student_number")
    private String studentNumber;

    @Column(name = "role", nullable = false)
    private String role;

    // Kept as Boolean (wrapper) so Lombok generates getIsVerified() and setIsVerified()
    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "password", nullable = false)
    private String password;

    @Builder.Default
    @Column(name = "total_trips", nullable = false)
    private int totalTrips = 0;
}