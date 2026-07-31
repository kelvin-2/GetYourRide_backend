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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the simulation engine.
 *
 * <p>Mockito throughout: no database, no broker, no ORS. The tuning values are passed explicitly to the
 * constructor rather than read from properties, so every step-size assertion below is arithmetic that
 * can be checked by hand rather than a magic number.
 *
 * <p>With {@code tickInterval=4000ms} and {@code speedMultiplier=10}, a leg reporting 400s of ORS
 * duration is meant to take 40s of wall clock, which is 10 ticks. Across a 100-point polyline that is
 * 10 points per tick — the figure most of these tests rely on.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TripSimulationServiceImplTest {

    private static final Long TRIP_ID = 42L;
    private static final long TICK_INTERVAL_MS = 4000;
    private static final double SPEED_MULTIPLIER = 10.0;
    private static final long DWELL_SECONDS = 20;
    private static final int FALLBACK_STEP_SIZE = 5;

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripStopRepository tripStopRepository;
    @Mock
    private TripLegRouteRepository tripLegRouteRepository;
    @Mock
    private TripLocationHistoryRepository locationHistoryRepository;
    @Mock
    private TrackingBroadcastService broadcastService;

    private TripSimulationServiceImpl service;
    private Trip trip;

    @BeforeEach
    void setUp() {
        service = new TripSimulationServiceImpl(
                tripRepository, tripStopRepository, tripLegRouteRepository,
                locationHistoryRepository, broadcastService, new ObjectMapper(),
                TICK_INTERVAL_MS, SPEED_MULTIPLIER, DWELL_SECONDS, FALLBACK_STEP_SIZE);

        trip = new Trip();
        trip.setTripId(TRIP_ID);
        trip.setStatus("IN_PROGRESS");
        trip.setCurrentLegIndex(0);
        trip.setCurrentPointIndex(0);

        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
    }

    // --- fixtures ---------------------------------------------------------

    /** Polyline of {@code count} points marching south-east, as stored in route_geometry. */
    private String geometryJson(int count) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(String.format("[%.4f,%.4f]", -33.9 - i * 0.001, 25.6 + i * 0.001));
        }
        return json.append(']').toString();
    }

    private TripLegRoute leg(Long id, int fromOrder, int toOrder, int pointCount, Double durationSeconds) {
        TripLegRoute leg = new TripLegRoute();
        leg.setId(id);
        leg.setTrip(trip);
        leg.setFromStopOrder(fromOrder);
        leg.setToStopOrder(toOrder);
        leg.setRouteGeometry(geometryJson(pointCount));
        leg.setDistanceMeters(1000.0);
        leg.setDurationSeconds(durationSeconds);
        return leg;
    }

    private TripStop stop(Long id, int order) {
        TripStop stop = new TripStop();
        stop.setId(id);
        stop.setTrip(trip);
        stop.setStopOrder(order);
        stop.setStopName("Stop " + order);
        stop.setLatitude(-33.9 - order * 0.01);
        stop.setLongitude(25.6 + order * 0.01);
        stop.setStatus(TripStopStatus.PENDING);
        return stop;
    }

    private void givenLegs(TripLegRoute... legs) {
        when(tripLegRouteRepository.findByTripTripIdOrderByFromStopOrderAsc(TRIP_ID))
                .thenReturn(Arrays.asList(legs));
    }

    private void givenStops(TripStop... stops) {
        when(tripStopRepository.findByTripTripIdOrderByStopOrderAsc(TRIP_ID))
                .thenReturn(new ArrayList<>(Arrays.asList(stops)));
    }

    // --- guards -----------------------------------------------------------

    @Nested
    @DisplayName("Trips that should not move")
    class Guards {

        @Test
        @DisplayName("A trip that is no longer IN_PROGRESS is skipped")
        void skipsTripThatChangedStatus() {
            // The scheduler filtered on status, but it may have changed before this transaction ran.
            trip.setStatus("CANCELLED");
            givenLegs(leg(1L, 1, 2, 100, 400.0));

            service.advanceTrip(TRIP_ID);

            verify(broadcastService, never()).broadcastLocationUpdate(anyLong(), anyDouble(), anyDouble(), anyInt());
            verify(locationHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("A missing trip is ignored rather than throwing")
        void missingTripIgnored() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

            service.advanceTrip(TRIP_ID);

            verify(locationHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("A trip with no precomputed legs cannot move")
        void noLegsMeansNoMovement() {
            // Legitimate state: the trip was started before precompute-route was called.
            givenLegs();

            service.advanceTrip(TRIP_ID);

            verify(broadcastService, never()).broadcastLocationUpdate(anyLong(), anyDouble(), anyDouble(), anyInt());
            verify(locationHistoryRepository, never()).save(any());
        }
    }

    // --- mid-leg movement -------------------------------------------------

    @Nested
    @DisplayName("Moving along a leg")
    class Movement {

        @Test
        @DisplayName("A tick advances the point cursor by the derived step size")
        void advancesByDerivedStepSize() {
            // 100 points, 400s duration, 10x speed, 4s tick => 10 ticks => 10 points per tick.
            givenLegs(leg(1L, 1, 2, 100, 400.0));

            service.advanceTrip(TRIP_ID);

            assertEquals(10, trip.getCurrentPointIndex());
            assertEquals(0, trip.getCurrentLegIndex(), "Still on the same leg");
        }

        @Test
        @DisplayName("Position is written to the trip, recorded in history, and broadcast together")
        void updatesPositionHistoryAndBroadcast() {
            givenLegs(leg(1L, 1, 2, 100, 400.0));

            service.advanceTrip(TRIP_ID);

            // All three must agree — a client's map, the durable trail and the stored position should
            // never be able to drift apart.
            ArgumentCaptor<TripLocationHistory> history = ArgumentCaptor.forClass(TripLocationHistory.class);
            verify(locationHistoryRepository).save(history.capture());

            assertEquals(trip.getCurrentLat(), history.getValue().getLatitude());
            assertEquals(trip.getCurrentLng(), history.getValue().getLongitude());
            assertNotNull(history.getValue().getRecordedAt());
            verify(broadcastService).broadcastLocationUpdate(TRIP_ID, trip.getCurrentLat(), trip.getCurrentLng(), 0);
        }

        @Test
        @DisplayName("Successive ticks keep moving forward from the stored cursor")
        void resumesFromStoredCursor() {
            // This is what makes the simulation restart-safe: progress lives in the row, not in memory.
            givenLegs(leg(1L, 1, 2, 100, 400.0));

            service.advanceTrip(TRIP_ID);
            assertEquals(10, trip.getCurrentPointIndex());

            service.advanceTrip(TRIP_ID);
            assertEquals(20, trip.getCurrentPointIndex());

            service.advanceTrip(TRIP_ID);
            assertEquals(30, trip.getCurrentPointIndex());
        }

        @Test
        @DisplayName("A null cursor is treated as the start of the route")
        void nullCursorTreatedAsStart() {
            // Trips created before the tracking columns existed have nulls here.
            trip.setCurrentLegIndex(null);
            trip.setCurrentPointIndex(null);
            givenLegs(leg(1L, 1, 2, 100, 400.0));

            service.advanceTrip(TRIP_ID);

            assertEquals(10, trip.getCurrentPointIndex());
        }

        @Test
        @DisplayName("A leg with no usable duration falls back to the configured step size")
        void fallsBackWhenDurationMissing() {
            // ORS omits the summary for zero-length routes, so duration can legitimately be absent.
            givenLegs(leg(1L, 1, 2, 100, null));

            service.advanceTrip(TRIP_ID);

            assertEquals(FALLBACK_STEP_SIZE, trip.getCurrentPointIndex());
        }

        @Test
        @DisplayName("A short leg still advances at least one point per tick")
        void alwaysAdvancesAtLeastOnePoint() {
            // A very long duration would compute a sub-1 step and freeze the vehicle forever.
            givenLegs(leg(1L, 1, 2, 5, 100000.0));
            givenStops(stop(1L, 1), stop(2L, 2));

            service.advanceTrip(TRIP_ID);

            assertTrue(trip.getCurrentPointIndex() >= 1 || "COMPLETED".equals(trip.getStatus()),
                    "The vehicle must not stall");
        }
    }

    // --- arriving at a stop ----------------------------------------------

    @Nested
    @DisplayName("Arriving at a stop")
    class Arrival {

        @Test
        @DisplayName("Reaching the end of a leg marks the destination stop ARRIVED and broadcasts it")
        void marksStopArrived() {
            givenLegs(leg(1L, 1, 2, 100, 400.0), leg(2L, 2, 3, 100, 400.0));
            TripStop second = stop(20L, 2);
            givenStops(stop(10L, 1), second, stop(30L, 3));
            trip.setCurrentPointIndex(95); // next step of 10 overshoots the last index (99)

            service.advanceTrip(TRIP_ID);

            assertEquals(TripStopStatus.ARRIVED, second.getStatus());
            verify(broadcastService).broadcastStopEvent(TRIP_ID, 20L, StopEventStatus.ARRIVED);
        }

        @Test
        @DisplayName("The vehicle snaps to the leg's final point instead of overshooting the stop")
        void snapsToFinalPoint() {
            givenLegs(leg(1L, 1, 2, 100, 400.0), leg(2L, 2, 3, 100, 400.0));
            givenStops(stop(10L, 1), stop(20L, 2), stop(30L, 3));
            trip.setCurrentPointIndex(95);

            service.advanceTrip(TRIP_ID);

            // Point 99 of the generated polyline.
            assertEquals(-33.9 - 99 * 0.001, trip.getCurrentLat(), 1e-6);
        }

        @Test
        @DisplayName("Arrival advances to the next leg and resets the point cursor")
        void advancesToNextLeg() {
            givenLegs(leg(1L, 1, 2, 100, 400.0), leg(2L, 2, 3, 100, 400.0));
            givenStops(stop(10L, 1), stop(20L, 2), stop(30L, 3));
            trip.setCurrentPointIndex(95);

            service.advanceTrip(TRIP_ID);

            assertEquals(1, trip.getCurrentLegIndex());
            assertEquals(0, trip.getCurrentPointIndex());
        }

        @Test
        @DisplayName("Arrival sets a dwell deadline to simulate boarding")
        void setsDwellDeadline() {
            givenLegs(leg(1L, 1, 2, 100, 400.0), leg(2L, 2, 3, 100, 400.0));
            givenStops(stop(10L, 1), stop(20L, 2), stop(30L, 3));
            trip.setCurrentPointIndex(95);

            LocalDateTime before = LocalDateTime.now();
            service.advanceTrip(TRIP_ID);

            assertNotNull(trip.getDwellUntil());
            assertTrue(trip.getDwellUntil().isAfter(before.plusSeconds(DWELL_SECONDS - 2)),
                    "Dwell should be roughly " + DWELL_SECONDS + "s out, was " + trip.getDwellUntil());
        }

        @Test
        @DisplayName("A leg ending at an unknown stop_order does not abort the tick")
        void unknownStopOrderTolerated() {
            // Stops could have been edited under a running trip.
            givenLegs(leg(1L, 1, 2, 100, 400.0), leg(2L, 2, 3, 100, 400.0));
            givenStops(stop(10L, 1));
            trip.setCurrentPointIndex(95);

            service.advanceTrip(TRIP_ID);

            // Movement still happened and the leg still advanced.
            verify(broadcastService).broadcastLocationUpdate(anyLong(), anyDouble(), anyDouble(), anyInt());
            assertEquals(1, trip.getCurrentLegIndex());
            verify(broadcastService, never()).broadcastStopEvent(anyLong(), anyLong(), any());
        }
    }

    // --- dwelling ---------------------------------------------------------

    @Nested
    @DisplayName("Dwelling at a stop")
    class Dwelling {

        @Test
        @DisplayName("While the dwell deadline is in the future the vehicle does not move")
        void doesNotMoveWhileDwelling() {
            trip.setDwellUntil(LocalDateTime.now().plusSeconds(30));
            givenLegs(leg(1L, 1, 2, 100, 400.0));
            trip.setCurrentPointIndex(0);

            service.advanceTrip(TRIP_ID);

            assertEquals(0, trip.getCurrentPointIndex(), "Cursor must not advance during a dwell");
            verify(locationHistoryRepository, never()).save(any());
            verify(broadcastService, never()).broadcastLocationUpdate(anyLong(), anyDouble(), anyDouble(), anyInt());
        }

        @Test
        @DisplayName("Once the dwell deadline passes the marker clears and movement resumes")
        void resumesAfterDwellExpires() {
            trip.setDwellUntil(LocalDateTime.now().minusSeconds(1));
            givenLegs(leg(1L, 1, 2, 100, 400.0));

            service.advanceTrip(TRIP_ID);

            assertNull(trip.getDwellUntil(), "Expired dwell must be cleared");
            assertEquals(10, trip.getCurrentPointIndex());
        }
    }

    // --- completion -------------------------------------------------------

    @Nested
    @DisplayName("Completing a trip")
    class Completion {

        @Test
        @DisplayName("Reaching the end of the final leg completes the trip")
        void completesOnFinalLeg() {
            givenLegs(leg(1L, 1, 2, 100, 400.0));
            givenStops(stop(10L, 1), stop(20L, 2));
            trip.setCurrentPointIndex(95);

            service.advanceTrip(TRIP_ID);

            assertEquals("COMPLETED", trip.getStatus());
            assertNotNull(trip.getArrivalTime());
            assertNull(trip.getDwellUntil(), "A completed trip should not be left dwelling");
        }

        @Test
        @DisplayName("The final stop is still marked ARRIVED when the trip completes")
        void finalStopMarkedArrived() {
            givenLegs(leg(1L, 1, 2, 100, 400.0));
            TripStop last = stop(20L, 2);
            givenStops(stop(10L, 1), last);
            trip.setCurrentPointIndex(95);

            service.advanceTrip(TRIP_ID);

            assertEquals(TripStopStatus.ARRIVED, last.getStatus());
            verify(broadcastService).broadcastStopEvent(TRIP_ID, 20L, StopEventStatus.ARRIVED);
        }

        @Test
        @DisplayName("A trip does NOT complete when a non-final leg ends")
        void doesNotCompleteEarly() {
            // Directly guards the acceptance criterion "COMPLETED after its final leg, not before".
            givenLegs(leg(1L, 1, 2, 100, 400.0), leg(2L, 2, 3, 100, 400.0), leg(3L, 3, 4, 100, 400.0));
            givenStops(stop(10L, 1), stop(20L, 2), stop(30L, 3), stop(40L, 4));
            trip.setCurrentPointIndex(95);

            service.advanceTrip(TRIP_ID);

            assertEquals("IN_PROGRESS", trip.getStatus());
            assertEquals(1, trip.getCurrentLegIndex());
        }

        @Test
        @DisplayName("A trip does not complete part-way along its final leg")
        void doesNotCompleteMidFinalLeg() {
            givenLegs(leg(1L, 1, 2, 100, 400.0));
            trip.setCurrentPointIndex(0);

            service.advanceTrip(TRIP_ID);

            assertEquals("IN_PROGRESS", trip.getStatus());
        }

        @Test
        @DisplayName("A leg index past the end of the route completes rather than stalling forever")
        void staleLegIndexCompletes() {
            // Happens if stops were removed and precompute re-run under a running trip.
            givenLegs(leg(1L, 1, 2, 100, 400.0));
            trip.setCurrentLegIndex(5);

            service.advanceTrip(TRIP_ID);

            assertEquals("COMPLETED", trip.getStatus());
        }

        @Test
        @DisplayName("A leg with unreadable geometry is stepped over, not fatal")
        void corruptGeometryAdvancesPastLeg() {
            TripLegRoute broken = leg(1L, 1, 2, 100, 400.0);
            broken.setRouteGeometry("not json");
            givenLegs(broken, leg(2L, 2, 3, 100, 400.0));

            service.advanceTrip(TRIP_ID);

            assertEquals(1, trip.getCurrentLegIndex(), "Should move past the unusable leg");
            assertEquals("IN_PROGRESS", trip.getStatus());
        }
    }

    // --- start / query ----------------------------------------------------

    @Nested
    @DisplayName("Starting tracking")
    class StartTracking {

        @Test
        @DisplayName("Starting resets the cursor to the beginning of the route")
        void resetsCursor() {
            // Without this a re-run trip would resume mid-route and appear to teleport.
            trip.setCurrentLegIndex(3);
            trip.setCurrentPointIndex(87);
            trip.setDwellUntil(LocalDateTime.now().plusMinutes(5));
            givenStops(stop(10L, 1), stop(20L, 2));

            service.startTracking(TRIP_ID);

            assertEquals(0, trip.getCurrentLegIndex());
            assertEquals(0, trip.getCurrentPointIndex());
            assertNull(trip.getDwellUntil());
        }

        @Test
        @DisplayName("Starting seeds the position at the first stop")
        void seedsPositionAtFirstStop() {
            // Gives a client somewhere to draw the marker before the first tick lands.
            TripStop first = stop(10L, 1);
            givenStops(first, stop(20L, 2));

            service.startTracking(TRIP_ID);

            assertEquals(first.getLatitude(), trip.getCurrentLat());
            assertEquals(first.getLongitude(), trip.getCurrentLng());
        }

        @Test
        @DisplayName("Starting clears arrivals left over from a previous run")
        void resetsStopStatuses() {
            TripStop first = stop(10L, 1);
            TripStop second = stop(20L, 2);
            first.setStatus(TripStopStatus.ARRIVED);
            second.setStatus(TripStopStatus.ARRIVED);
            givenStops(first, second);

            service.startTracking(TRIP_ID);

            assertEquals(TripStopStatus.PENDING, first.getStatus());
            assertEquals(TripStopStatus.PENDING, second.getStatus());
        }

        @Test
        @DisplayName("Starting a trip with no stops does not fail")
        void toleratesNoStops() {
            givenStops();

            service.startTracking(TRIP_ID);

            assertEquals(0, trip.getCurrentLegIndex());
        }

        @Test
        @DisplayName("Starting a missing trip is a no-op")
        void missingTripIsNoOp() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

            service.startTracking(TRIP_ID);

            verify(tripRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("findActiveTripIds returns only IN_PROGRESS trips")
    void findsOnlyInProgressTrips() {
        Trip other = new Trip();
        other.setTripId(99L);
        other.setStatus("IN_PROGRESS");
        when(tripRepository.findByStatus("IN_PROGRESS")).thenReturn(Arrays.asList(trip, other));

        List<Long> ids = service.findActiveTripIds();

        assertEquals(Arrays.asList(TRIP_ID, 99L), ids);
        verify(tripRepository).findByStatus("IN_PROGRESS");
    }

    @Test
    @DisplayName("Each trip's cursor is read and written independently")
    void tripCursorsAreIndependent() {
        // Acceptance criterion: no two trips interfere with each other's leg/point state. State lives
        // per row, so advancing one trip must leave another's untouched.
        Trip otherTrip = new Trip();
        otherTrip.setTripId(99L);
        otherTrip.setStatus("IN_PROGRESS");
        otherTrip.setCurrentLegIndex(0);
        otherTrip.setCurrentPointIndex(50);

        when(tripRepository.findById(99L)).thenReturn(Optional.of(otherTrip));
        when(tripLegRouteRepository.findByTripTripIdOrderByFromStopOrderAsc(anyLong()))
                .thenReturn(Collections.singletonList(leg(1L, 1, 2, 100, 400.0)));

        service.advanceTrip(TRIP_ID);
        service.advanceTrip(99L);

        assertEquals(10, trip.getCurrentPointIndex());
        assertEquals(60, otherTrip.getCurrentPointIndex());
    }
}
