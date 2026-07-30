package com.example1.getyourride.repository;

import com.example1.getyourride.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Vehicle persistence.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // Find vehicle by plate registration
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumber(String registrationNumber);

    // Derived Spring Data query matching driver.driverId field hierarchy
    List<Vehicle> findByDriverDriverId(Long driverId);
}