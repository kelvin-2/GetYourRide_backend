package com.example1.getyourride.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed review for a single trip, shown to the driver on the "View Ratings" screen.
 * One of these is returned per student who rated the trip.
 */
@Getter
@Builder
@AllArgsConstructor
public class TripReviewDetailResponse {
    private Long reviewId;
    private Long bookingId;
    private String studentName;   // the student who left the rating
    private Integer rating;       // 1-5
    private String review;        // free-text comment, may be empty
    private List<String> tags;    // e.g. "On time", "Friendly driver"
    private LocalDateTime reviewDate;
}
