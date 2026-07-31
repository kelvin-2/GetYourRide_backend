package com.example1.getyourride.dto.message;

/**
 * Lifecycle state reported by a {@link StopEventDTO}.
 *
 * <p>Only {@code ARRIVED} is defined, because that is the only value in the documented contract
 * (§4.4). Deliberately not padded out with speculative values such as {@code DEPARTED} or
 * {@code SKIPPED} — Phase 4 owns the stop-arrival logic and can add what it actually needs.
 *
 * <p>Note this is a <em>message</em> field only. The {@code trip_stop} table has no {@code status}
 * column in the documented schema, so nothing here is persisted yet. See the note on Phase 4 in
 * {@code doc/Task}.
 */
public enum StopEventStatus {

    /** The vehicle has reached this stop. */
    ARRIVED
}
