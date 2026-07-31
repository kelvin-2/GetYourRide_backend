package com.example1.getyourride.service.impl;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
     * Minimum stops needed to form a leg. With fewer than two there is no consecutive pair to
     * route between.
     */
    private static final int MIN_STOPS_FOR_A_LEG = 2;

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

        if (stops.size() < MIN_STOPS_FOR_A_LEG) {
            throw new BadRequestException(String.format(
                    "Trip %d has %d stop(s). At least %d ordered stops are required to build a leg route.",
                    tripId, stops.size(), MIN_STOPS_FOR_A_LEG));
        }

        stops.forEach(stop -> assertRoutablePair(tripId,
                "stop " + stop.getStopOrder() + " (" + stop.getStopName() + ")",
                stop.getLatitude(), stop.getLongitude()));

        // Replace rather than append, so re-running after a stop is added or removed does not
        // leave stale legs behind. flush() forces the delete to reach the database before the
        // inserts, instead of letting Hibernate choose the statement order.
        tripLegRouteRepository.deleteByTripTripId(tripId);
        tripLegRouteRepository.flush();

        List<TripLegRoute> legs = new ArrayList<>();
        for (int i = 0; i < stops.size() - 1; i++) {
            TripStop from = stops.get(i);
            TripStop to = stops.get(i + 1);

            RouteResponse route = routeService.getRoute(
                    from.getLatitude(), from.getLongitude(),
                    to.getLatitude(), to.getLongitude());

            TripLegRoute leg = new TripLegRoute();
            leg.setTrip(trip);
            leg.setFromStopOrder(from.getStopOrder());
            leg.setToStopOrder(to.getStopOrder());
            leg.setRouteGeometry(serialiseGeometry(route.getCoordinates()));
            leg.setDistanceMeters(route.getDistanceMeters());
            leg.setDurationSeconds(route.getDurationSeconds());
            legs.add(leg);
        }

        List<TripLegRoute> saved = tripLegRouteRepository.saveAll(legs);
        log.info("Precomputed {} leg route(s) for trip {} across {} stops", saved.size(), tripId, stops.size());

        return toResponses(saved, stops);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripLegRouteResponse> getLegRoutes(Long tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("Trip not found with id: " + tripId);
        }

        List<TripLegRoute> legs = tripLegRouteRepository.findByTripTripIdOrderByFromStopOrderAsc(tripId);
        return toResponses(legs, tripStopRepository.findByTripTripIdOrderByStopOrderAsc(tripId));
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

    private List<TripLegRouteResponse> toResponses(List<TripLegRoute> legs, List<TripStop> stops) {
        List<TripLegRouteResponse> responses = new ArrayList<>(legs.size());
        for (int index = 0; index < legs.size(); index++) {
            TripLegRoute leg = legs.get(index);
            List<double[]> geometry = deserialiseGeometry(leg);

            responses.add(TripLegRouteResponse.builder()
                    .id(leg.getId())
                    .legIndex(index)
                    .fromStopOrder(leg.getFromStopOrder())
                    .toStopOrder(leg.getToStopOrder())
                    .fromStopName(stopNameFor(stops, leg.getFromStopOrder()))
                    .toStopName(stopNameFor(stops, leg.getToStopOrder()))
                    .distanceMeters(leg.getDistanceMeters())
                    .durationSeconds(leg.getDurationSeconds())
                    .pointCount(geometry.size())
                    .startPoint(geometry.isEmpty() ? null : geometry.get(0))
                    .endPoint(geometry.isEmpty() ? null : geometry.get(geometry.size() - 1))
                    .build());
        }
        return responses;
    }

    private String stopNameFor(List<TripStop> stops, Integer stopOrder) {
        if (stopOrder == null) {
            return null;
        }
        return stops.stream()
                .filter(stop -> stopOrder.equals(stop.getStopOrder()))
                .map(TripStop::getStopName)
                .findFirst()
                .orElse(null);
    }
}
