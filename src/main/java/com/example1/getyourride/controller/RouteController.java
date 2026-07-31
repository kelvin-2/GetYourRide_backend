package com.example1.getyourride.controller;

import com.example1.getyourride.dto.response.RouteResponse;
import com.example1.getyourride.service.TripRouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only route lookup for a ride.
 *
 * <p>The path stays {@code /api/rides/**} rather than moving under {@code /api/trips} because
 * the Android client already consumes it at this address; renaming would be a breaking change
 * for no functional gain. {@code rideId} is a trip id.
 */
@RestController
@RequestMapping("/api/rides")
public class RouteController {

    private final TripRouteService tripRouteService;

    public RouteController(TripRouteService tripRouteService) {
        this.tripRouteService = tripRouteService;
    }

    /**
     * Road-following route between the trip's departure and destination coordinates.
     *
     * <p>Until Phase 2 this returned a route between two hardcoded points in Gqeberha
     * (-33.9581, 25.6014 to -33.9615, 25.6089) regardless of which ride was requested, so every
     * ride rendered the same ~800m polyline. The lookup now goes through {@link TripRouteService},
     * which resolves the real coordinates; the controller stays free of repository access per the
     * project layering rule.
     *
     * <p>Returns 404 if the trip does not exist, and 400 if it has no usable coordinates —
     * previously both cases silently returned the placeholder route.
     */
    @GetMapping("/{rideId}/route")
    public ResponseEntity<RouteResponse> getRoute(@PathVariable Long rideId) {
        return ResponseEntity.ok(tripRouteService.getTripRoute(rideId));
    }
}
