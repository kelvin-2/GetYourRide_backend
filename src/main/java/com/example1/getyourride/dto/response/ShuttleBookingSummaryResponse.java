package com.example1.getyourride.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShuttleBookingSummaryResponse {
    private BookingResponse bookingConfirmation;
    private List<TripResponse> myConfirmedShuttles;
    private List<TripResponse> allShuttleTrips;
}
