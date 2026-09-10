package com.example1.getyourride.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example1.getyourride.dto.response.RouteResponse;
import com.example1.getyourride.dto.response.TripLegRouteResponse;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.entity.TripLegRoute;
import com.example1.getyourride.entity.TripStop;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.repository.TripLegRouteRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.repository.TripStopRepository;
import com.example1.getyourride.service.RouteService;
import com.example1.getyourride.service.TripRouteService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Default {@link TripRouteService}.
 *
 * <p>Reuses {@link RouteService} for every OpenRouteService call rather than issuing its own
 * HTTP requests, so the API key handling, the lng/lat flip and the error translation stay in
 * one place.
 */
@Service
public class TripRouteServiceImpl implements TripRouteService {

    private static final Logger log = LoggerFactory.getLogger(TripRouteServiceImpl.class);

    /**
     * Minimum waypoints needed to form a leg. With fewer than two there is no consecutive pair
     * to route between.
     */
    private static final int MIN_WAYPOINTS_FOR_A_LEG = 2;

    /**
     * {@code stop_order} assigned to the trip's own departure point.
     *
     * <p>Real {@code trip_stop} rows are numbered from 1, so 0 is free and keeps the legs in
     * travel order under {@code ORDER BY from_stop_order} without a separate sequence column.
     */
    private static final int DEPARTURE_STOP_ORDER = 0;

    /**
     * Consecutive waypoints closer together than this are treated as one.
     *
     * <p>Needed because the live data contains stops repeated at identical coordinates (trip 25
     * has three, trips 552 and 555 two each), and a student's pickup stop is frequently the
     * trip's departure point restated. Routing between two points at the same place returns a
     * zero-length route with no duration, which the simulator cannot derive a step size from —
     * the vehicle would sit still for a leg instead of moving. 25 m is below the accuracy of a
     * geocoded street address, so nothing meaningful is collapsed.
     */
    private static final double DUPLICATE_WAYPOINT_METERS = 25.0;

    /** Mean earth radius in metres, for the duplicate-waypoint distance check. */
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /**
     * Matches the tolerance used by {@code CoordinatesValidator}. Stops are validated at the
     * request boundary from Phase 1 onward, but rows created before that fix, or edited
     * directly in the database, can still hold 0,0 — and ORS would answer such a request with
     * a nonsensical route across the Atlantic rather than an error.
     */
    private static final double ZERO_TOLERANCE = 1e-6;

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final TripLegRouteRepository tripLegRouteRepository;
    private final RouteService routeService;
    private final ObjectMapper objectMapper;

    public TripRouteServiceImpl(TripRepository tripRepository,
                                TripStopRepository tripStopRepository,
                                TripLegRouteRepository tripLegRouteRepository,
                                RouteService routeService,
                                ObjectMapper objectMapper) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.tripLegRouteRepository = tripLegRouteRepository;
        this.routeService = routeService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getTripRoute(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        // departure_lat/lng and destination_lat/lng are nullable, and older trips predate
        // coordinate capture entirely. Failing with a clear message beats sending nulls to ORS
        // or silently substituting placeholders, which is what this endpoint used to do.
        assertRoutablePair(tripId, "departure", trip.getDepartureLat(), trip.getDepartureLng());
        assertRoutablePair(tripId, "destination", trip.getDestinationLat(), trip.getDestinationLng());

        return routeService.getRoute(
                trip.getDepartureLat(), trip.getDepartureLng(),
                trip.getDestinationLat(), trip.getDestinationLng());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Runs in a single transaction that spans the ORS calls. That holds a database
     * connection open across external HTTP requests, which is normally worth avoiding, but the
     * trade is deliberate: a trip has a handful of stops, this is an explicit setup action
     * rather than a hot path, and a partially written leg set would leave the Phase 4 simulator
     * with a route that stops halfway. Atomicity matters more here than connection hold time.
     */
    @Override
    @Transactional
    public List<TripLegRouteResponse> precomputeLegRoutes(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        // Read through the repository rather than trip.getStops() so the ordering is explicit
        // in the query and does not depend on the entity's @OrderBy being preserved.
        List<TripStop> stops = tripStopRepository.findByTripTripIdOrderByStopOrderAsc(tripId);

        List<Waypoint> waypoints = buildWaypoints(trip, stops);

        if (waypoints.size() < MIN_WAYPOINTS_FOR_A_LEG) {
            throw new BadRequestException(String.format(
                    "Trip %d resolves to %d distinct routable waypoint(s) from its departure, %d stop(s) "
                            + "and its destination. At least %d are required to build a leg route.",
                    tripId, waypoints.size(), stops.size(), MIN_WAYPOINTS_FOR_A_LEG));
        }

        // Replace rather than append, so re-running after a stop is added or removed does not
        // leave stale legs behind. flush() forces the delete to reach the database before the
        // inserts, instead of letting Hibernate choose the statement order.
        tripLegRouteRepository.deleteByTripTripId(tripId);
        tripLegRouteRepository.flush();

        List<TripLegRoute> legs = new ArrayList<>();
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Waypoint from = waypoints.get(i);
            Waypoint to = waypoints.get(i + 1);

            RouteResponse route = routeService.getRoute(from.lat(), from.lng(), to.lat(), to.lng());

            TripLegRoute leg = new TripLegRoute();
            leg.setTrip(trip);
            leg.setFromStopOrder(from.stopOrder());
            leg.setToStopOrder(to.stopOrder());
            leg.setRouteGeometry(serialiseGeometry(route.getCoordinates()));
            leg.setDistanceMeters(route.getDistanceMeters());
            leg.setDurationSeconds(route.getDurationSeconds());
            legs.add(leg);
        }

        List<TripLegRoute> saved = tripLegRouteRepository.saveAll(legs);
        log.info("Precomputed {} leg route(s) for trip {} across {} waypoint(s): {}",
                saved.size(), tripId, waypoints.size(), describe(waypoints));

        return toResponses(saved, nameByStopOrder(trip, stops));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripLegRouteResponse> getLegRoutes(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        List<TripLegRoute> legs = tripLegRouteRepository.findByTripTripIdOrderByFromStopOrderAsc(tripId);
        List<TripStop> stops = tripStopRepository.findByTripTripIdOrderByStopOrderAsc(tripId);
        return toResponses(legs, nameByStopOrder(trip, stops));
    }

    @Override
    @Transactional
    public List<TripLegRouteResponse> ensureLegRoutes(Long tripId, boolean force) {
        if (!force && tripLegRouteRepository.countByTripTripId(tripId) > 0) {
            log.debug("Trip {} already has legs; skipping precomputation", tripId);
            return getLegRoutes(tripId);
        }
        return precomputeLegRoutes(tripId);
    }

    /**
     * Resolves the trip into the ordered points the vehicle actually drives through:
     * its departure, then each {@code trip_stop} in {@code stop_order}, then its destination.
     *
     * <p>Tracking documentation §4.1 describes legs as consecutive {@code trip_stop} pairs only.
     * That is not enough on this data: {@code trip_stop} does not reliably start at the trip's
     * departure or end at its destination — trip 24's stops run Summerstrand → North Campus
     * while its {@code destination_stop} is South Campus — so a stop-only leg set leaves the
     * vehicle stranded short of where the trip claims to go. Bracketing the stops with the
     * trip's own endpoints is what makes the simulated vehicle arrive.
     *
     * <p>The endpoints are included only when they carry usable coordinates. A trip with stops
     * but no {@code departure_lat/lng} (all four of the pre-Phase-1 carpool trips are like this)
     * still gets a route between its stops rather than an outright failure.
     */
    private List<Waypoint> buildWaypoints(Trip trip, List<TripStop> stops) {
        Long tripId = trip.getTripId();

        // Stops keep failing hard on bad coordinates: they were validated at the request
        // boundary from Phase 1 onward, so a 0,0 stop means something wrote around the API and
        // silently routing past it would hide that.
        stops.forEach(stop -> assertRoutablePair(tripId,
                "stop " + stop.getStopOrder() + " (" + stop.getStopName() + ")",
                stop.getLatitude(), stop.getLongitude()));

        int maxStopOrder = stops.stream()
                .map(TripStop::getStopOrder)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(DEPARTURE_STOP_ORDER);

        List<Waypoint> ordered = new ArrayList<>();

        if (isRoutable(trip.getDepartureLat(), trip.getDepartureLng())) {
            ordered.add(new Waypoint(DEPARTURE_STOP_ORDER, trip.getDepartureStop(),
                    trip.getDepartureLat(), trip.getDepartureLng()));
        } else {
            log.warn("Trip {} has no usable departure coordinates; the route will start at its first stop",
                    tripId);
        }

        stops.forEach(stop -> ordered.add(new Waypoint(
                stop.getStopOrder(), stop.getStopName(), stop.getLatitude(), stop.getLongitude())));

        if (isRoutable(trip.getDestinationLat(), trip.getDestinationLng())) {
            ordered.add(new Waypoint(maxStopOrder + 1, trip.getDestinationStop(),
                    trip.getDestinationLat(), trip.getDestinationLng()));
        } else {
            log.warn("Trip {} has no usable destination coordinates; the route will end at its last stop, "
                    + "so the vehicle will never be seen arriving", tripId);
        }

        return dropConsecutiveDuplicates(tripId, ordered);
    }

    /**
     * Collapses runs of waypoints that describe the same place.
     *
     * <p>Keeps the first of each run rather than the last, so the earliest {@code stop_order}
     * survives and the leg sequence stays monotonic. See {@link #DUPLICATE_WAYPOINT_METERS} for
     * why this is necessary rather than defensive.
     */
    private List<Waypoint> dropConsecutiveDuplicates(Long tripId, List<Waypoint> waypoints) {
        List<Waypoint> distinct = new ArrayList<>(waypoints.size());
        for (Waypoint candidate : waypoints) {
            if (distinct.isEmpty()) {
                distinct.add(candidate);
                continue;
            }
            Waypoint previous = distinct.get(distinct.size() - 1);
            double metres = distanceMeters(previous.lat(), previous.lng(), candidate.lat(), candidate.lng());
            if (metres < DUPLICATE_WAYPOINT_METERS) {
                log.debug("Trip {}: waypoint order {} ({}) is {}m from order {} ({}); merging",
                        tripId, candidate.stopOrder(), candidate.name(), Math.round(metres),
                        previous.stopOrder(), previous.name());
            } else {
                distinct.add(candidate);
            }
        }
        if (distinct.size() < waypoints.size()) {
            log.info("Trip {}: collapsed {} waypoint(s) that repeated the same location",
                    tripId, waypoints.size() - distinct.size());
        }
        return distinct;
    }

    /** Great-circle distance in metres. Only used for the duplicate-waypoint threshold. */
    private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** True when a coordinate pair can be sent to ORS: present and not the 0,0 sentinel. */
    private boolean isRoutable(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && !(Math.abs(latitude) < ZERO_TOLERANCE && Math.abs(longitude) < ZERO_TOLERANCE);
    }

    private String describe(List<Waypoint> waypoints) {
        return waypoints.stream()
                .map(waypoint -> waypoint.stopOrder() + ":" + waypoint.name())
                .collect(java.util.stream.Collectors.joining(" -> "));
    }

    /**
     * Maps every {@code stop_order} that can appear on a leg to a display name, including the two
     * synthetic orders that are not {@code trip_stop} rows: {@link #DEPARTURE_STOP_ORDER} and the
     * destination's {@code maxStopOrder + 1}. Without these the trip's first and last legs would
     * report {@code null} names.
     */
    private java.util.Map<Integer, String> nameByStopOrder(Trip trip, List<TripStop> stops) {
        java.util.Map<Integer, String> names = new java.util.LinkedHashMap<>();
        names.put(DEPARTURE_STOP_ORDER, trip.getDepartureStop());

        int maxStopOrder = DEPARTURE_STOP_ORDER;
        for (TripStop stop : stops) {
            if (stop.getStopOrder() == null) {
                continue;
            }
            names.put(stop.getStopOrder(), stop.getStopName());
            maxStopOrder = Math.max(maxStopOrder, stop.getStopOrder());
        }

        names.put(maxStopOrder + 1, trip.getDestinationStop());
        return names;
    }

    /** One point the vehicle drives through: a trip endpoint or a {@code trip_stop}. */
    private record Waypoint(Integer stopOrder, String name, Double lat, Double lng) {
    }

    /**
     * Rejects coordinates that cannot be routed: missing, or the 0,0 sentinel that indicates a
     * client lost the selected address.
     *
     * @param label human-readable description of which coordinate pair failed, so the caller
     *              knows whether to fix the trip or a specific stop
     */
    private void assertRoutablePair(Long tripId, String label, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new BadRequestException(String.format(
                    "Trip %d has no %s coordinates, so a route cannot be calculated.", tripId, label));
        }
        if (Math.abs(latitude) < ZERO_TOLERANCE && Math.abs(longitude) < ZERO_TOLERANCE) {
            throw new BadRequestException(String.format(
                    "Trip %d has 0,0 %s coordinates, which is not a real location. "
                            + "Fix the stored coordinates before requesting a route.", tripId, label));
        }
    }

    /**
     * Serialises a polyline to the JSON stored in {@code trip_leg_route.route_geometry}: an
     * array of {@code [latitude, longitude]} pairs in travel order.
     */
    private String serialiseGeometry(List<double[]> coordinates) {
        try {
            return objectMapper.writeValueAsString(coordinates);
        } catch (JsonProcessingException ex) {
            // Serialising a list of double[] cannot realistically fail, but the checked
            // exception has to be handled and swallowing it would store an invalid geometry.
            throw new IllegalStateException("Failed to serialise route geometry to JSON", ex);
        }
    }

    /** Reads back a stored geometry. Returns an empty list if the JSON is unreadable. */
    private List<double[]> deserialiseGeometry(TripLegRoute leg) {
        String geometry = leg.getRouteGeometry();
        if (geometry == null || geometry.isBlank()) {
            return List.of();
        }
        try {
            double[][] points = objectMapper.readValue(geometry, double[][].class);
            return List.of(points);
        } catch (JsonProcessingException ex) {
            // Reported rather than thrown: a single corrupt row should not make the whole
            // leg listing fail, and the summary is still useful without the endpoints.
            log.warn("Leg {} on trip {} has unreadable route_geometry: {}",
                    leg.getId(), leg.getTrip() != null ? leg.getTrip().getTripId() : null, ex.getMessage());
            return List.of();
        }
    }

    private List<TripLegRouteResponse> toResponses(List<TripLegRoute> legs,
                                                   java.util.Map<Integer, String> nameByStopOrder) {
        List<TripLegRouteResponse> responses = new ArrayList<>(legs.size());
        for (int index = 0; index < legs.size(); index++) {
            TripLegRoute leg = legs.get(index);
            List<double[]> geometry = deserialiseGeometry(leg);

            responses.add(TripLegRouteResponse.builder()
                    .id(leg.getId())
                    .legIndex(index)
                    .fromStopOrder(leg.getFromStopOrder())
                    .toStopOrder(leg.getToStopOrder())
                    .fromStopName(nameByStopOrder.get(leg.getFromStopOrder()))
                    .toStopName(nameByStopOrder.get(leg.getToStopOrder()))
                    .distanceMeters(leg.getDistanceMeters())
                    .durationSeconds(leg.getDurationSeconds())
                    .pointCount(geometry.size())
                    .startPoint(geometry.isEmpty() ? null : geometry.get(0))
                    .endPoint(geometry.isEmpty() ? null : geometry.get(geometry.size() - 1))
                    .build());
        }
        return responses;
    }
}
