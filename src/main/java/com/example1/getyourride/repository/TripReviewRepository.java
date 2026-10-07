package com.example1.getyourride.repository;

import com.example1.getyourride.entity.TripReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripReviewRepository extends JpaRepository<TripReview, Long> {
    Optional<TripReview> findByBookingBookingId(Long bookingId);

    // --- Driver profile rating aggregation ---
    // Reviews are tied to a booking, which belongs to a trip, which belongs to a driver.
    // These walk that path to summarise all ratings a given driver has received.

    /** Average rating (1-5) across all reviews on this driver's trips, or null when none. */
    @Query("SELECT AVG(r.rating) FROM TripReview r WHERE r.booking.trip.driver.driverId = :driverId")
    Double findAverageRatingByDriverId(@Param("driverId") Long driverId);

    /** Number of ratings this driver has received. */
    @Query("SELECT COUNT(r) FROM TripReview r WHERE r.booking.trip.driver.driverId = :driverId")
    int countByDriverId(@Param("driverId") Long driverId);

    /** Distinct review tags across this driver's reviews (e.g. "On time", "Friendly driver"). */
    @Query("SELECT DISTINCT t FROM TripReview r JOIN r.tags t WHERE r.booking.trip.driver.driverId = :driverId")
    List<String> findDistinctTagsByDriverId(@Param("driverId") Long driverId);
}
