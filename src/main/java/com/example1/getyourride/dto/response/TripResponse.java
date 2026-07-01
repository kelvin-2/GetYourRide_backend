package com.example1.getyourride.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Trip details.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {
    private Long tripId;
    private Long driverId;
    private String driverName;
    private String registrationNumber;
    private String tripType;
    private String departureStop;
    private String destinationStop;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer availableSeats;
    private BigDecimal price;
    private String status;
}
