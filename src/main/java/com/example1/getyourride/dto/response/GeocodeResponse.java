package com.example1.getyourride.dto.response;

public class GeocodeResponse {
    private boolean found;
    private double latitude;
    private double longitude;
    private String matchedAddress;
    private String message; // null when found=true; explains why not, when false

    public static GeocodeResponse notFound() {
        return notFound("No matching address found. Please check the spelling and try again.");
    }

    public static GeocodeResponse notFound(String message) {
        GeocodeResponse r = new GeocodeResponse();
        r.found = false;
        r.message = message;
        return r;
    }

    public static GeocodeResponse of(double lat, double lng, String matchedAddress) {
        GeocodeResponse r = new GeocodeResponse();
        r.found = true;
        r.latitude = lat;
        r.longitude = lng;
        r.matchedAddress = matchedAddress;
        return r;
    }

    public boolean isFound() { return found; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getMatchedAddress() { return matchedAddress; }
    public String getMessage() { return message; }
}