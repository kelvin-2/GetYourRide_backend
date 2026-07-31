package com.example1.getyourride.scheduler;

import com.example1.getyourride.service.TripSimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the scheduled tick loop's isolation guarantees.
 *
 * <p>The interesting behaviour here is not that trips advance — that is
 * {@code TripSimulationServiceImplTest}'s job — but that a single misbehaving trip cannot take the
 * whole simulation down with it. That is the acceptance criterion about trips not interfering with each
 * other, and it is the reason the scheduler is a separate bean from the service.
 */
@ExtendWith(MockitoExtension.class)
class TripSimulationSchedulerTest {

    @Mock
    private TripSimulationService tripSimulationService;

    private TripSimulationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TripSimulationScheduler(tripSimulationService, 4000);
    }

    @Test
    @DisplayName("Every active trip is advanced once per tick")
    void advancesEveryActiveTrip() {
        when(tripSimulationService.findActiveTripIds()).thenReturn(Arrays.asList(1L, 2L, 3L));

        scheduler.tick();

        verify(tripSimulationService).advanceTrip(1L);
        verify(tripSimulationService).advanceTrip(2L);
        verify(tripSimulationService).advanceTrip(3L);
    }

    @Test
    @DisplayName("One failing trip does not stop the others from advancing")
    void oneFailingTripDoesNotBlockOthers() {
        // A trip with corrupt data must not starve every other trip on every tick.
        when(tripSimulationService.findActiveTripIds()).thenReturn(Arrays.asList(1L, 2L, 3L));
        doThrow(new IllegalStateException("corrupt geometry")).when(tripSimulationService).advanceTrip(2L);

        assertDoesNotThrow(scheduler::tick);

        verify(tripSimulationService).advanceTrip(1L);
        verify(tripSimulationService).advanceTrip(2L);
        verify(tripSimulationService).advanceTrip(3L);
    }

    @Test
    @DisplayName("No active trips means no work")
    void noActiveTripsMeansNoWork() {
        when(tripSimulationService.findActiveTripIds()).thenReturn(Collections.emptyList());

        scheduler.tick();

        verify(tripSimulationService, never()).advanceTrip(anyLong());
    }

    @Test
    @DisplayName("A failure loading active trips is swallowed so the next tick can retry")
    void handlesLookupFailure() {
        // Typically a database blip. An escaping exception would not stop future ticks in Spring, but
        // swallowing it keeps the log readable and the behaviour explicit.
        when(tripSimulationService.findActiveTripIds()).thenThrow(new IllegalStateException("db down"));

        assertDoesNotThrow(scheduler::tick);

        verify(tripSimulationService, never()).advanceTrip(anyLong());
    }

    @Test
    @DisplayName("The configured tick interval is retained")
    void retainsTickInterval() {
        assertEquals(4000, scheduler.getTickIntervalMs());
    }
}
