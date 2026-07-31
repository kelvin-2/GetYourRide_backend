package com.example1.getyourride.scheduler;

import com.example1.getyourride.service.TripSimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodic trigger that advances every in-progress trip one step along its route.
 *
 * <h2>Why this is a separate bean from {@link TripSimulationService}</h2>
 * The Phase 4 deliverables describe a {@code @Scheduled} tick method on the service itself. It lives
 * here instead for two connected reasons:
 *
 * <ol>
 *   <li><b>Per-trip transactions.</b> Each trip must advance in its own transaction so a single
 *       failing trip cannot roll back or block every other trip's progress. Spring applies
 *       {@code @Transactional} through a proxy, and a method calling another method on {@code this}
 *       bypasses that proxy — so a scheduled loop inside the service could not start a fresh
 *       transaction per trip. Calling across bean boundaries makes the proxy apply.</li>
 *   <li><b>Failure isolation.</b> The loop catches per trip, so one trip with corrupt geometry logs an
 *       error and the rest still move. A shared transaction would let one bad trip stall the whole
 *       simulation indefinitely.</li>
 * </ol>
 *
 * <p>Together these satisfy the acceptance criterion that no two trips interfere with each other's
 * {@code current_leg_index}/{@code current_point_index} state.
 *
 * <h2>Enabling and pacing</h2>
 * <b>Disabled unless {@code getyourride.tracking.simulation.enabled=true}.</b> With the flag absent
 * this bean is not registered and no tick ever fires.
 *
 * <p>Off by default because this writes to the database on a timer: every tick mutates
 * {@code trip.current_*} and appends {@code trip_location_history} rows for any trip that happens to be
 * {@code IN_PROGRESS}. Anything that starts an application context inherits that — including the
 * {@code @SpringBootTest} suite, which runs against the real configured database. An opt-in flag means a
 * test run or a stray local start cannot quietly advance live trips, and it matches the roadmap's
 * instruction to dry-run the tick against one trip before enabling it broadly.
 *
 * <p>The interval is {@code getyourride.tracking.simulation.tick-interval-ms} (default 4000, matching
 * §4.3). It is read at annotation level, so changing it needs a restart. {@code fixedRateString} rather
 * than {@code fixedDelayString} keeps the tick cadence steady regardless of how long a tick takes.
 */
@Component
@ConditionalOnProperty(
        name = "getyourride.tracking.simulation.enabled",
        havingValue = "true")
public class TripSimulationScheduler {

    private static final Logger log = LoggerFactory.getLogger(TripSimulationScheduler.class);

    private final TripSimulationService tripSimulationService;
    private final long tickIntervalMs;

    public TripSimulationScheduler(
            TripSimulationService tripSimulationService,
            @Value("${getyourride.tracking.simulation.tick-interval-ms:4000}") long tickIntervalMs) {
        this.tripSimulationService = tripSimulationService;
        this.tickIntervalMs = tickIntervalMs;
        log.info("Trip simulation scheduler enabled, ticking every {}ms", tickIntervalMs);
    }

    /**
     * Advances all in-progress trips by one step.
     *
     * <p>Exceptions are caught per trip and never rethrown. An exception escaping a {@code @Scheduled}
     * method does not stop future executions in Spring, but it would abandon the remaining trips in
     * this pass — so a single bad trip would starve the others on every tick.
     */
    @Scheduled(fixedRateString = "${getyourride.tracking.simulation.tick-interval-ms:4000}")
    public void tick() {
        List<Long> activeTripIds;
        try {
            activeTripIds = tripSimulationService.findActiveTripIds();
        } catch (RuntimeException ex) {
            // Typically a database blip. Logged and dropped; the next tick retries.
            log.error("Could not load active trips for simulation tick: {}", ex.getMessage());
            return;
        }

        if (activeTripIds.isEmpty()) {
            return;
        }

        log.debug("Simulation tick advancing {} trip(s)", activeTripIds.size());

        for (Long tripId : activeTripIds) {
            try {
                tripSimulationService.advanceTrip(tripId);
            } catch (RuntimeException ex) {
                log.error("Failed to advance trip {}: {}", tripId, ex.getMessage(), ex);
            }
        }
    }

    /** Exposed for logging and tests; the annotation reads the property independently. */
    public long getTickIntervalMs() {
        return tickIntervalMs;
    }
}
