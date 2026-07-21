package com.example1.getyourride.dto.response;

public class ShuttleStopResponse {

    private Long stopId;
    private String stopName;
    private String area;
    private String location;
    private Double latitude;
    private Double longitude;

    public ShuttleStopResponse() {
    }

    public ShuttleStopResponse(Long stopId, String stopName, String area,
                               String location, Double latitude, Double longitude) {
        this.stopId = stopId;
        this.stopName = stopName;
        this.area = area;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // ---- getters and setters ----
    public Long getStopId() {
        return stopId;
    }

    public void setStopId(Long stopId) {
        this.stopId = stopId;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}