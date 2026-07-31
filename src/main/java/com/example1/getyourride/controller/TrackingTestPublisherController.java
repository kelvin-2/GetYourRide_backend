package com.example1.getyourride.controller;

import com.example1.getyourride.dto.message.StopEventStatus;
import com.example1.getyourride.service.TrackingBroadcastService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Development-only endpoints for publishing tracking messages by hand.
 *
 * <p><b>Why this exists:</b> Phase 3 delivers the transport before Phase 4 delivers the simulator
 * that drives it. Without a manual publisher there is no way to confirm that a subscriber actually
 * receives a frame on {@code /topic/trip/{tripId}} in the documented shape, which is the phase's
 * acceptance criterion.
 *
 * <p><b>Why it is disabled by default:</b> these endpoints broadcast arbitrary positions to whoever
 * is watching a trip. Left enabled in a deployed environment, any authenticated caller could feed
 * riders a fabricated vehicle location. Enabling it is therefore an explicit, local opt-in:
 *
 * <pre>
 * getyourride.tracking.test-publisher.enabled=true
 * </pre>
 *
 * <p>With the flag absent or false the bean is not registered at all, so the routes return 404 —
 * the safe default rather than a live spoofing surface. The commented line in
 * {@code application.properties} documents the toggle next to the config it belongs with.
 *
 * <p>No trip-existence check is performed on purpose: this tests the transport in isolation, and
 * requiring a real trip row would couple a transport smoke test to database state.
 *
 * <p>Delete this class once the Phase 4 simulator is verified end to end.
 */
@RestController
@RequestMapping("/api/trips/{tripId}/tracking")
@ConditionalOnProperty(name = "getyourride.tracking.test-publisher.enabled", havingValue = "true")
public class TrackingTestPublisherController {

    private final TrackingBroadcastService trackingBroadcastService;

    public TrackingTestPublisherController(TrackingBroadcastService trackingBroadcastService) {
        this.trackingBroadcastService = trackingBroadcastService;
    }

    /**
     * Publishes a single {@code LOCATION_UPDATE} to the trip's topic.
     *
     * <p>Example: {@code POST /api/trips/42/tracking/test-location?lat=-33.96&lng=25.61&legIndex=1}
     */
    @PostMapping("/test-location")
    public ResponseEntity<Map<String, Object>> publishTestLocation(
            @PathVariable Long tripId,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "0") int legIndex) {

        trackingBroadcastService.broadcastLocationUpdate(tripId, lat, lng, legIndex);

        return ResponseEntity.ok(Map.of(
                "published", "LOCATION_UPDATE",
                "destination", "/topic/trip/" + tripId));
    }

    /**
     * Publishes a single {@code STOP_EVENT} to the trip's topic.
     *
     * <p>Example: {@code POST /api/trips/42/tracking/test-stop-event?stopId=7&status=ARRIVED}
     */
    @PostMapping("/test-stop-event")
    public ResponseEntity<Map<String, Object>> publishTestStopEvent(
            @PathVariable Long tripId,
            @RequestParam Long stopId,
            @RequestParam(defaultValue = "ARRIVED") StopEventStatus status) {

        trackingBroadcastService.broadcastStopEvent(tripId, stopId, status);

        return ResponseEntity.ok(Map.of(
                "published", "STOP_EVENT",
                "destination", "/topic/trip/" + tripId));
    }
}
