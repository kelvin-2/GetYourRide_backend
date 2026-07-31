package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.message.LocationUpdateDTO;
import com.example1.getyourride.dto.message.StopEventDTO;
import com.example1.getyourride.dto.message.StopEventStatus;
import com.example1.getyourride.dto.message.TrackingMessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the tracking broadcast seam.
 *
 * <p>{@link SimpMessagingTemplate} is mocked, so no broker or WebSocket connection is needed. What
 * matters here is the destination a message lands on and the payload it carries — get the
 * destination wrong and subscribers silently receive nothing.
 */
@ExtendWith(MockitoExtension.class)
class TrackingBroadcastServiceImplTest {

    private static final Long TRIP_ID = 42L;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TrackingBroadcastServiceImpl trackingBroadcastService;

    @Test
    @DisplayName("Location updates go to /topic/trip/{tripId}")
    void locationUpdateUsesTripTopic() {
        trackingBroadcastService.broadcastLocationUpdate(TRIP_ID, -33.96, 25.61, 1);

        // The destination is the contract the Android client subscribes against. A typo here means
        // messages are published into the void with no error anywhere.
        verify(messagingTemplate).convertAndSend(eq("/topic/trip/42"), any(LocationUpdateDTO.class));
    }

    @Test
    @DisplayName("Location update payload carries the supplied position and leg index")
    void locationUpdateCarriesPosition() {
        trackingBroadcastService.broadcastLocationUpdate(TRIP_ID, -33.96, 25.61, 3);

        ArgumentCaptor<LocationUpdateDTO> captor = ArgumentCaptor.forClass(LocationUpdateDTO.class);
        verify(messagingTemplate).convertAndSend(anyString(), captor.capture());

        LocationUpdateDTO sent = captor.getValue();
        assertEquals(TRIP_ID, sent.getTripId());
        assertEquals(-33.96, sent.getLat());
        assertEquals(25.61, sent.getLng());
        assertEquals(3, sent.getLegIndex());
        assertEquals(TrackingMessageType.LOCATION_UPDATE, sent.getType());
    }

    @Test
    @DisplayName("Stop events go to the same trip topic as location updates")
    void stopEventUsesSameTripTopic() {
        // Both shapes share one destination by design (§4.4); the type field is how subscribers
        // tell them apart.
        trackingBroadcastService.broadcastStopEvent(TRIP_ID, 7L, StopEventStatus.ARRIVED);

        verify(messagingTemplate).convertAndSend(eq("/topic/trip/42"), any(StopEventDTO.class));
    }

    @Test
    @DisplayName("Stop event payload carries the stop id and status")
    void stopEventCarriesStopDetails() {
        trackingBroadcastService.broadcastStopEvent(TRIP_ID, 7L, StopEventStatus.ARRIVED);

        ArgumentCaptor<StopEventDTO> captor = ArgumentCaptor.forClass(StopEventDTO.class);
        verify(messagingTemplate).convertAndSend(anyString(), captor.capture());

        StopEventDTO sent = captor.getValue();
        assertEquals(TRIP_ID, sent.getTripId());
        assertEquals(7L, sent.getStopId());
        assertEquals(StopEventStatus.ARRIVED, sent.getStatus());
        assertEquals(TrackingMessageType.STOP_EVENT, sent.getType());
    }

    @Test
    @DisplayName("Each trip gets its own topic, so trips cannot leak into each other's feeds")
    void topicsAreScopedPerTrip() {
        trackingBroadcastService.broadcastLocationUpdate(1L, -33.96, 25.61, 0);
        trackingBroadcastService.broadcastLocationUpdate(999L, -33.97, 25.62, 0);

        verify(messagingTemplate).convertAndSend(eq("/topic/trip/1"), any(LocationUpdateDTO.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/trip/999"), any(LocationUpdateDTO.class));
    }

    @Test
    @DisplayName("A broker failure is swallowed so it cannot abort a simulation tick")
    void brokerFailureDoesNotPropagate() {
        // From Phase 4 the caller is a @Scheduled tick shared by every active trip. If a publish
        // failure escaped, one bad subscriber could stall the simulation for all of them.
        doThrow(new MessagingException("broker down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertDoesNotThrow(() -> trackingBroadcastService.broadcastLocationUpdate(TRIP_ID, -33.96, 25.61, 1));
        assertDoesNotThrow(() -> trackingBroadcastService.broadcastStopEvent(TRIP_ID, 7L, StopEventStatus.ARRIVED));
    }
}
