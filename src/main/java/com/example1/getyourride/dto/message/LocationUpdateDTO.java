package com.example1.getyourride.dto.message;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

/**
 * Per-tick vehicle position, broadcast on {@code /topic/trip/{tripId}}.
 *
 * <p>Wire format is fixed by {@code GetYourRide_Tracking_Documentation.md} §4.4:
 * <pre>
 * { "type": "LOCATION_UPDATE", "tripId": 42, "lat": -33.96, "lng": 25.61, "legIndex": 1 }
 * </pre>
 *
 * <p>The abbreviated {@code lat}/{@code lng} names are intentional — they match the documented
 * contract that the Android client already expects. Renaming them to {@code latitude}/{@code
 * longitude} for consistency with {@code TripStopRequest} would silently break the client, so the
 * contract wins over internal naming consistency here.
 *
 * <p>Immutable: these are outbound messages, so there are no setters. {@code @JsonPropertyOrder}
 * pins the field order to the documented example, which matters for anyone eyeballing frames in
 * wscat or Postman.
 *
 * <p>Named {@code ...DTO} rather than {@code ...Response} because it is neither a request nor a
 * response — it is a server-pushed message. The name is the one used in the Phase 3 deliverables and
 * the tracking documentation.
 */
@Getter
@JsonPropertyOrder({"type", "tripId", "lat", "lng", "legIndex"})
public class LocationUpdateDTO implements TrackingMessage {

    private final TrackingMessageType type = TrackingMessageType.LOCATION_UPDATE;

    private final Long tripId;
    private final double lat;
    private final double lng;

    /**
     * Zero-based index of the leg the vehicle is currently travelling, matching
     * {@code trip.current_leg_index} and the ordering of {@code trip_leg_route} rows. Lets the
     * client tell "still on leg 1" from "moved on to leg 2" without recomputing anything.
     */
    private final int legIndex;

    public LocationUpdateDTO(Long tripId, double lat, double lng, int legIndex) {
        this.tripId = tripId;
        this.lat = lat;
        this.lng = lng;
        this.legIndex = legIndex;
    }
}
