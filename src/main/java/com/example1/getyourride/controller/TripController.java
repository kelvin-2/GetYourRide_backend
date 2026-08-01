package com.example1.getyourride.controller;

import com.example1.getyourride.dto.request.BookCarpoolRequest;
import com.example1.getyourride.dto.request.CreateTripRequest;
import com.example1.getyourride.dto.response.TripResponse;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example1.getyourride.dto.request.OfferRideRequest;
import com.example1.getyourride.dto.response.OfferRideResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * Controller for managing Trips.
 * Handles endpoints related to trip creation, retrieval, and status updates.
 */
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    /**
     * Create a new trip.
     * Only drivers (Student Driver or Shuttle Driver) should typically access this.
     * The driver must be logged in; their information is retrieved from the security context.
     * @param request The trip details.
     * @return The created trip.
     */
    @PostMapping
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody CreateTripRequest request) {
        return ResponseEntity.ok(tripService.createTrip(request));
    }

    /**
     * Book a carpool trip.
     * Called when the frontend fires "book carpool".
     * @param tripId The trip ID.
     * @param request The booking details (pickup/drop-off stops).
     * @return The updated trip details.
     */
    @PostMapping("/{tripId}/book")
    public ResponseEntity<TripResponse> bookCarpool(@PathVariable Long tripId, @Valid @RequestBody BookCarpoolRequest request) {
        return ResponseEntity.ok(tripService.bookCarpool(tripId, request));
    }

    /**
     * Get a trip by its ID.
     * @param id The trip ID.
     * @return The trip details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    /**
     * List all trips.
     * @return List of all trips.
     */
    @GetMapping
    public ResponseEntity<List<TripResponse>> getAllTrips() {
        return ResponseEntity.ok(tripService.getAllTrips());
    }

    /**
     * Get trips filtered by status.
     * @param status The status (e.g., SCHEDULED, IN_PROGRESS, COMPLETED).
     * @return List of matching trips.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TripResponse>> getTripsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(tripService.getTripsByStatus(status));
    }

    /**
     * Update the status of a trip.
     * Used by drivers to start or complete a trip.
     * @param id The trip ID.
     * @param status The new status.
     * @return The updated trip details.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TripResponse> updateTripStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(tripService.updateTripStatus(id, status));
    }

    /**
     * Cancel a trip.
     * Changes the status of the trip to CANCELLED.
     * @param id The trip ID.
     * @return The updated trip details.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TripResponse> cancelTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.cancelTrip(id));
    }

    /**
     * Mark a trip as completed.
     * Changes the status of the trip to COMPLETED and sets arrival time.
     * @param id The trip ID.
     * @return The updated trip details.
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<TripResponse> completeTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.completeTrip(id));
    }

    /**
     * Schedule a trip.
     * Changes the status of the trip to SCHEDULED.
     * @param id The trip ID.
     * @return The updated trip details.
     */
    @PatchMapping("/{id}/schedule")
    public ResponseEntity<TripResponse> scheduleTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.scheduleTrip(id));
    }

    /**
     * Search for trips by departure and destination stops.
     * Can search by address name (string) or by specific coordinates (lat/lng).
     * @param departure Departure stop address or keyword (optional if coordinates provided).
     * @param destination Destination stop address or keyword (optional if coordinates provided).
     * @param depLat Departure latitude (optional).
     * @param depLng Departure longitude (optional).
     * @param destLat Destination latitude (optional).
     * @param destLng Destination longitude (optional).
     * @param radius Radius in km for coordinate search (default 2km).
     * @return List of matching trips.
     */
    @GetMapping("/search")
    public ResponseEntity<List<TripResponse>> searchTrips(
            @RequestParam(required = false) String departure,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) Double depLat,
            @RequestParam(required = false) Double depLng,
            @RequestParam(required = false) Double destLat,
            @RequestParam(required = false) Double destLng,
            @RequestParam(defaultValue = "2.0") Double radius,
            Authentication authentication) {
        
        String email = authentication != null ? authentication.getName() : null;
        
        if (depLat != null && depLng != null && destLat != null && destLng != null) {
            return ResponseEntity.ok(tripService.searchTripsByCoordinates(depLat, depLng, destLat, destLng, radius, email));
        }
        
        if (departure != null && destination != null) {
            return ResponseEntity.ok(tripService.searchTrips(departure, destination, email));
        }

        return ResponseEntity.badRequest().build();
    }
    /**
     * POST /api/trips/offer
     * Allows a verified student driver to post a carpool ride.
     */
    @PostMapping("/offer")
    public ResponseEntity<OfferRideResponse> offerRide(
            @RequestBody OfferRideRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        OfferRideResponse response = tripService.offerRide(email, request);
        return ResponseEntity.ok(response);
    }
    /**
 * GET /api/trips/my-trips
 * Fetches all trips created by the currently authenticated driver.
 */
@GetMapping("/my-trips")
public ResponseEntity<List<TripResponse>> getMyTrips(Authentication authentication) {
    String email = authentication.getName();
    return ResponseEntity.ok(tripService.getMyTrips(email));
}
}
