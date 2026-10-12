package com.example1.getyourride.service;

public interface ShuttleTripCompletionService {
    /**
     * Finds and processes all SHUTTLE trips that have reached their slot end time.
     * Marks such trips as COMPLETED and unboarded confirmed bookings as NO_SHOW.
     */
    void completeExpiredShuttleTrips();
}
