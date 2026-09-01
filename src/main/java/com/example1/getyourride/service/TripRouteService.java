package com.example1.getyourride.service;

import java.util.List;

import com.example1.getyourride.dto.response.RouteResponse;
import com.example1.getyourride.dto.response.TripLegRouteResponse;

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
     * Calculates and stores one route per consecutive pair of the trip's waypoints, where the
     * waypoints are the trip's departure, then each stop in {@code stop_order}, then the trip's
     * destination.
     *
     * <p>The endpoints are bracketed onto the stop list rather than the legs being built from
     * {@code trip_stop} pairs alone, because the stops do not reliably begin at the departure or
     * end at the destination — a stop-only leg set leaves the simulated vehicle short of where
     * the trip says it is going. An endpoint with missing or {@code 0,0} coordinates is skipped
     * instead of failing, so trips predating coordinate capture still route between their stops.
     *
     * <p>Consecutive waypoints at the same location are merged: routing a zero-length leg yields
     * no duration, which the simulator cannot pace, and the vehicle would stall.
     *
     * <p>Legs are addressed by {@code stop_order}, with {@code 0} reserved for the departure and
     * {@code maxStopOrder + 1} for the destination. Neither corresponds to a {@code trip_stop}
     * row, so a leg can legitimately end at an order that has no stop.
     *
     * <p>Idempotent: any existing legs for the trip are replaced, so this can be re-run safely
     * after stops change.
     *
     * @param tripId the trip whose departure, stops and destination define the legs
     * @return a summary of the legs that were stored, in travel order
     * @throws com.example1.getyourride.exception.ResourceNotFoundException if the trip does not exist
     * @throws com.example1.getyourride.exception.BadRequestException      if fewer than two distinct
     *                                                                    routable waypoints remain, or a
     *                                                                    stop has unusable coordinates
     */
    List<TripLegRouteResponse> precomputeLegRoutes(Long tripId);

    /**
     * Previously precomputed legs for a trip, in travel order. Empty if precomputation has not
     * been run.
     */
    List<TripLegRouteResponse> getLegRoutes(Long tripId);

    /**
     * Guarantees the trip has legs, computing them only if it does not already.
     *
     * <p>Exists so starting a trip is a single call that cannot leave a vehicle stationary for
     * want of a route, without paying for precomputation every time. Precomputing makes one
     * OpenRouteService call per leg, so re-running it on a trip whose stops have not changed
     * spends quota to arrive at the same answer.
     *
     * @param tripId the trip to check
     * @param force  recompute even when legs already exist. Needed after the stops change, since
     *               existing legs then describe a route the trip no longer takes.
     * @return the trip's legs in travel order, whether freshly computed or already present
     * @throws com.example1.getyourride.exception.ResourceNotFoundException if the trip does not exist
     * @throws com.example1.getyourride.exception.BadRequestException      if legs are needed but cannot
     *                                                                    be built
     */
    List<TripLegRouteResponse> ensureLegRoutes(Long tripId, boolean force);
}
