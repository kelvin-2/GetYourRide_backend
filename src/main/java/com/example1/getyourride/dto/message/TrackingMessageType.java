package com.example1.getyourride.dto.message;

/**
 * Discriminator carried in the {@code type} field of every tracking message.
 *
 * <p>Subscribers receive both message shapes on the same destination
 * ({@code /topic/trip/{tripId}}), so they need a field to switch on. An enum rather than a bare
 * String because the wire values are a closed set, and Jackson serialises the constant name
 * verbatim — {@code LOCATION_UPDATE} and {@code STOP_EVENT} — matching the contract documented in
 * {@code GetYourRide_Tracking_Documentation.md} §4.4.
 *
 * <p>Renaming a constant here is a breaking change for the Android client.
 */
public enum TrackingMessageType {

    /** Per-tick position update while a trip is in progress. */
    LOCATION_UPDATE,

    /** Stop lifecycle event, published when the vehicle reaches a stop. */
    STOP_EVENT
}
