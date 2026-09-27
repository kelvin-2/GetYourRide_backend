package com.example1.getyourride.repository;

import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.entity.TripLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Data access for the recorded position trail of a trip.
 */
@Repository
public interface TripLocationHistoryRepository extends JpaRepository<TripLocationHistory, Long> {

    /** Full trail for a trip in chronological order. */
    List<TripLocationHistory> findByTripTripIdOrderByRecordedAtAsc(Long tripId);

    /**
     * Most recent positions first. Used to answer "where has this vehicle just been" without loading
     * an entire completed trip's trail.
     */
    List<TripLocationHistory> findTop50ByTripTripIdOrderByRecordedAtDesc(Long tripId);

    long countByTripTripId(Long tripId);

    /**
     * Removes the position trail for every given trip. Used when a driver profile is deleted:
     * the {@code trip_id} FK is NOT NULL, so these rows must go before their trips can be deleted.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    void deleteByTripIn(List<Trip> trips);
}
