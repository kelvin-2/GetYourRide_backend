package com.example1.getyourride.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on Spring's scheduled task support, which drives
 * {@code scheduler.TripSimulationScheduler}.
 *
 * <p>Kept as its own configuration class rather than annotating {@code GetYourRideApplication} so the
 * reason it exists is documented next to the switch, and so it sits with the other configuration in
 * this package.
 *
 * <p>Spring Boot's default scheduler is a single-threaded pool. That is intentional here: the
 * simulation tick is the only scheduled task, and a single thread means two ticks can never overlap
 * and race on the same trip's cursor. If more scheduled work is added later, size the pool
 * deliberately via {@code spring.task.scheduling.pool.size} rather than letting tasks queue behind
 * each other.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
