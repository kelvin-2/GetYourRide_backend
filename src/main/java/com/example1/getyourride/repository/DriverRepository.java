package com.example1.getyourride.repository;

import com.example1.getyourride.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    // Find driver by email (used for authentication & duplicate email checks)
    Optional<Driver> findByEmail(String email);

    // Check if an email exists directly without fetching the entire entity
    boolean existsByEmail(String email);

    // Fetch drivers based on verification status (useful for admin review dashboards)
    List<Driver> findByIsVerified(boolean isVerified);

    // Fetch drivers by role (e.g. "STUDENT_DRIVER" vs "SHUTTLE_DRIVER")
    List<Driver> findByRole(String role);

    // Shuttle driver login — matches by email AND role so student drivers can't use this endpoint
    Optional<Driver> findByEmailAndRole(String email, String role);
}