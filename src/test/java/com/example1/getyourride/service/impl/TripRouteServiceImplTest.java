package com.example1.getyourride.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link TripRouteServiceImpl}.
 *
 * <p>Mockito rather than {@code @SpringBootTest}: the two existing Spring-context test classes
 * cannot start without a reachable MySQL, and these tests need neither a database nor a live
 * OpenRouteService account. {@link RouteService} is mocked, so no real ORS quota is consumed.
 */
@ExtendWith(MockitoExtension.class)
class TripRouteServiceImplTest {

    private static final Long TRIP_ID = 42L;

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripStopRepository tripStopRepository;
    @Mock
    private TripLegRouteRepository tripLegRouteRepository;
    @Mock
    private RouteService routeService;

    // A real ObjectMapper, not a mock: the geometry JSON these tests assert on is exactly what
    // gets written to trip_leg_route.route_geometry, so it must be produced by real Jackson.
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TripRouteServiceImpl tripRouteService;

    private Trip trip;

    @BeforeEach
    void setUp() {
        trip = new Trip();
        trip.setTripId(TRIP_ID);
        trip.setDepartureStop("Walmer, 6th Avenue");
        trip.setDestinationStop("South Campus");
        trip.setDepartureLat(-33.9758);
        trip.setDepartureLng(25.5858);
        trip.setDestinationLat(-33.9984);
        trip.setDestinationLng(25.6750);
    }

    private TripStop stop(Long id, String name, int order, Double lat, Double lng) {
        TripStop stop = new TripStop();
        stop.setId(id);
        stop.setTrip(trip);
        stop.setStopName(name);
        stop.setStopOrder(order);
        stop.setLatitude(lat);
        stop.setLongitude(lng);
        return stop;
    }

    private RouteResponse route(double distanceMeters) {
        List<double[]> path = Arrays.asList(
                new double[]{-33.9758, 25.5858},
                new double[]{-33.9800, 25.6000},
                new double[]{-33.9984, 25.6750});
        return new RouteResponse(path, distanceMeters, distanceMeters / 10);
    }

    // ---------------------------------------------------------------------
    // getTripRoute — the placeholder-coordinate fix
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getTripRoute uses the trip's real coordinates, not the old placeholders")
    void usesRealTripCoordinates() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(routeService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(route(4200));

        tripRouteService.getTripRoute(TRIP_ID);

        // The regression guard for Phase 2: these must be the trip's own coordinates. The old
        // implementation always passed -33.9581, 25.6014 -> -33.9615, 25.6089.
        verify(routeService).getRoute(-33.9758, 25.5858, -33.9984, 25.6750);
    }

    @Test
    @DisplayName("getTripRoute returns the ORS distance and geometry unchanged")
    void returnsRouteFromOrs() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(routeService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(route(4200));

        RouteResponse response = tripRouteService.getTripRoute(TRIP_ID);

        assertEquals(4200, response.getDistanceMeters());
        assertEquals(3, response.getCoordinates().size());
    }

    @Test
    @DisplayName("getTripRoute returns 404 semantics for an unknown trip")
    void unknownTripIsNotFound() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tripRouteService.getTripRoute(TRIP_ID));
        verify(routeService, never()).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("getTripRoute rejects a trip with no departure coordinates")
    void missingDepartureCoordinatesRejected() {
        trip.setDepartureLat(null);
        trip.setDepartureLng(null);
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> tripRouteService.getTripRoute(TRIP_ID));

        assertTrue(ex.getMessage().contains("departure"), "Message should name the missing pair: " + ex.getMessage());
        // Never call ORS with nulls — that was the failure mode this guard exists to prevent.
        verify(routeService, never()).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("getTripRoute rejects a trip with 0,0 destination coordinates")
    void nullIslandDestinationRejected() {
        trip.setDestinationLat(0.0);
        trip.setDestinationLng(0.0);
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> tripRouteService.getTripRoute(TRIP_ID));

        assertTrue(ex.getMessage().contains("0,0"), "Actual: " + ex.getMessage());
        verify(routeService, never()).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    // ---------------------------------------------------------------------
    // precomputeLegRoutes
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Legs run departure -> each stop -> destination, so the vehicle actually arrives")
    void legsBracketStopsWithTripEndpoints() {
        // Stops deliberately distinct from the trip's own departure and destination, which is the
        // case this exists to cover: before legs were bracketed, the route ended at stop 3 and the
        // simulated vehicle never reached South Campus.
        List<TripStop> stops = Arrays.asList(
                stop(1L, "Newton Park", 1, -33.9457, 25.5661),
                stop(2L, "Library", 2, -33.9900, 25.6400),
                stop(3L, "Humewood", 3, -33.9756, 25.6406));

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(stops);
        when(routeService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(route(2000));
        when(tripLegRouteRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<TripLegRouteResponse> legs = tripRouteService.precomputeLegRoutes(TRIP_ID);

        assertEquals(4, legs.size(), "3 stops bracketed by 2 endpoints must yield 4 legs");
        verify(routeService, times(4)).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());

        // Legs must chain 0->1->2->3->4 so the simulator can walk them in order. Order 0 is the
        // trip's departure and order 4 its destination; neither is a trip_stop row.
        assertEquals(0, legs.get(0).getFromStopOrder());
        assertEquals(1, legs.get(0).getToStopOrder());
        assertEquals(3, legs.get(3).getFromStopOrder());
        assertEquals(4, legs.get(3).getToStopOrder());

        assertEquals(0, legs.get(0).getLegIndex());
        assertEquals(3, legs.get(3).getLegIndex());

        // The synthetic endpoint orders must still resolve to readable names.
        assertEquals("Walmer, 6th Avenue", legs.get(0).getFromStopName());
        assertEquals("South Campus", legs.get(3).getToStopName());
    }

    @Test
    @DisplayName("The first leg starts at the trip's departure coordinates")
    void firstLegStartsAtTripDeparture() {
        List<TripStop> stops = Collections.singletonList(
                stop(1L, "Library", 1, -33.9900, 25.6400));

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(stops);
        when(routeService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(route(2000));
        when(tripLegRouteRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        tripRouteService.precomputeLegRoutes(TRIP_ID);

        verify(routeService).getRoute(-33.9758, 25.5858, -33.9900, 25.6400);
        verify(routeService).getRoute(-33.9900, 25.6400, -33.9984, 25.6750);
    }

    @Test
    @DisplayName("Stops repeated at the same location are merged into one waypoint")
    void duplicateWaypointsAreMerged() {
        // Exactly the shape of trips 552 and 555 in the live database: the student's stop restates
        // the trip's departure point. Routing a zero-length leg returns no duration, which the
        // simulator cannot pace, so the vehicle would sit still for a leg instead of moving.
        List<TripStop> stops = Arrays.asList(
                stop(1L, "South Campus", 1, -33.9758, 25.5858),
                stop(2L, "South Campus again", 2, -33.9758, 25.5858),
                stop(3L, "Library", 3, -33.9900, 25.6400));

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(stops);
        when(routeService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(route(2000));
        when(tripLegRouteRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<TripLegRouteResponse> legs = tripRouteService.precomputeLegRoutes(TRIP_ID);

        // departure == stop 1 == stop 2, so those three collapse to one waypoint, leaving
        // departure -> Library -> destination.
        assertEquals(2, legs.size());
        verify(routeService, never()).getRoute(-33.9758, 25.5858, -33.9758, 25.5858);
    }

    @Test
    @DisplayName("Each leg is routed between its own pair of stop coordinates")
    void legsUseTheirOwnStopCoordinates() {
        List<TripStop> stops = Arrays.asList(
                stop(1L, "Walmer", 1, -33.9758, 25.5858),
                stop(2L, "Library", 2, -33.9900, 25.6400),
                stop(3L, "South Campus", 3, -33.9984, 25.6750));

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(stops);
        when(routeService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(route(2000));
        when(tripLegRouteRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        tripRouteService.precomputeLegRoutes(TRIP_ID);

        verify(routeService).getRoute(-33.9758, 25.5858, -33.9900, 25.6400);
        verify(routeService).getRoute(-33.9900, 25.6400, -33.9984, 25.6750);
    }

    @Test
    @DisplayName("Geometry is stored as JSON [lat,lng] pairs with ORS distance and duration")
    void storesGeometryAndSummary() {
        List<TripStop> stops = Arrays.asList(
                stop(1L, "Walmer", 1, -33.9758, 25.5858),
                stop(2L, "South Campus", 2, -33.9984, 25.6750));

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(stops);
        when(routeService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(route(3500));
        when(tripLegRouteRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        tripRouteService.precomputeLegRoutes(TRIP_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TripLegRoute>> captor = ArgumentCaptor.forClass(List.class);
        verify(tripLegRouteRepository).saveAll(captor.capture());

        TripLegRoute saved = captor.getValue().get(0);
        assertEquals(3500, saved.getDistanceMeters());
        assertEquals(350, saved.getDurationSeconds());
        assertEquals(TRIP_ID, saved.getTrip().getTripId());

        // Latitude first, matching RouteResponse and what the Android client expects. If this
        // ever flips, every simulated position would appear off the coast of Somalia.
        assertEquals("[[-33.9758,25.5858],[-33.98,25.6],[-33.9984,25.675]]", saved.getRouteGeometry());
    }

    @Test
    @DisplayName("Existing legs are deleted before new ones are written, so re-running is safe")
    void precomputeIsIdempotent() {
        List<TripStop> stops = Arrays.asList(
                stop(1L, "Walmer", 1, -33.9758, 25.5858),
                stop(2L, "South Campus", 2, -33.9984, 25.6750));

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(stops);
        when(routeService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(route(3500));
        when(tripLegRouteRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        tripRouteService.precomputeLegRoutes(TRIP_ID);

        verify(tripLegRouteRepository).deleteByTripTripId(TRIP_ID);
        verify(tripLegRouteRepository).flush();
    }

    @Test
    @DisplayName("A trip with no stops still routes directly from its departure to its destination")
    void noStopsStillProducesOneLeg() {
        // Previously rejected for having fewer than two stops. That rejection was the reason 332 of
        // the 353 trips in the live database could not be tracked at all: the overwhelming majority
        // have no trip_stop rows, only trip-level departure and destination coordinates.
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(new ArrayList<>());
        when(routeService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(route(4200));
        when(tripLegRouteRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<TripLegRouteResponse> legs = tripRouteService.precomputeLegRoutes(TRIP_ID);

        assertEquals(1, legs.size());
        assertEquals(0, legs.get(0).getFromStopOrder());
        assertEquals(1, legs.get(0).getToStopOrder());
        verify(routeService).getRoute(-33.9758, 25.5858, -33.9984, 25.6750);
    }

    @Test
    @DisplayName("A trip with neither stops nor coordinates has nothing to route and is rejected")
    void noStopsAndNoCoordinatesRejected() {
        trip.setDepartureLat(null);
        trip.setDepartureLng(null);
        trip.setDestinationLat(null);
        trip.setDestinationLng(null);

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(new ArrayList<>());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> tripRouteService.precomputeLegRoutes(TRIP_ID));

        assertTrue(ex.getMessage().contains("waypoint"), "Actual: " + ex.getMessage());
        verify(routeService, never()).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
        // Nothing must be deleted when the request is invalid.
        verify(tripLegRouteRepository, never()).deleteByTripTripId(anyLong());
    }

    @Test
    @DisplayName("A single stop that repeats the only coordinates available is rejected")
    void singleStopCollapsingToOneWaypointRejected() {
        trip.setDestinationLat(null);
        trip.setDestinationLng(null);

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID))
                .thenReturn(Collections.singletonList(stop(1L, "Walmer", 1, -33.9758, 25.5858)));

        // Departure and the only stop are the same place, and there is no destination to route to,
        // so one waypoint remains.
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> tripRouteService.precomputeLegRoutes(TRIP_ID));

        assertTrue(ex.getMessage().contains("waypoint"), "Actual: " + ex.getMessage());
        verify(routeService, never()).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
        verify(tripLegRouteRepository, never()).deleteByTripTripId(anyLong());
    }

    // ---------------------------------------------------------------------
    // ensureLegRoutes
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("ensureLegRoutes skips precomputation when legs already exist")
    void ensureReusesExistingLegs() {
        when(tripLegRouteRepository.countByTripTripId(TRIP_ID)).thenReturn(3L);
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripLegRouteRepository.findByTripTripIdOrderByFromStopOrderAsc(TRIP_ID))
                .thenReturn(new ArrayList<>());
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(new ArrayList<>());

        tripRouteService.ensureLegRoutes(TRIP_ID, false);

        // The point of the method: no ORS quota spent, and the existing legs are left in place.
        verify(routeService, never()).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
        verify(tripLegRouteRepository, never()).deleteByTripTripId(anyLong());
    }

    @Test
    @DisplayName("ensureLegRoutes with force=true recomputes even when legs exist")
    void ensureForceRecomputes() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(new ArrayList<>());
        when(routeService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(route(4200));
        when(tripLegRouteRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        tripRouteService.ensureLegRoutes(TRIP_ID, true);

        verify(tripLegRouteRepository).deleteByTripTripId(TRIP_ID);
        verify(routeService).getRoute(-33.9758, 25.5858, -33.9984, 25.6750);
    }

    @Test
    @DisplayName("A 0,0 stop is rejected before any ORS quota is spent")
    void nullIslandStopRejectedBeforeCallingOrs() {
        // Phase 0 cleaned existing bad rows and Phase 1 blocks new ones, but a row edited
        // directly in the database could still be 0,0 — and ORS would happily route across the
        // Atlantic rather than reporting an error.
        List<TripStop> stops = Arrays.asList(
                stop(1L, "Walmer", 1, -33.9758, 25.5858),
                stop(2L, "Broken stop", 2, 0.0, 0.0));

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(stops);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> tripRouteService.precomputeLegRoutes(TRIP_ID));

        assertTrue(ex.getMessage().contains("0,0"), "Actual: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Broken stop"), "Message should identify the stop: " + ex.getMessage());
        verify(routeService, never()).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
        verify(tripLegRouteRepository, never()).deleteByTripTripId(anyLong());
    }

    @Test
    @DisplayName("precomputeLegRoutes returns 404 semantics for an unknown trip")
    void precomputeUnknownTripIsNotFound() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tripRouteService.precomputeLegRoutes(TRIP_ID));
    }

    // ---------------------------------------------------------------------
    // getLegRoutes
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getLegRoutes summarises stored geometry with endpoints and point count")
    void getLegRoutesSummarisesGeometry() {
        TripLegRoute leg = new TripLegRoute();
        leg.setId(7L);
        leg.setTrip(trip);
        leg.setFromStopOrder(1);
        leg.setToStopOrder(2);
        leg.setDistanceMeters(3500.0);
        leg.setDurationSeconds(350.0);
        leg.setRouteGeometry("[[-33.9758,25.5858],[-33.98,25.6],[-33.9984,25.675]]");

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripLegRouteRepository.findByTripTripIdOrderByFromStopOrderAsc(TRIP_ID))
                .thenReturn(Collections.singletonList(leg));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(Arrays.asList(
                stop(1L, "Walmer", 1, -33.9758, 25.5858),
                stop(2L, "South Campus", 2, -33.9984, 25.6750)));

        List<TripLegRouteResponse> responses = tripRouteService.getLegRoutes(TRIP_ID);

        assertEquals(1, responses.size());
        TripLegRouteResponse response = responses.get(0);
        assertEquals(3, response.getPointCount());
        assertEquals("Walmer", response.getFromStopName());
        assertEquals("South Campus", response.getToStopName());
        assertNotNull(response.getStartPoint());
        assertEquals(-33.9758, response.getStartPoint()[0]);
        assertEquals(25.675, response.getEndPoint()[1]);
    }

    @Test
    @DisplayName("getLegRoutes is empty when precomputation has not run")
    void getLegRoutesEmptyBeforePrecompute() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripLegRouteRepository.findByTripTripIdOrderByFromStopOrderAsc(TRIP_ID))
                .thenReturn(new ArrayList<>());
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(new ArrayList<>());

        assertTrue(tripRouteService.getLegRoutes(TRIP_ID).isEmpty());
    }

    @Test
    @DisplayName("getLegRoutes returns 404 semantics for an unknown trip")
    void getLegRoutesUnknownTripIsNotFound() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tripRouteService.getLegRoutes(TRIP_ID));
    }

    @Test
    @DisplayName("A leg with unreadable geometry still returns a usable summary")
    void corruptGeometryDoesNotFailTheListing() {
        // One bad row should not take down the whole listing; distance and stop names are still
        // useful for diagnosing the problem.
        TripLegRoute leg = new TripLegRoute();
        leg.setId(7L);
        leg.setTrip(trip);
        leg.setFromStopOrder(1);
        leg.setToStopOrder(2);
        leg.setDistanceMeters(3500.0);
        leg.setRouteGeometry("not json");

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripLegRouteRepository.findByTripTripIdOrderByFromStopOrderAsc(TRIP_ID))
                .thenReturn(Collections.singletonList(leg));
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID)).thenReturn(new ArrayList<>());

        List<TripLegRouteResponse> responses = tripRouteService.getLegRoutes(TRIP_ID);

        assertEquals(1, responses.size());
        assertEquals(0, responses.get(0).getPointCount());
        assertEquals(3500.0, responses.get(0).getDistanceMeters());
    }
}
