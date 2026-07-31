package com.example1.getyourride.validation;

/**
 * Contract for any request DTO that carries a single geographic point.
 *
 * <p>This exists so {@link CoordinatesValidator} can validate coordinates without
 * importing anything from the {@code dto} package. Keeping the dependency pointing
 * one way ({@code dto} -> {@code validation}) avoids a package cycle and lets future
 * coordinate-bearing DTOs (for example driver location pings in the live tracking
 * phases) reuse {@link ValidCoordinates} by simply implementing this interface.
 *
 * <p>Implementors that use Lombok's {@code @Data} or {@code @Getter} already satisfy
 * this contract for {@code Double latitude} / {@code Double longitude} fields, so
 * implementing it costs no extra code.
 */
public interface HasCoordinates {

    /**
     * @return latitude in decimal degrees, or {@code null} if the client omitted it
     */
    Double getLatitude();

    /**
     * @return longitude in decimal degrees, or {@code null} if the client omitted it
     */
    Double getLongitude();
}
