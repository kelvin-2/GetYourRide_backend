package com.example1.getyourride.service;

import com.example1.getyourride.dto.message.StopEventStatus;

/**
 * Publishes live tracking messages to a trip's subscribers.
 *
 * <p>The single seam between whatever produces tracking events and the STOMP transport. Phase 4's
 * simulation engine calls this rather than holding a {@code SimpMessagingTemplate} itself, so the
 * destination format and message construction stay in one place and the simulator stays testable
 * without a broker.
 */
public interface TrackingBroadcastService {

    /**
     * Broadcasts the vehicle's current position for a trip.
     *
     * @param tripId   trip being tracked
     * @param lat      current latitude
     * @param lng      current longitude
     * @param legIndex zero-based index of the leg currently being travelled
     */
    void broadcastLocationUpdate(Long tripId, double lat, double lng, int legIndex);

    /**
     * Broadcasts a stop lifecycle event for a trip.
     *
     * @param tripId trip being tracked
     * @param stopId {@code trip_stop.id} of the stop concerned
     * @param status what happened at the stop
     */
    void broadcastStopEvent(Long tripId, Long stopId, StopEventStatus status);
}
