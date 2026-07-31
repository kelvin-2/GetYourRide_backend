package com.example1.getyourride.dto.request;

import com.example1.getyourride.validation.HasCoordinates;
import com.example1.getyourride.validation.ValidCoordinates;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for a single stop on a trip.
 *
 * <p>Used on four write paths, all of which persist a {@code trip_stop} row:
 * {@code POST /api/trips} (nested in {@link CreateTripRequest#getStops()}),
 * {@code POST /api/trips/{tripId}/book} (nested in {@link BookCarpoolRequest}),
 * {@code POST /api/trips/{tripId}/stops} and {@code POST /api/trips/{tripId}/stops/student}.
 * The {@link ValidCoordinates} constraint lives here, on the shared DTO, so the
 * 0,0-coordinate rule is enforced identically on all four rather than being repeated
 * in each service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidCoordinates
public class TripStopRequest implements HasCoordinates {

    @NotBlank(message = "Stop name is required")
    private String stopName;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private Integer stopOrder;
}
