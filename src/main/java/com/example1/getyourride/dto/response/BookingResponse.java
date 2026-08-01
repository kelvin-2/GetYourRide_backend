package com.example1.getyourride.dto.response;

public class BookingResponse {
    private Long bookingId;
    private Long tripId;
    private String status;
    private String message;

    public BookingResponse(Long bookingId, Long tripId, String status, String message) {
        this.bookingId = bookingId;
        this.tripId = tripId;
        this.status = status;
        this.message = message;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getTripId() {
        return tripId;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
