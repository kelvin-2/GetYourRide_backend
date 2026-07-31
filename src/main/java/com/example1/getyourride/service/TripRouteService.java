package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.RouteResponse;
import com.example1.getyourride.dto.response.TripLegRouteResponse;

import java.util.List;

/**
 * Trip-aware routing operations.
 *
 * <p>Sits between the controllers and {@link RouteService}: {@code RouteService} knows how to
 * talk to OpenRouteService but nothing about trips, while this service resolves a trip id into
 * real coordinates and owns the {@code trip_leg_route} precomputation. Controllers depend on
 * this rather than reaching for {@code TripRepository} directly, per the layering rule in
 * {@code doc/project-rules.md}.
 */
public interface TripRouteService {

    /**
     * Road-following route for a trip, from its departure coordinates to its destination
     * coordinates.
     *
     * @param tripId the trip to route
     * @return the ORS polyline plus distance and duration
     * @throws com.example1.getyourride.exception.ResourceNotFoundException if the trip does not exist
     * @throws com.example1.getyourride.exception.BadRequestException      if the trip has no usable
     *                                                                    departure/destination coordinates
     */
    RouteResponse getTripRoute(Long tripId);

    /**
     * Calculates and stores one route per consecutive pair of the trip's stops.
     *
     * <p>Idempotent: any existing legs for the trip are replaced, so this can be re-run safely
     * after stops change.
     *
     * @param tripId the trip whose stops define the legs
     * @return a summary of the legs that were stored, in travel order
     * @throws com.example1.getyourride.exception.ResourceNotFoundException if the trip does not exist
     * @throws com.example1.getyourride.exception.BadRequestException      if the trip has fewer than two
     *                                                                    stops, or a stop has unusable
     *                                                                    coordinates
     */
    List<TripLegRouteResponse> precomputeLegRoutes(Long tripId);

    /**
     * Previously precomputed legs for a trip, in travel order. Empty if precomputation has not
     * been run.
     */
    List<TripLegRouteResponse> getLegRoutes(Long tripId);
}
