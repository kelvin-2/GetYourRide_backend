package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.message.StopEventStatus;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.entity.TripLegRoute;
import com.example1.getyourride.entity.TripLocationHistory;
import com.example1.getyourride.entity.TripStop;
import com.example1.getyourride.entity.TripStopStatus;
import com.example1.getyourride.repository.TripLegRouteRepository;
import com.example1.getyourride.repository.TripLocationHistoryRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.repository.TripStopRepository;
import com.example1.getyourride.service.TrackingBroadcastService;
import com.example1.getyourride.service.TripSimulationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Default {@link TripSimulationService}.
 *
 * <h2>How movement works</h2>
 * A trip's route is stored as one polyline per leg. The vehicle's position is a cursor into that data:
 * {@code current_leg_index} picks the leg, {@code current_point_index} picks the point within it. A
 * tick moves the cursor forward by a computed step, writes the new coordinates to the trip, appends a
 * {@code trip_location_history} row and broadcasts a {@code LOCATION_UPDATE}.
 *
 * <p>Storing the cursor in the database rather than in memory is what makes the simulation survive a
 * restart, and it is also what keeps trips independent of one another — each trip's progress lives in
 * its own row, so two trips can never share or corrupt each other's position.
 *
 * <h2>Reaching a stop</h2>
 * When a step would run past the end of the current leg, the vehicle is snapped to the leg's final
 * point instead of overshooting. That point is a stop, so the stop is marked {@code ARRIVED}, a
 * {@code STOP_EVENT} goes out, and {@code dwell_until} is set to simulate boarding. Subsequent ticks
 * do nothing until the dwell expires, at which point the cursor moves to the next leg.
 *
 * <p>Completion happens only when the vehicle reaches the end of the <em>last</em> leg — not when the
 * leg index runs out mid-route — so a trip cannot report {@code COMPLETED} early.
 */
@Service
public class TripSimulationServiceImpl implements TripSimulationService {

    private static final Logger log = LoggerFactory.getLogger(TripSimulationServiceImpl.class);

    /** Only trips in this status are simulated. */
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final TripLegRouteRepository tripLegRouteRepository;
    private final TripLocationHistoryRepository locationHistoryRepository;
    private final TrackingBroadcastService broadcastService;
    private final ObjectMapper objectMapper;

    private final long tickIntervalMs;
    private final double speedMultiplier;
    private final long dwellSeconds;
    private final int fallbackStepSize;

    public TripSimulationServiceImpl(
            TripRepository tripRepository,
            TripStopRepository tripStopRepository,
            TripLegRouteRepository tripLegRouteRepository,
            TripLocationHistoryRepository locationHistoryRepository,
            TrackingBroadcastService broadcastService,
            ObjectMapper objectMapper,
            @Value("${getyourride.tracking.simulation.tick-interval-ms:4000}") long tickIntervalMs,
            @Value("${getyourride.tracking.simulation.speed-multiplier:10.0}") double speedMultiplier,
            @Value("${getyourride.tracking.simulation.dwell-seconds:20}") long dwellSeconds,
            @Value("${getyourride.tracking.simulation.fallback-step-size:5}") int fallbackStepSize) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.tripLegRouteRepository = tripLegRouteRepository;
        this.locationHistoryRepository = locationHistoryRepository;
        this.broadcastService = broadcastService;
        this.objectMapper = objectMapper;
        this.tickIntervalMs = tickIntervalMs;
        this.speedMultiplier = speedMultiplier;
        this.dwellSeconds = dwellSeconds;
        this.fallbackStepSize = fallbackStepSize;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findActiveTripIds() {
        return tripRepository.findByStatus(STATUS_IN_PROGRESS).stream()
                .map(Trip::getTripId)
                .toList();
    }

    @Override
    @Transactional
    public void startTracking(Long tripId) {
        Optional<Trip> found = tripRepository.findById(tripId);
        if (found.isEmpty()) {
            return;
        }
        Trip trip = found.get();

        List<TripStop> stops = tripStopRepository.findByTripTripIdOrderByStopOrderAsc(tripId);

        // Reset the cursor to the start of the route. Without this, restarting a previously-run trip
        // would resume from stale indices and the vehicle would appear mid-route.
        trip.setCurrentLegIndex(0);
        trip.setCurrentPointIndex(0);
        trip.setDwellUntil(null);

        // Seed the position at the first stop so a client that loads the screen before the first tick
        // has somewhere to draw the marker, rather than a null island or a blank map.
        if (!stops.isEmpty()) {
            TripStop first = stops.get(0);
            trip.setCurrentLat(first.getLatitude());
            trip.setCurrentLng(first.getLongitude());
        }

        // Clear arrivals from any previous run of this trip, otherwise the stop list would render as
        // already-visited the moment tracking restarts.
        stops.forEach(stop -> stop.setStatus(TripStopStatus.PENDING));
        tripStopRepository.saveAll(stops);

        tripRepository.save(trip);

        long legCount = tripLegRouteRepository.countByTripTripId(tripId);
        if (legCount == 0) {
            // Not an exception: the trip is legitimately started, it just cannot move yet. Surfacing
            // it as a warning keeps the diagnosis obvious instead of leaving a silently stationary
            // vehicle.
            log.warn("Trip {} started but has no precomputed legs. Call POST /api/trips/{}/precompute-route "
                    + "or it will not move.", tripId, tripId);
        } else {
            log.info("Tracking started for trip {} with {} leg(s)", tripId, legCount);
        }
    }

    @Override
    @Transactional
    public void advanceTrip(Long tripId) {
        Optional<Trip> found = tripRepository.findById(tripId);
        if (found.isEmpty()) {
            return;
        }
        Trip trip = found.get();

        // Re-checked even though the scheduler filtered on it: the status may have changed between
        // the scheduler's query and this transaction.
        if (!STATUS_IN_PROGRESS.equalsIgnoreCase(trip.getStatus())) {
            return;
        }

        if (isDwelling(trip)) {
            return;
        }

        List<TripLegRoute> legs = tripLegRouteRepository.findByTripTripIdOrderByFromStopOrderAsc(tripId);
        if (legs.isEmpty()) {
            log.warn("Trip {} is IN_PROGRESS but has no precomputed legs; skipping tick", tripId);
            return;
        }

        int legIndex = orZero(trip.getCurrentLegIndex());

        // Defensive: a leg index past the end means the route was shortened underneath a running trip
        // (stops edited, precompute re-run). Completing is the only sane resolution — the alternative
        // is a trip stuck IN_PROGRESS forever.
        if (legIndex >= legs.size()) {
            log.warn("Trip {} has leg index {} but only {} leg(s); completing", tripId, legIndex, legs.size());
            complete(trip);
            tripRepository.save(trip);
            return;
        }

        List<double[]> points = parseGeometry(legs.get(legIndex));
        if (points.size() < 2) {
            log.warn("Trip {} leg {} has {} point(s); advancing past it", tripId, legIndex, points.size());
            moveToNextLegOrComplete(trip, legs, legIndex);
            tripRepository.save(trip);
            return;
        }

        int pointIndex = orZero(trip.getCurrentPointIndex());
        int lastIndex = points.size() - 1;
        int nextIndex = pointIndex + stepSizeFor(legs.get(legIndex), points.size());

        if (nextIndex >= lastIndex) {
            // Snap to the leg's final point rather than overshooting past the stop.
            arriveAtEndOfLeg(trip, legs, legIndex, points.get(lastIndex));
        } else {
            double[] point = points.get(nextIndex);
            trip.setCurrentPointIndex(nextIndex);
            publishPosition(trip, point, legIndex);
        }

        tripRepository.save(trip);
    }

    /**
     * True while the vehicle is parked at a stop. Clears the marker once it expires so the next tick
     * moves, keeping the "am I paused" check to a single nullable column.
     */
    private boolean isDwelling(Trip trip) {
        LocalDateTime dwellUntil = trip.getDwellUntil();
        if (dwellUntil == null) {
            return false;
        }
        if (LocalDateTime.now().isBefore(dwellUntil)) {
            log.trace("Trip {} dwelling until {}", trip.getTripId(), dwellUntil);
            return true;
        }
        trip.setDwellUntil(null);
        return false;
    }

    /** Handles reaching the stop that terminates the current leg. */
    private void arriveAtEndOfLeg(Trip trip, List<TripLegRoute> legs, int legIndex, double[] finalPoint) {
        publishPosition(trip, finalPoint, legIndex);

        TripLegRoute leg = legs.get(legIndex);
        markStopArrived(trip, leg.getToStopOrder());
        moveToNextLegOrComplete(trip, legs, legIndex);
    }

    /**
     * Flips the arrived stop to {@code ARRIVED} and broadcasts a {@code STOP_EVENT}.
     *
     * <p>Looked up by {@code stop_order} because that is what a leg records; the broadcast carries
     * {@code trip_stop.id}, which is what the client's stop list is keyed on.
     */
    private void markStopArrived(Trip trip, Integer toStopOrder) {
        if (toStopOrder == null) {
            return;
        }

        Optional<TripStop> arrived = tripStopRepository
                .findByTripTripIdOrderByStopOrderAsc(trip.getTripId()).stream()
                .filter(stop -> toStopOrder.equals(stop.getStopOrder()))
                .findFirst();

        if (arrived.isEmpty()) {
            log.warn("Trip {} leg ends at stop_order {} but no such stop exists", trip.getTripId(), toStopOrder);
            return;
        }

        TripStop stop = arrived.get();
        stop.setStatus(TripStopStatus.ARRIVED);
        tripStopRepository.save(stop);

        broadcastService.broadcastStopEvent(trip.getTripId(), stop.getId(), StopEventStatus.ARRIVED);
        log.debug("Trip {} arrived at stop {} (order {})", trip.getTripId(), stop.getId(), toStopOrder);
    }

    /**
     * Either starts the next leg after a dwell pause, or completes the trip if this was the last one.
     *
     * <p>The completion check is on the leg index rather than "have we run out of legs", so the trip
     * only finishes after actually traversing its final leg.
     */
    private void moveToNextLegOrComplete(Trip trip, List<TripLegRoute> legs, int legIndex) {
        boolean wasFinalLeg = legIndex >= legs.size() - 1;

        if (wasFinalLeg) {
            complete(trip);
            return;
        }

        trip.setCurrentLegIndex(legIndex + 1);
        trip.setCurrentPointIndex(0);
        trip.setDwellUntil(LocalDateTime.now().plusSeconds(dwellSeconds));
        log.debug("Trip {} advancing to leg {} after {}s dwell", trip.getTripId(), legIndex + 1, dwellSeconds);
    }

    private void complete(Trip trip) {
        trip.setStatus(STATUS_COMPLETED);
        trip.setArrivalTime(LocalDateTime.now());
        trip.setDwellUntil(null);
        log.info("Trip {} completed its final leg", trip.getTripId());
    }

    /**
     * Writes the new position to the trip, appends a history row, and broadcasts it.
     *
     * <p>All three happen together so the persisted position, the durable trail and what subscribers
     * see cannot drift apart.
     */
    private void publishPosition(Trip trip, double[] point, int legIndex) {
        double lat = point[0];
        double lng = point[1];

        trip.setCurrentLat(lat);
        trip.setCurrentLng(lng);

        locationHistoryRepository.save(new TripLocationHistory(trip, lat, lng, LocalDateTime.now()));
        broadcastService.broadcastLocationUpdate(trip.getTripId(), lat, lng, legIndex);
    }

    /**
     * Points to advance per tick for a given leg.
     *
     * <p>Derived from the leg's own ORS duration rather than being one flat value, so a long highway
     * leg and a short side-street leg take time proportional to reality — a fixed step would crawl
     * through a dense urban polyline and rocket along a sparse one.
     *
     * <p>{@code speedMultiplier} compresses real time: a 5-minute leg at 10x takes 30 seconds of wall
     * clock. Without compression a realistic trip would take as long to watch as to drive.
     *
     * <p>Always at least 1, otherwise a leg with more points than ticks would never advance.
     */
    private int stepSizeFor(TripLegRoute leg, int pointCount) {
        Double durationSeconds = leg.getDurationSeconds();

        if (durationSeconds == null || durationSeconds <= 0 || speedMultiplier <= 0) {
            // ORS omits the summary for zero-length routes, so duration can legitimately be absent.
            return Math.max(1, fallbackStepSize);
        }

        double tickIntervalSeconds = Math.max(tickIntervalMs / 1000.0, 0.001);
        double wallClockSecondsForLeg = durationSeconds / speedMultiplier;
        double ticksForLeg = Math.max(wallClockSecondsForLeg / tickIntervalSeconds, 1.0);

        return Math.max(1, (int) Math.ceil(pointCount / ticksForLeg));
    }

    /** Reads a stored leg polyline. Returns empty rather than throwing, so one bad row cannot stall a tick. */
    private List<double[]> parseGeometry(TripLegRoute leg) {
        String geometry = leg.getRouteGeometry();
        if (geometry == null || geometry.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.asList(objectMapper.readValue(geometry, double[][].class));
        } catch (JsonProcessingException ex) {
            log.warn("Leg {} has unreadable route_geometry: {}", leg.getId(), ex.getMessage());
            return List.of();
        }
    }

    /** Tracking columns are nullable for trips that predate tracking, so treat null as "at the start". */
    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
