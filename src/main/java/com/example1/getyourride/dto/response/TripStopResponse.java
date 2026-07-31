package com.example1.getyourride.dto.response;

import com.example1.getyourride.entity.TripStopStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripStopResponse {
    private Long id;
    private String stopName;
    private Double latitude;
    private Double longitude;
    private Integer stopOrder;
    private Long studentId;
    private String studentName;

    /**
     * PENDING or ARRIVED.
     *
     * <p>Exposed so a client opening the tracking screen mid-trip can draw which stops have already
     * been visited. Live changes arrive as {@code STOP_EVENT} messages, but those only cover arrivals
     * that happen while subscribed — without this field a reconnecting client would show every stop as
     * outstanding. See tracking documentation §4.5.
     */
    private TripStopStatus status;
}
