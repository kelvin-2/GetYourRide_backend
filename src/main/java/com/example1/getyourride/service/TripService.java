package com.example1.getyourride.service;

import java.util.List;

import com.example1.getyourride.dto.request.BookCarpoolRequest;
import com.example1.getyourride.dto.request.CreateTripRequest;
import com.example1.getyourride.dto.request.OfferRideRequest;
import com.example1.getyourride.dto.response.OfferRideResponse;
import com.example1.getyourride.dto.response.TripBookingResponse;
import com.example1.getyourride.dto.response.TripResponse;

/**
 * Service interface for managing Trips.
 */
public interface TripService {

    /**
     * Creates a new carpool trip for an authenticated, verified student driver.
     * @param email Driver's email extracted from JWT auth.
     * @param request Form data provided by the driver.
     * @return Confirmation response with created trip ID.
     */
    OfferRideResponse offerRide(String email, OfferRideRequest request);

    /**
     * Creates a new trip.
     * @param request Data for the new trip.
     * @return The created trip details.
     */
    TripResponse createTrip(CreateTripRequest request);

    /**
     * Book a carpool trip for a student.
     * @param tripId The ID of the trip to book.
     * @param request Booking details including pickup and drop-off stops.
     * @return The updated trip details.
     */
    TripResponse bookCarpool(Long tripId, BookCarpoolRequest request);
    
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
    List<TripResponse> getTripsByStatus(String status,String studentEmail);
    
    /**
     * Updates the status of a trip.
     * @param tripId The ID of the trip.
     * @param status The new status.
     * @return The updated trip details.
     */
    TripResponse updateTripStatus(Long tripId, String status);

    /**
     * Puts a trip on the road: ensures its route is precomputed, sets it to {@code IN_PROGRESS}
     * and resets the tracking cursor to the start of the route.
     *
     * <p>Exists because the documented start sequence was three separate calls
     * ({@code precompute-route}, then {@code PATCH /status?status=IN_PROGRESS}, relying on the
     * status change to seed tracking) and getting them out of order left a trip
     * {@code IN_PROGRESS} with no legs — a vehicle that never moves, with nothing in the response
     * to say why. Collapsing them into one action means a trip cannot be started half-configured.
     *
     * <p>Safe to call again on a trip that is already running: it restarts the trip from its
     * departure point and clears any stops marked as arrived on the previous run.
     *
     * @param tripId         The ID of the trip to start.
     * @param recomputeRoute Recompute the leg routes even if they already exist. Pass true after
     *                       editing the trip's stops; otherwise the existing legs are reused and
     *                       no OpenRouteService quota is spent.
     * @return The updated trip, including its seeded {@code currentLat}/{@code currentLng}.
     */
    TripResponse startTrip(Long tripId, boolean recomputeRoute);

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
     * @param studentEmail Email of the student searching (to check funding).
     * @return List of matching trips.
     */
    List<TripResponse> searchTrips(String departure, String destination, String studentEmail);

    /**
     * Search for trips by departure and destination coordinates.
     * @param depLat Departure latitude.
     * @param depLng Departure longitude.
     * @param destLat Destination latitude.
     * @param destLng Destination longitude.
     * @param radiusInKm Radius in kilometers to search within.
     * @param studentEmail Email of the student searching (to check funding).
     * @return List of matching trips.
     */
    
    List<TripResponse> searchTripsByCoordinates(Double depLat, Double depLng, Double destLat, Double destLng, Double radiusInKm, String studentEmail);
    List<TripResponse> getMyTrips(String email);

    // --- CHANGED (Phase 4 — booking wiring): new booking-specific operations ---

    /**
     * Cancels the authenticated student's booking on a given trip.
     *
     * <p>CHANGED: The Android client sends the tripId (not the bookingId) because that is what it
     * has on screen. The backend resolves the booking from (tripId + authenticated student email).
     *
     * @param tripId The trip the student wants to cancel their booking on.
     * @return The updated trip with the booking status set to CANCELLED.
     */
    TripResponse cancelBooking(Long tripId);

    /**
     * Returns bookings for the authenticated student, optionally filtered by booking_status.
     *
     * @param email  The student's email from the JWT.
     * @param status Optional filter: "CONFIRMED", "CANCELLED", "PENDING". Null means all.
     * @return Booking records ordered by booking date descending.
     */
    List<TripBookingResponse> getMyBookings(String email, String status);
}