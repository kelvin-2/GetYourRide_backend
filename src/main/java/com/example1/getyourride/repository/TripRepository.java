package com.example1.getyourride.repository;

import com.example1.getyourride.entity.Trip;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    @Override
    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<Trip> findAll();

    List<Trip> findByDriverDriverIdOrderByDepartureTimeDesc(Long driverId);
    
    @Override
    @EntityGraph(attributePaths = {"driver", "vehicle"})
    Optional<Trip> findById(Long id);
    
    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<Trip> findByStatus(String status);
    
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Trip t WHERE t.tripId = :tripId")
    Optional<Trip> findByIdForUpdate(@org.springframework.data.repository.query.Param("tripId") Long tripId);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<Trip> findByDriverDriverId(Long driverId);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<Trip> findByTripTypeIgnoreCaseAndStatus(String tripType, String status);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<Trip> findByTripTypeIgnoreCase(String tripType);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Trip t WHERE " +
            "LOWER(t.departureStop) LIKE LOWER(CONCAT('%', :departure, '%')) AND " +
            "LOWER(t.destinationStop) LIKE LOWER(CONCAT('%', :destination, '%')) AND " +
            "(:includeShuttle = true OR LOWER(t.tripType) != 'shuttle')")
    List<Trip> findByDepartureAndDestination(
            @org.springframework.data.repository.query.Param("departure") String departure,
            @org.springframework.data.repository.query.Param("destination") String destination,
            @org.springframework.data.repository.query.Param("includeShuttle") boolean includeShuttle);

    @EntityGraph(attributePaths = {"driver", "vehicle", "stops"})
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Trip t LEFT JOIN t.stops s WHERE " +
            "((t.departureLat BETWEEN :minDepLat AND :maxDepLat AND t.departureLng BETWEEN :minDepLng AND :maxDepLng) OR " +
            "(s.latitude BETWEEN :minDepLat AND :maxDepLat AND s.longitude BETWEEN :minDepLng AND :maxDepLng)) AND " +
            "((t.destinationLat BETWEEN :minDestLat AND :maxDestLat AND t.destinationLng BETWEEN :minDestLng AND :maxDestLng) OR " +
            "(s.latitude BETWEEN :minDestLat AND :maxDestLat AND s.longitude BETWEEN :minDestLng AND :maxDestLng)) AND " +
            "t.status = :status AND (:includeShuttle = true OR LOWER(t.tripType) != 'shuttle')")
    List<Trip> findNearbyTrips(
            @org.springframework.data.repository.query.Param("minDepLat") Double minDepLat,
            @org.springframework.data.repository.query.Param("maxDepLat") Double maxDepLat,
            @org.springframework.data.repository.query.Param("minDepLng") Double minDepLng,
            @org.springframework.data.repository.query.Param("maxDepLng") Double maxDepLng,
            @org.springframework.data.repository.query.Param("minDestLat") Double minDestLat,
            @org.springframework.data.repository.query.Param("maxDestLat") Double maxDestLat,
            @org.springframework.data.repository.query.Param("minDestLng") Double minDestLng,
            @org.springframework.data.repository.query.Param("maxDestLng") Double maxDestLng,
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("includeShuttle") boolean includeShuttle);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Trip t JOIN t.stops s WHERE s.student.studentId = :studentId")
    List<Trip> findTripsByStudentInStops(@org.springframework.data.repository.query.Param("studentId") Long studentId);

    // --- Shuttle driver profile queries ---

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Trip t WHERE t.driver.driverId = :driverId AND UPPER(t.status) = :status")
    int countByDriverIdAndStatus(@org.springframework.data.repository.query.Param("driverId") Long driverId,
                                 @org.springframework.data.repository.query.Param("status") String status);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Trip t WHERE t.driver.driverId = :driverId AND UPPER(t.status) IN ('SCHEDULED', 'IN_PROGRESS', 'CONFIRMED') ORDER BY t.departureTime DESC")
    List<Trip> findActiveTrips(@org.springframework.data.repository.query.Param("driverId") Long driverId);

    // Delete all trips for a driver (used for cascade deletion of driver profile)
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Trip t WHERE t.driver.driverId = :driverId")
    void deleteByDriverId(@org.springframework.data.repository.query.Param("driverId") Long driverId);
}
