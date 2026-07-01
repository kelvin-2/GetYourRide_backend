package com.example1.getyourride.service;

import com.example1.getyourride.dto.request.CreateTripRequest;
import com.example1.getyourride.dto.response.TripResponse;

import java.util.List;

/**
 * Service interface for managing Trips.
 */
public interface TripService {
    
    /**
     * Creates a new trip.
     * @param request Data for the new trip.
     * @return The created trip details.
     */
    TripResponse createTrip(CreateTripRequest request);
    
    /**
     * Gets details of a specific trip.
     * @param tripId The ID of the trip.
     * @return The trip details.
     */
    TripResponse getTripById(Long tripId);
    
    /**
     * Lists all trips.
     * @return List of all trips.
     */
    List<TripResponse> getAllTrips();
    
    /**
     * Lists trips by status.
     * @param status The status filter.
     * @return List of trips with the matching status.
     */
    List<TripResponse> getTripsByStatus(String status);
    
    /**
     * Updates the status of a trip.
     * @param tripId The ID of the trip.
     * @param status The new status.
     * @return The updated trip details.
     */
    TripResponse updateTripStatus(Long tripId, String status);
}
