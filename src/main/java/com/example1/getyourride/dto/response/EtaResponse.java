package com.example1.getyourride.dto.response;

/** ETA for an in-progress trip, calculated live via Google's Compute Routes API. */
public class EtaResponse {

    private Long tripId;
    private Double etaSeconds;
    private Integer etaMinutes;
    private Double distanceMeters;

    public EtaResponse() {
        // Required no-arg constructor for Jackson serialization
    }

    public EtaResponse(Long tripId, Double etaSeconds, Integer etaMinutes, Double distanceMeters) {
        this.tripId = tripId;
        this.etaSeconds = etaSeconds;
        this.etaMinutes = etaMinutes;
        this.distanceMeters = distanceMeters;
    }

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }

    public Double getEtaSeconds() { return etaSeconds; }
    public void setEtaSeconds(Double etaSeconds) { this.etaSeconds = etaSeconds; }

    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Integer etaMinutes) { this.etaMinutes = etaMinutes; }

    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }
}