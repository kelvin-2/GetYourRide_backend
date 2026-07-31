package com.example1.getyourride.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket transport for live trip tracking.
 *
 * <p>Before this class the application had no real-time transport at all; trip position could only
 * be polled over REST. The simulation engine in Phase 4 pushes a position update every few seconds
 * per active trip, which polling handles badly.
 *
 * <h2>Why the simple in-memory broker</h2>
 * {@code enableSimpleBroker} keeps subscriptions in the application's own memory. That is the right
 * fit while this runs as a single instance: no external broker to operate, and messages are
 * transient anyway — a subscriber that misses a tick gets a fresh position two seconds later, and
 * {@code trip_location_history} is the durable record. The trade-off is that it does not work across
 * multiple instances, because each would only reach the clients connected to itself. Scaling out
 * means switching to a relay (RabbitMQ/ActiveMQ) via {@code enableStompBrokerRelay}, which is a
 * config change here rather than a rewrite of the publishing code.
 *
 * <h2>Security</h2>
 * The {@code /ws} handshake is a normal HTTP GET, so it passes through the Spring Security filter
 * chain and is covered by the existing {@code anyRequest().authenticated()} rule in
 * {@link SecurityConfig}. It is deliberately <em>not</em> added to the public matcher list: a client
 * must present {@code Authorization: Bearer <token>} on the handshake, which {@code JwtAuthFilter}
 * reads exactly as it does for any REST call.
 *
 * <p><b>Known gap:</b> authentication gates the connection, not the subscription. Any authenticated
 * user can subscribe to {@code /topic/trip/{tripId}} for a trip they have nothing to do with and
 * watch its live position. Closing that needs a per-destination authorisation check on inbound
 * SUBSCRIBE frames, plus a decision on who is entitled to watch a trip. Recorded against Phase 5 in
 * {@code doc/Task} rather than guessed at here.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** Path clients open the STOMP WebSocket connection against. */
    public static final String STOMP_ENDPOINT = "/ws";

    /** Prefix the broker serves subscriptions under. */
    public static final String TOPIC_PREFIX = "/topic";

    /**
     * Destination template for a single trip's tracking feed. Both {@code LOCATION_UPDATE} and
     * {@code STOP_EVENT} messages go here, per §4.4.
     */
    public static final String TRIP_TOPIC_TEMPLATE = TOPIC_PREFIX + "/trip/%d";

    /** Builds the tracking destination for a trip, so the format is defined in exactly one place. */
    public static String tripTopic(Long tripId) {
        return String.format(TRIP_TOPIC_TEMPLATE, tripId);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(TOPIC_PREFIX);

        // No setApplicationDestinationPrefixes call on purpose. That prefix routes client-sent
        // messages to @MessageMapping handlers, and tracking is push-only — the server broadcasts,
        // clients only subscribe. Adding a prefix for handlers that do not exist would suggest an
        // inbound path that is not supported. Configure it here if client-to-server messaging is
        // ever added.
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Plain WebSocket, no withSockJS(). SockJS exists to give browsers a fallback when
        // WebSocket is unavailable, and it moves the real endpoint to /ws/websocket — which would
        // break both the Android client and a direct wscat test against /ws. The consumer here is
        // an Android app using raw WebSocket, so the fallback machinery is cost without benefit.
        // If a browser client is added later, register a second SockJS-enabled endpoint rather than
        // converting this one.
        registry.addEndpoint(STOMP_ENDPOINT);
    }
}
