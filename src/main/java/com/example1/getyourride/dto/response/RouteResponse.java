package com.example1.getyourride.dto.response;

import java.util.List;

public class RouteResponse {
    private List<double[]> coordinates; // each entry: [lat, lng], in path order
    private double distanceMeters;
    private double durationSeconds;

    public RouteResponse(List<double[]> coordinates, double distanceMeters, double durationSeconds) {
        this.coordinates = coordinates;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
    }

    public List<double[]> getCoordinates() { return coordinates; }
    public double getDistanceMeters() { return distanceMeters; }
    public double getDurationSeconds() { return durationSeconds; }
}