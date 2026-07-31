package com.example1.getyourride.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary of one precomputed trip leg.
 *
 * <p>Deliberately excludes the full polyline. A leg's geometry is typically hundreds of
 * points, and a trip has several legs, so returning all of it would make the precompute and
 * leg-listing responses very large for no benefit — the client renders routes through
 * {@code GET /api/rides/{rideId}/route}, and the simulator reads geometry from the database
 * directly.
 *
 * <p>{@code startPoint} and {@code endPoint} are included because they make a leg verifiable
 * at a glance: together with {@code distanceMeters} they confirm the leg is anchored where
 * its stops are, which is exactly what the Phase 2 acceptance criteria ask for.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripLegRouteResponse {

    private Long id;

    /** Zero-based position of this leg in the trip, matching {@code trip.current_leg_index}. */
    private Integer legIndex;

    private Integer fromStopOrder;
    private Integer toStopOrder;

    private String fromStopName;
    private String toStopName;

    private Double distanceMeters;
    private Double durationSeconds;

    /** Number of points in the stored polyline. A value of 2 suggests a straight line. */
    private Integer pointCount;

    /** First point of the polyline as {@code [latitude, longitude]}. */
    private double[] startPoint;

    /** Last point of the polyline as {@code [latitude, longitude]}. */
    private double[] endPoint;
}
