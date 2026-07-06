package com.example1.getyourride.dto.response;

public class AddressSuggestion {
    private String displayName;
    private double latitude;
    private double longitude;

    public AddressSuggestion(String displayName, double latitude, double longitude) {
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getDisplayName() { return displayName; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}