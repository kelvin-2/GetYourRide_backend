package com.example1.getyourride.entity;

/**
 * Lifecycle state of a single stop on a trip, persisted in {@code trip_stop.status}.
 *
 * <p>Added by the {@code doc/02_trip_stop_status.sql} migration as
 * {@code ENUM('PENDING','ARRIVED') NOT NULL DEFAULT 'PENDING'}, so the constant names here must
 * match those database values exactly — they are mapped with {@code EnumType.STRING}.
 *
 * <p><b>Why this is separate from {@code StopEventStatus}:</b> that one is the WebSocket wire
 * contract, this one is the persistence model. They overlap on {@code ARRIVED} today but serve
 * different masters — {@code PENDING} is never broadcast, and a future schema value should not be
 * forced onto the Android client just because the column gained it. Coupling them would make every
 * database change a breaking API change.
 */
public enum TripStopStatus {

    /** The vehicle has not reached this stop yet. Default for every new stop. */
    PENDING,

    /** The vehicle has reached this stop. */
    ARRIVED
}
