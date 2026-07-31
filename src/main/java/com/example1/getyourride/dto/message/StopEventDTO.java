package com.example1.getyourride.dto.message;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

/**
 * Stop lifecycle event, broadcast on {@code /topic/trip/{tripId}}.
 *
 * <p>Wire format is fixed by {@code GetYourRide_Tracking_Documentation.md} §4.4:
 * <pre>
 * { "type": "STOP_EVENT", "tripId": 42, "stopId": 7, "status": "ARRIVED" }
 * </pre>
 *
 * <p>Shares a destination with {@link LocationUpdateDTO}, so the {@code type} field is what tells
 * subscribers which shape they received. Per §4.5 the client uses this to update the stop list UI
 * (arrived badge) rather than to move the map marker.
 *
 * <p>Immutable, for the same reason as {@link LocationUpdateDTO}.
 */
@Getter
@JsonPropertyOrder({"type", "tripId", "stopId", "status"})
public class StopEventDTO implements TrackingMessage {

    private final TrackingMessageType type = TrackingMessageType.STOP_EVENT;

    private final Long tripId;

    /** {@code trip_stop.id} of the stop this event concerns, not its {@code stop_order}. */
    private final Long stopId;

    private final StopEventStatus status;

    public StopEventDTO(Long tripId, Long stopId, StopEventStatus status) {
        this.tripId = tripId;
        this.stopId = stopId;
        this.status = status;
    }
}
