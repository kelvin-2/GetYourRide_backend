package com.example1.getyourride.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for a student booking a seat on an existing carpool trip.
 *
 * <p>Both stops are annotated {@code @Valid} because
 * {@code TripServiceImpl.bookCarpool} persists them as {@code trip_stop} rows. Without
 * the cascade the nested {@link TripStopRequest} constraints never ran, so this endpoint
 * was a second route to the same silent 0,0-coordinate save that
 * {@code POST /api/trips} suffered from.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookCarpoolRequest {

    @NotNull(message = "Pickup stop is required")
    @Valid
    private TripStopRequest pickupStop;

    /** Optional - students may ride to the driver's stated destination. */
    @Valid
    private TripStopRequest dropOffStop;
}
