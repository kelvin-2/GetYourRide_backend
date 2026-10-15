package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.BookingResponse;
import com.example1.getyourride.dto.response.ShuttleAvailabilityResponse;
import com.example1.getyourride.dto.response.ShuttleBookingSummaryResponse;

import java.time.LocalDate;

public interface ShuttleBookingService {
    ShuttleBookingSummaryResponse bookShuttle(Long tripId, String studentEmail);
    BookingResponse verifyAndBoard(String qrToken, Long bookingId, Long studentId, String expiry);
    ShuttleAvailabilityResponse getAvailableShuttles(LocalDate date);
}
