package com.example1.getyourride.service.impl;

import com.example1.getyourride.config.WebSocketConfig;
import com.example1.getyourride.dto.message.LocationUpdateDTO;
import com.example1.getyourride.dto.message.StopEventDTO;
import com.example1.getyourride.dto.message.StopEventStatus;
import com.example1.getyourride.dto.message.TrackingMessage;
import com.example1.getyourride.service.TrackingBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes tracking messages through the STOMP simple broker.
 *
 * <p>{@link SimpMessagingTemplate} is contributed by {@code @EnableWebSocketMessageBroker} in
 * {@link WebSocketConfig}; Jackson converts the payload, so the DTOs' field names and
 * {@code @JsonPropertyOrder} determine the wire format directly.
 */
@Service
public class TrackingBroadcastServiceImpl implements TrackingBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(TrackingBroadcastServiceImpl.class);

    private final SimpMessagingTemplate messagingTemplate;

    public TrackingBroadcastServiceImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcastLocationUpdate(Long tripId, double lat, double lng, int legIndex) {
        publish(tripId, new LocationUpdateDTO(tripId, lat, lng, legIndex));
    }

    @Override
    public void broadcastStopEvent(Long tripId, Long stopId, StopEventStatus status) {
        publish(tripId, new StopEventDTO(tripId, stopId, status));
    }

    /**
     * Sends a message to the trip's topic.
     *
     * <p>Failures are logged and swallowed rather than propagated. The caller from Phase 4 onward is
     * a scheduled simulation tick: an exception escaping here would abort that tick, so one
     * unreachable subscriber or a transient broker problem could stall the simulation for every
     * trip. A dropped frame is recoverable — the next tick carries a fresh position, and
     * {@code trip_location_history} remains the durable record — so availability of the simulation
     * is worth more than delivery of any single message.
     */
    private void publish(Long tripId, TrackingMessage message) {
        String destination = WebSocketConfig.tripTopic(tripId);
        try {
            messagingTemplate.convertAndSend(destination, message);
            // Trace, not debug: at one message per trip every few seconds this is high volume, and
            // it carries live coordinates that should not sit in logs by default.
            log.trace("Published {} to {}", message.getType(), destination);
        } catch (RuntimeException ex) {
            log.warn("Failed to publish {} for trip {} to {}: {}",
                    message.getType(), tripId, destination, ex.getMessage());
        }
    }
}
