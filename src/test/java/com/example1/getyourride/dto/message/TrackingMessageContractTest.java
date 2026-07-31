package com.example1.getyourride.dto.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the tracking message wire format to the contract documented in
 * {@code GetYourRide_Tracking_Documentation.md} §4.4.
 *
 * <p>This is the automated half of the Phase 3 acceptance criterion "message shapes match the
 * documented JSON". The Android client parses these frames by field name, so a rename, a type change
 * or a stray extra field is a breaking change that would only show up as a silent parse failure on
 * the device. Asserting on the exact serialised string is blunt on purpose: it fails loudly the
 * moment the shape drifts.
 *
 * <p>Uses a plain {@link ObjectMapper}, which is what {@code SimpMessagingTemplate} converts
 * payloads with, so this reflects what actually goes on the wire.
 */
class TrackingMessageContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("LOCATION_UPDATE serialises exactly as documented in §4.4")
    void locationUpdateMatchesContract() throws Exception {
        LocationUpdateDTO message = new LocationUpdateDTO(42L, -33.96, 25.61, 1);

        String json = objectMapper.writeValueAsString(message);

        assertEquals(
                "{\"type\":\"LOCATION_UPDATE\",\"tripId\":42,\"lat\":-33.96,\"lng\":25.61,\"legIndex\":1}",
                json);
    }

    @Test
    @DisplayName("STOP_EVENT serialises exactly as documented in §4.4")
    void stopEventMatchesContract() throws Exception {
        StopEventDTO message = new StopEventDTO(42L, 7L, StopEventStatus.ARRIVED);

        String json = objectMapper.writeValueAsString(message);

        assertEquals("{\"type\":\"STOP_EVENT\",\"tripId\":42,\"stopId\":7,\"status\":\"ARRIVED\"}", json);
    }

    @Test
    @DisplayName("The type discriminator is present on both shapes so subscribers can switch on it")
    void bothShapesCarryTheirType() {
        assertEquals(TrackingMessageType.LOCATION_UPDATE,
                new LocationUpdateDTO(1L, 0.1, 0.1, 0).getType());
        assertEquals(TrackingMessageType.STOP_EVENT,
                new StopEventDTO(1L, 1L, StopEventStatus.ARRIVED).getType());
    }

    @Test
    @DisplayName("Enum constants serialise to the exact strings the client expects")
    void enumsSerialiseToDocumentedStrings() throws Exception {
        // Guards against someone adding @JsonProperty or a custom serialiser that changes casing.
        assertEquals("\"LOCATION_UPDATE\"", objectMapper.writeValueAsString(TrackingMessageType.LOCATION_UPDATE));
        assertEquals("\"STOP_EVENT\"", objectMapper.writeValueAsString(TrackingMessageType.STOP_EVENT));
        assertEquals("\"ARRIVED\"", objectMapper.writeValueAsString(StopEventStatus.ARRIVED));
    }

    @Test
    @DisplayName("Coordinates keep the abbreviated lat/lng names, not latitude/longitude")
    void coordinateFieldNamesAreAbbreviated() throws Exception {
        // The rest of the codebase uses latitude/longitude. The wire contract does not, and the
        // contract wins — "fixing" the inconsistency here would break the client silently.
        String json = objectMapper.writeValueAsString(new LocationUpdateDTO(42L, -33.96, 25.61, 1));

        assertTrue(json.contains("\"lat\":"), json);
        assertTrue(json.contains("\"lng\":"), json);
        assertTrue(!json.contains("latitude") && !json.contains("longitude"), json);
    }

    @Test
    @DisplayName("Messages expose no setters, so a broadcast payload cannot be mutated in flight")
    void messagesAreImmutable() {
        boolean hasSetter = java.util.Arrays.stream(LocationUpdateDTO.class.getMethods())
                .anyMatch(method -> method.getName().startsWith("set"));

        assertTrue(!hasSetter, "LocationUpdateDTO must stay immutable");
    }
}
