package com.example1.getyourride.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating a new Trip.
 */
@Getter
@Setter
public class CreateTripRequest {

    @NotBlank(message = "Trip type is required")
    private String tripType; // e.g. "SHUTTLE" or "STUDENT_DRIVER"

    @NotBlank(message = "Departure stop is required")
    private String departureStop;

    @NotBlank(message = "Destination stop is required")
    private String destinationStop;

    private Double departureLat;
    private Double departureLng;
    private Double destinationLat;
    private Double destinationLng;

    @NotNull(message = "Departure time is required")
    private LocalDateTime departureTime;

    @NotNull(message = "Available seats is required")
    @Positive(message = "Available seats must be positive")
    private Integer availableSeats;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    /**
     * Intermediate stops for the trip. Optional - a trip may be created with none.
     *
     * <p>The {@code @Valid} is load-bearing: without it Bean Validation does not descend
     * into the list, so every constraint declared on {@link TripStopRequest} (including
     * {@code @NotNull} on latitude/longitude) was silently inert on this endpoint and
     * stops persisted with 0,0 coordinates. The element-level {@code @NotNull} is a
     * separate gap: {@code @Valid} cascades into list elements but skips null ones, so
     * a payload of {@code "stops": [null]} passed validation and then threw a
     * NullPointerException inside TripServiceImpl.createTrip.
     */
    @Valid
    private List<@NotNull(message = "Stop entries cannot be null") TripStopRequest> stops;
}
