package com.example1.getyourride.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the STOMP broker wiring without starting a Spring context.
 *
 * <p>The prefixes and endpoint path are a public contract: the Android client connects to a fixed
 * URL and subscribes to a fixed prefix, so a change here breaks it with no compile error anywhere.
 * These tests also pin two deliberate omissions — no SockJS and no application destination prefix —
 * so that reinstating either is a conscious decision rather than a drive-by edit.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketConfigTest {

    private final WebSocketConfig config = new WebSocketConfig();

    @Mock
    private MessageBrokerRegistry brokerRegistry;

    @Mock
    private StompEndpointRegistry endpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;

    @Test
    @DisplayName("The simple broker serves the /topic prefix")
    void enablesSimpleBrokerOnTopic() {
        config.configureMessageBroker(brokerRegistry);

        verify(brokerRegistry).enableSimpleBroker("/topic");
    }

    @Test
    @DisplayName("No application destination prefix is configured, since tracking is push-only")
    void noApplicationDestinationPrefix() {
        config.configureMessageBroker(brokerRegistry);

        // There are no @MessageMapping handlers. Declaring a prefix would advertise an inbound
        // path that nothing serves.
        verify(brokerRegistry, never()).setApplicationDestinationPrefixes(anyString());
    }

    @Test
    @DisplayName("The STOMP endpoint is registered at /ws")
    void registersWsEndpoint() {
        when(endpointRegistry.addEndpoint(anyString())).thenReturn(endpointRegistration);

        config.registerStompEndpoints(endpointRegistry);

        verify(endpointRegistry).addEndpoint("/ws");
    }

    @Test
    @DisplayName("SockJS is not enabled, so /ws stays a plain WebSocket endpoint")
    void doesNotEnableSockJs() {
        // withSockJS() would move the real endpoint to /ws/websocket, breaking both the Android
        // client and a direct wscat test against /ws.
        when(endpointRegistry.addEndpoint(anyString())).thenReturn(endpointRegistration);

        config.registerStompEndpoints(endpointRegistry);

        verify(endpointRegistration, never()).withSockJS();
    }

    @Test
    @DisplayName("tripTopic builds the documented per-trip destination")
    void tripTopicMatchesDocumentedDestination() {
        assertEquals("/topic/trip/42", WebSocketConfig.tripTopic(42L));
        assertEquals("/topic/trip/1", WebSocketConfig.tripTopic(1L));
    }

    @Test
    @DisplayName("Published constants match the documented contract values")
    void constantsMatchContract() {
        assertEquals("/ws", WebSocketConfig.STOMP_ENDPOINT);
        assertEquals("/topic", WebSocketConfig.TOPIC_PREFIX);
    }
}
