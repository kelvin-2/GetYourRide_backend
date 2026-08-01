package com.example1.getyourride.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.List;

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
    private String vehicleModel;
    private String vehicleColour;
    private Integer vehicleCapacity;
    private String tripType;
    private String departureStop;
    private Double departureLat;
    private Double departureLng;
    private String destinationStop;
    private Double destinationLat;
    private Double destinationLng;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer availableSeats;
    private BigDecimal price;
    private String status;
    private String bookingStatus;
    private String routeName;
    private String slotTime;
    private long studentId;
    private String studentName;
    private Double pickupDistance;
    private Double dropOffDistance;
    private List<TripStopResponse> stops;
}
