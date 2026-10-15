package com.example1.getyourride.validation;

import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.entity.TripStop;
import java.util.List;
import java.util.Arrays;

public class CampusValidationUtils {

    private static final List<String> CAMPUS_KEYWORDS = Arrays.asList(
            "north campus",
            "south campus",
            "missionvale",
            "missionvile",
            "2nd avenue",
            "2 avenue",
            "second avenue",
            "bird street"
    );

    public static boolean hasCampusStop(Trip trip) {
        if (isCampus(trip.getDepartureStop()) || isCampus(trip.getDestinationStop())) {
            return true;
        }
        if (trip.getStops() != null) {
            for (TripStop stop : trip.getStops()) {
                if (isCampus(stop.getStopName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isCampus(String stopName) {
        if (stopName == null) return false;
        String name = stopName.toLowerCase();
        for (String keyword : CAMPUS_KEYWORDS) {
            if (name.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
