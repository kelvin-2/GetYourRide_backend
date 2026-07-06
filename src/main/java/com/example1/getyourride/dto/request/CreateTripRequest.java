package com.example1.getyourride.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    private List<TripStopRequest> stops;
}
