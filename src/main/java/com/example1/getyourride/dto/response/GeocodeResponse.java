package com.example1.getyourride.dto.response;

public class GeocodeResponse {
    private boolean found;
    private double latitude;
    private double longitude;
    private String matchedAddress;

    public static GeocodeResponse notFound() {
        GeocodeResponse r = new GeocodeResponse();
        r.found = false;
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
}