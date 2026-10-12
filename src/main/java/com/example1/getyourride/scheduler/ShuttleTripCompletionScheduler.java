package com.example1.getyourride.scheduler;

import com.example1.getyourride.service.ShuttleTripCompletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ShuttleTripCompletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ShuttleTripCompletionScheduler.class);

    private final ShuttleTripCompletionService shuttleTripCompletionService;

    public ShuttleTripCompletionScheduler(ShuttleTripCompletionService shuttleTripCompletionService) {
        this.shuttleTripCompletionService = shuttleTripCompletionService;
    }

    /**
     * Periodically checks for and completes expired SHUTTLE trips.
     * Runs every 2 minutes.
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void completeExpiredTrips() {
        try {
            shuttleTripCompletionService.completeExpiredShuttleTrips();
        } catch (Exception e) {
            log.error("Error occurred while completing expired shuttle trips: {}", e.getMessage(), e);
        }
    }
}
