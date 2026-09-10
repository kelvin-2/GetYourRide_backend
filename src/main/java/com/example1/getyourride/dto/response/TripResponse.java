package com.example1.getyourride.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    /**
     * CHANGED (Phase 4 — booking wiring): added bookingId so the frontend can reference the
     * booking row directly (e.g. to cancel it) without a separate lookup.
     */
    private Long bookingId;

    // --- Live tracking state -------------------------------------------------
    // Mirrors the trip.current_* columns the simulation engine writes on every tick.
    //
    // These exist so GET /api/trips/{id} is a usable polling fallback when the STOMP socket is
    // unavailable, which tracking documentation section 5 (Phase 5) requires. Without them the
    // Android TrackingViewModel's refreshTripDetails() has no position field to read, so it has
    // to preserve whatever the socket last delivered and a client that never connected shows no
    // vehicle at all. Null until the trip starts moving.

    /** Vehicle's most recently published latitude, or null if the trip has not started. */
    private Double currentLat;

    /** Vehicle's most recently published longitude, or null if the trip has not started. */
    private Double currentLng;

    /**
     * Zero-based index of the leg the vehicle is currently driving, matching the {@code legIndex}
     * on {@code LOCATION_UPDATE} messages so a polling client and a subscribed client agree.
     */
    private Integer currentLegIndex;

    private String bookingStatus;
    private String routeName;
    private String slotTime;
    private long studentId;
    private String studentName;
    private Double pickupDistance;
    private Double dropOffDistance;
    private List<TripStopResponse> stops;
}
