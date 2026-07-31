package com.example1.getyourride.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One recorded vehicle position for a trip — the durable breadcrumb trail.
 *
 * <p>Maps {@code trip_location_history}, added by the Phase 0 migration. The simulation engine writes
 * a row per tick while a trip is in progress.
 *
 * <p><b>Why persist this at all,</b> given {@code trip.current_lat/lng} already holds the latest
 * position: broadcast messages are fire-and-forget, so a client that connects late or drops a frame
 * has no way to reconstruct where the vehicle has been. This table is the record that makes the
 * WebSocket feed disposable — which is precisely what lets the broadcaster swallow publish failures
 * instead of retrying them.
 *
 * <p>Manual accessors, no Lombok, per the entity convention in {@code doc/project-rules.md}.
 */
@Entity
@Table(name = "trip_location_history")
public class TripLocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    /**
     * When this position was recorded.
     *
     * <p>The column defaults to {@code CURRENT_TIMESTAMP}, but the value is set explicitly in code so
     * that a batch of rows written in one tick share the timestamp the application saw, and so tests
     * can assert on it without depending on database clock behaviour.
     */
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    public TripLocationHistory() {
        // Required no-arg constructor for JPA/Hibernate
    }

    public TripLocationHistory(Trip trip, Double latitude, Double longitude, LocalDateTime recordedAt) {
        this.trip = trip;
        this.latitude = latitude;
        this.longitude = longitude;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
