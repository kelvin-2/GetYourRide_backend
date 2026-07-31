package com.example1.getyourride.controller;

import com.example1.getyourride.dto.response.TripLegRouteResponse;
import com.example1.getyourride.service.TripRouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Precomputed per-leg routes for a trip.
 *
 * <p>Separate from {@link TripController} for the same reason {@code TripStopController} is:
 * legs are a sub-resource of a trip with their own lifecycle, and {@code TripController} is
 * already large. Both classes map under {@code /api/trips}, which Spring allows because the
 * concrete paths do not overlap.
 *
 * <p>Both endpoints require authentication via the default {@code anyRequest().authenticated()}
 * rule in {@code SecurityConfig}; neither is added to the public matcher list.
 */
@RestController
@RequestMapping("/api/trips")
public class TripLegRouteController {

    private final TripRouteService tripRouteService;

    public TripLegRouteController(TripRouteService tripRouteService) {
        this.tripRouteService = tripRouteService;
    }

    /**
     * Calculates and stores one route per consecutive pair of the trip's stops.
     *
     * <p>Exposed as an explicit action rather than being folded into trip creation on purpose.
     * Precomputation makes one OpenRouteService call per leg, so wiring it into
     * {@code POST /api/trips} would mean an ORS outage or an exhausted quota stopped drivers
     * from posting rides at all. As a separate call it is idempotent and retryable, and a
     * failure leaves the trip itself intact.
     *
     * <p>Uses {@code POST} rather than the {@code PATCH /{id}/{action}} convention because this
     * creates {@code trip_leg_route} rows rather than transitioning the trip's state.
     *
     * @return the legs that were stored, in travel order
     */
    @PostMapping("/{tripId}/precompute-route")
    public ResponseEntity<List<TripLegRouteResponse>> precomputeRoute(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripRouteService.precomputeLegRoutes(tripId));
    }

    /**
     * Lists the trip's precomputed legs, in travel order. Empty if precomputation has not run.
     *
     * <p>Geometry is summarised rather than returned in full — see {@link TripLegRouteResponse}.
     */
    @GetMapping("/{tripId}/legs")
    public ResponseEntity<List<TripLegRouteResponse>> getLegRoutes(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripRouteService.getLegRoutes(tripId));
    }
}
