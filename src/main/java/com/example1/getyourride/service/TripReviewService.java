package com.example1.getyourride.service;

import com.example1.getyourride.dto.request.TripRatingRequest;
import com.example1.getyourride.dto.response.TripReviewDetailResponse;
import com.example1.getyourride.entity.TripReview;

import java.util.List;

public interface TripReviewService {
    TripReview rateTrip(TripRatingRequest request, String studentEmail);

    /**
     * Returns all reviews left on the given trip, for the driver who owns it.
     * @param tripId      the trip whose reviews are requested
     * @param driverEmail the authenticated driver's email (from JWT); must own the trip
     */
    List<TripReviewDetailResponse> getReviewsForTrip(Long tripId, String driverEmail);
}
