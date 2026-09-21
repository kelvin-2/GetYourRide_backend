package com.example1.getyourride.service;

import com.example1.getyourride.dto.request.TripRatingRequest;
import com.example1.getyourride.entity.TripReview;

public interface TripReviewService {
    TripReview rateTrip(TripRatingRequest request, String studentEmail);
}
