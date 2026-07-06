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

    /**
     * Search for trips by departure and destination stop.
     * @param departure Departure stop keyword.
     * @param destination Destination stop keyword.
     * @return List of matching trips.
     */
    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<Trip> findByDepartureStopContainingIgnoreCaseAndDestinationStopContainingIgnoreCase(String departure, String destination);

    /**
     * Search for trips by coordinates with a specific radius (approximate using bounding box).
     * Now includes checking stops as well.
     */
    @EntityGraph(attributePaths = {"driver", "vehicle", "stops"})
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Trip t LEFT JOIN t.stops s WHERE " +
            "((t.departureLat BETWEEN :minDepLat AND :maxDepLat AND t.departureLng BETWEEN :minDepLng AND :maxDepLng) OR " +
            "(s.latitude BETWEEN :minDepLat AND :maxDepLat AND s.longitude BETWEEN :minDepLng AND :maxDepLng)) AND " +
            "((t.destinationLat BETWEEN :minDestLat AND :maxDestLat AND t.destinationLng BETWEEN :minDestLng AND :maxDestLng) OR " +
            "(s.latitude BETWEEN :minDestLat AND :maxDestLat AND s.longitude BETWEEN :minDestLng AND :maxDestLng)) AND " +
            "t.status = :status")
    List<Trip> findNearbyTrips(
            @org.springframework.data.repository.query.Param("minDepLat") Double minDepLat,
            @org.springframework.data.repository.query.Param("maxDepLat") Double maxDepLat,
            @org.springframework.data.repository.query.Param("minDepLng") Double minDepLng,
            @org.springframework.data.repository.query.Param("maxDepLng") Double maxDepLng,
            @org.springframework.data.repository.query.Param("minDestLat") Double minDestLat,
            @org.springframework.data.repository.query.Param("maxDestLat") Double maxDestLat,
            @org.springframework.data.repository.query.Param("minDestLng") Double minDestLng,
            @org.springframework.data.repository.query.Param("maxDestLng") Double maxDestLng,
            @org.springframework.data.repository.query.Param("status") String status);
}
