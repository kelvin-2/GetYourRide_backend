package com.example1.getyourride.controller;

import com.example1.getyourride.dto.request.TripRatingRequest;
import com.example1.getyourride.entity.TripReview;
import com.example1.getyourride.service.TripReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ratings")
public class TripReviewController {

    private final TripReviewService tripReviewService;

    public TripReviewController(TripReviewService tripReviewService) {
        this.tripReviewService = tripReviewService;
    }

    @PostMapping("/rate")
    public ResponseEntity<TripReview> rateTrip(
            @Valid @RequestBody TripRatingRequest request,
            Authentication authentication) {
        String studentEmail = authentication.getName();
        TripReview review = tripReviewService.rateTrip(request, studentEmail);
        return ResponseEntity.ok(review);
    }
}
