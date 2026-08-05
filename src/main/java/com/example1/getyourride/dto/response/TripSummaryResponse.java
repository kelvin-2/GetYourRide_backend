package com.example1.getyourride.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TripSummaryResponse {
    private String currentTripRoute;
    private String currentTripStatus;
    private int scheduledTrips;
    private int inProgressTrips;
    private int completedTrips;
    private int cancelledTrips;
    private int studentsBookedToday;
    private int studentsBoardedToday;
}
