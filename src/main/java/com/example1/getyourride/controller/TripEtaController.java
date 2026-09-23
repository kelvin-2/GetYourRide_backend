package com.example1.getyourride.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example1.getyourride.dto.response.EtaResponse;
import com.example1.getyourride.service.TripEtaService;

/**
 * Live ETA for a trip, calculated on demand via Google Compute Routes.
 *
 * <p>Separate from {@link TripController} for the same reason {@code TripLegRouteController}
 * is: a focused sub-resource with its own external dependency (Google, not the
 * OpenRouteService-backed route precomputation used to actually move the simulated vehicle),
 * kept out of the already-large TripController.
 *
 * <p>Requires authentication via the default {@code anyRequest().authenticated()} rule in
 * {@code SecurityConfig}; not added to the public matcher list.
 */
@RestController
@RequestMapping("/api/trips")
public class TripEtaController {

    private final TripEtaService tripEtaService;

    public TripEtaController(TripEtaService tripEtaService) {
        this.tripEtaService = tripEtaService;
    }

    /**
     * GET /api/trips/{tripId}/eta — current ETA to the trip's destination from wherever the
     * vehicle is right now (falls back to the departure point if tracking hasn't started yet).
     */
    @GetMapping("/{tripId}/eta")
    public ResponseEntity<EtaResponse> getEta(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripEtaService.getEta(tripId));
    }
}