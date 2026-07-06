package com.example1.getyourride.controller;

import com.example1.getyourride.dto.request.TripStopRequest;
import com.example1.getyourride.dto.response.TripStopResponse;
import com.example1.getyourride.service.TripStopService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/stops")
public class TripStopController {

    private final TripStopService tripStopService;

    public TripStopController(TripStopService tripStopService) {
        this.tripStopService = tripStopService;
    }

    /**
     * Add a generic stop to a trip (typically for drivers).
     */
    @PostMapping
    public ResponseEntity<TripStopResponse> addStopToTrip(
            @PathVariable Long tripId,
            @Valid @RequestBody TripStopRequest request) {
        return ResponseEntity.ok(tripStopService.addStopToTrip(tripId, request));
    }

    /**
     * Add a student-specific stop to a trip.
     */
    @PostMapping("/student")
    public ResponseEntity<TripStopResponse> addStudentStopToTrip(
            @PathVariable Long tripId,
            @Valid @RequestBody TripStopRequest request) {
        return ResponseEntity.ok(tripStopService.addStudentStopToTrip(tripId, request));
    }

    /**
     * Get all stops for a specific trip.
     */
    @GetMapping
    public ResponseEntity<List<TripStopResponse>> getStopsByTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripStopService.getStopsByTrip(tripId));
    }

    /**
     * Remove a stop from a trip.
     */
    @DeleteMapping("/{stopId}")
    public ResponseEntity<Void> removeStop(@PathVariable Long stopId) {
        tripStopService.removeStop(stopId);
        return ResponseEntity.noContent().build();
    }
}
