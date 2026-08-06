package com.example1.getyourride.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a student's booking record.
 *
 * <p>Returned by the /my-bookings endpoint so the frontend can filter by booking status
 * (Confirmed, Cancelled, Pending) and display the booking alongside the trip it belongs to.
 *
 * <p>Carries the full TripResponse nested inside so the client gets trip details (departure,
 * destination, driver info, stops) in a single call rather than needing a follow-up GET per trip.
 *
 * <p><b>CHANGED (Phase 4 — booking wiring):</b> New DTO. Before this, the frontend only had
 * bookingStatus as a loose field on TripResponse set during getMyTrips; now it has a
 * first-class booking object with its own id, date, and status that the client can use
 * to cancel or filter independently.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripBookingResponse {

    /** Primary key of the trip_booking row — needed by the cancel endpoint. */
    private Long bookingId;

    /** The trip this booking is for, with full details so no follow-up call is needed. */
    private TripResponse trip;

    /** When the booking was made. */
    private LocalDateTime bookingDate;

    /**
     * Current booking lifecycle state: Pending, Confirmed, Cancelled.
     *
     * <p>The frontend uses this to filter: "show me only Confirmed bookings" gives the student
     * their active rides; "show me Cancelled" gives their history. The value comes from the
     * BookingStatus enum serialised through BookingStatusConverter.
     */
    private String bookingStatus;
}
