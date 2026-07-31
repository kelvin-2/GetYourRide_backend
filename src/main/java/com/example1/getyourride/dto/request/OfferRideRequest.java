package com.example1.getyourride.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Payload sent by the mobile app when a student driver offers a ride.
 */
@Getter
@Setter
public class OfferRideRequest {
    private String pickupLocation;
    private String destination;
    private String rideDate;       // Format: "YYYY-MM-DD"
    private String rideTime;       // Format: "HH:mm"
    private int availableSeats;
    private double farePerSeat;
    private Double pickupLat;      // Latitude coordinate
    private Double pickupLng;      // Longitude coordinate
    private Double destinationLat; // Latitude coordinate
    private Double destinationLng; // Longitude coordinate
}