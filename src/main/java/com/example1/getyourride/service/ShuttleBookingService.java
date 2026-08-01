package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.BookingResponse;
import com.example1.getyourride.dto.response.ShuttleBookingSummaryResponse;

public interface ShuttleBookingService {
    ShuttleBookingSummaryResponse bookShuttle(Long tripId, String studentEmail);
    BookingResponse verifyAndBoard(String qrToken, Long bookingId, Long studentId, String expiry);
}
