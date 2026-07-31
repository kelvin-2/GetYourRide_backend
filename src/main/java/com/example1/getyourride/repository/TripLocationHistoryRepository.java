package com.example1.getyourride.repository;

import com.example1.getyourride.entity.TripLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
