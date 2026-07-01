package com.example1.getyourride.repository;

import com.example1.getyourride.entity.Trip;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Trip entity.
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    @Override
    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<Trip> findAll();

    @Override
    @EntityGraph(attributePaths = {"driver", "vehicle"})
    Optional<Trip> findById(Long id);
    
    /**
     * Find trips by their status.
     * @param status The status of the trip (e.g., SCHEDULED, IN_PROGRESS).
     * @return List of trips with the given status.
     */
    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<Trip> findByStatus(String status);
    
    /**
     * Find trips by driver ID.
     * @param driverId The ID of the driver.
     * @return List of trips for the given driver.
     */
    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<Trip> findByDriverDriverId(Long driverId);
}
