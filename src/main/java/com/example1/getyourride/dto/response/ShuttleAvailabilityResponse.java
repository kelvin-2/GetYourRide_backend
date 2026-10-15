package com.example1.getyourride.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Shuttle availability for a single day. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShuttleAvailabilityResponse {
    private LocalDate date;
    private boolean available;
    /** Friendly message, populated when no shuttles run on the date. */
    private String message;
    private List<TripResponse> trips;
}
