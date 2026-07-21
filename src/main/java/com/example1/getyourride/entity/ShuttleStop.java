package com.example1.getyourride.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "shuttle_stop")
public class ShuttleStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stop_id")
    private Long stopId;

    @Column(name = "stop_name", nullable = false)
    private String stopName; // e.g. "PSA, 163 Durban Rd"

    @Column(name = "area")
    private String area; // e.g. "Korsten" - the named pickup zone

    @Column(name = "location")
    private String location; // general area/city, e.g. "Gqeberha"

    @Column(name = "latitude")
    private Double latitude; // nullable - geocoded later, same pattern as trip_stop

    @Column(name = "longitude")
    private Double longitude;

    // ---- constructors ----
    public ShuttleStop() {
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