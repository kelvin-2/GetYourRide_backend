package com.example1.getyourride.dto.message;

/**
 * Common contract for messages broadcast on a trip's tracking topic.
 *
 * <p>Exists so the broadcast service has one payload type to work with, and so any future message
 * shape is forced to carry the {@code type} discriminator that subscribers switch on.
 */
public interface TrackingMessage {

    /** Discriminator serialised as the {@code type} field. */
    TrackingMessageType getType();

    /** The trip this message concerns. Always matches the {@code tripId} in the destination. */
    Long getTripId();
}
