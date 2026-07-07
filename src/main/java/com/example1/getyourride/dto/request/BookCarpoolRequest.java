package com.example1.getyourride.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookCarpoolRequest {
    @NotNull(message = "Pickup stop is required")
    private TripStopRequest pickupStop;

    private TripStopRequest dropOffStop;
}
