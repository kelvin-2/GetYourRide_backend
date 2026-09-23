package com.example1.getyourride.repository;

import com.example1.getyourride.entity.TripReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TripReviewRepository extends JpaRepository<TripReview, Long> {
    Optional<TripReview> findByBookingBookingId(Long bookingId);
}
