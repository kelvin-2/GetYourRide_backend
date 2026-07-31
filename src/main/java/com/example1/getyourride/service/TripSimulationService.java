package com.example1.getyourride.service;

import java.util.List;

/**
 * Drives simulated vehicle movement along a trip's precomputed leg polylines.
 *
 * <p>There is no real GPS feed from drivers, so movement is simulated: each tick advances a trip a
 * few points further along its current leg, records the position, and broadcasts it. The leg geometry
 * comes from {@code trip_leg_route}, populated by {@link TripRouteService#precomputeLegRoutes(Long)}.
 *
 * <p>The scheduled trigger deliberately lives in a separate component
 * ({@code scheduler.TripSimulationScheduler}) rather than on this interface — see that class for why.
 */
public interface TripSimulationService {

    /**
     * Ids of trips currently eligible for simulation, i.e. status {@code IN_PROGRESS}.
     *
     * <p>Returns ids rather than entities so the scheduler can advance each trip in its own
     * transaction without holding detached state.
     */
    List<Long> findActiveTripIds();

    /**
     * Advances one trip by a single tick.
     *
     * <p>Reads the trip's resume point ({@code current_leg_index}, {@code current_point_index}), moves
     * it forward, and writes the new state back. Handles arrival at a stop, the dwell pause, and trip
     * completion. Safe to call for a trip that is not {@code IN_PROGRESS} or has no precomputed legs —
     * it returns without doing anything.
     *
     * @param tripId trip to advance
     */
    void advanceTrip(Long tripId);

    /**
     * Initialises tracking state so a trip starts from its first stop.
     *
     * <p>Called when a trip transitions to {@code IN_PROGRESS}. Without this a restarted trip would
     * resume from whatever indices it held previously, making the vehicle appear to teleport into the
     * middle of its route.
     *
     * @param tripId trip being started
     */
    void startTracking(Long tripId);
}
