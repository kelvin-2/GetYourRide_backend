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

    /**
     * Cancels a trip by setting its status to CANCELLED.
     * @param tripId The ID of the trip.
     * @return The updated trip details.
     */
    TripResponse cancelTrip(Long tripId);

    /**
     * Completes a trip by setting its status to COMPLETED and recording arrival time.
     * @param tripId The ID of the trip.
     * @return The updated trip details.
     */
    TripResponse completeTrip(Long tripId);

    /**
     * Schedules a trip by setting its status to SCHEDULED.
     * @param tripId The ID of the trip.
     * @return The updated trip details.
     */
    TripResponse scheduleTrip(Long tripId);

    /**
     * Search for trips by departure and destination stops.
     * @param departure Departure stop address or keyword.
     * @param destination Destination stop address or keyword.
     * @return List of matching trips.
     */
    List<TripResponse> searchTrips(String departure, String destination);

    /**
     * Search for trips by departure and destination coordinates.
     * @param depLat Departure latitude.
     * @param depLng Departure longitude.
     * @param destLat Destination latitude.
     * @param destLng Destination longitude.
     * @param radiusInKm Radius in kilometers to search within.
     * @return List of matching trips.
     */
    List<TripResponse> searchTripsByCoordinates(Double depLat, Double depLng, Double destLat, Double destLng, Double radiusInKm);
}
