package com.example1.getyourride.exception;

/**
 * Thrown when a booking has neither a campus pickup nor a campus drop-off.
 * Mapped to HTTP 400 with errorCode CAMPUS_REQUIRED so the frontend can show a friendly message.
 */
public class CampusRequiredException extends BadRequestException {

    public static final String ERROR_CODE = "CAMPUS_REQUIRED";

    public static final String FRIENDLY_MESSAGE =
            "Hold on! At least one of your locations (pickup or drop-off) must be an NMU campus: "
                    + "North Campus, South Campus, Missionvale Campus or 2nd Avenue Campus. "
                    + "Please update your trip and try again.";

    public CampusRequiredException() {
        super(FRIENDLY_MESSAGE);
    }
}
