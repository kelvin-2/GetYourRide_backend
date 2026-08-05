package com.example1.getyourride.repository;

import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Returns the first vehicle assigned to this driver (for shuttle driver profile)
    Optional<Vehicle> findFirstByDriverDriverId(Long driverId);

    // Delete all vehicles assigned to a driver (used for cascade deletion)
    @Modifying
    @Query("DELETE FROM Vehicle v WHERE v.driver = :driver")
    void deleteByDriver(@Param("driver") Driver driver);
}