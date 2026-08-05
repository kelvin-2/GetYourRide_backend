package com.example1.getyourride.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActiveTripResponse {
    private Long tripId;
    private String departureStop;
    private String destinationStop;
    private String departureTime;
    private String arrivalTime;
    private String status;
    private int capacity;
    private String registrationNumber;
    private int totalBooked;
    private int totalBoarded;
}
