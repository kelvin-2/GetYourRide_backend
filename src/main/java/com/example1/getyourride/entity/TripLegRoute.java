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

/**
 * One precomputed road-following route between two consecutive stops on a trip.
 *
 * <p>Maps the {@code trip_leg_route} table added by the Phase 0 migration. Tracking is
 * leg-based: the vehicle moves stop1 -> stop2 -> stop3, following the real road geometry of
 * one leg at a time, so each leg's polyline is fetched from OpenRouteService once and
 * stored here rather than being requested on every simulation tick. ORS has a request
 * quota and adds latency, so a tick loop calling it live would be both slow and fragile.
 *
 * <p>Rows are identified by the stop pair they connect ({@code fromStopOrder} ->
 * {@code toStopOrder}) rather than by a leg index column, because {@code stop_order} is
 * already the canonical ordering on {@code trip_stop}. The simulator's
 * {@code current_leg_index} is the zero-based position within a trip's legs sorted by
 * {@code fromStopOrder}.
 *
 * <p>Manual accessors, no Lombok, per the entity convention in {@code doc/project-rules.md}.
 */
@Entity
@Table(name = "trip_leg_route")
public class TripLegRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    /** {@code stop_order} of the stop this leg starts at. */
    @Column(name = "from_stop_order", nullable = false)
    private Integer fromStopOrder;

    /** {@code stop_order} of the stop this leg ends at. */
    @Column(name = "to_stop_order", nullable = false)
    private Integer toStopOrder;

    /**
     * The leg's polyline as a JSON array of {@code [latitude, longitude]} pairs in travel
     * order, for example {@code [[-33.97,25.58],[-33.98,25.59]]}.
     *
     * <p>Held as a {@code String} rather than a mapped object graph: the column is MySQL
     * {@code json}, and MySQL accepts and returns valid JSON text through a string
     * parameter. Keeping it opaque at the persistence layer means the geometry format is
     * decided in one place (the service that writes it) and there is no risk of Hibernate
     * re-encoding the array structure. Callers deserialise with Jackson.
     *
     * <p>Note the coordinate order: {@code [lat, lng]}, matching {@code RouteResponse} and
     * what the Android client consumes. ORS itself returns {@code [lng, lat]}; the flip
     * happens in {@code RouteService}.
     */
    @Column(name = "route_geometry", columnDefinition = "json", nullable = false)
    private String routeGeometry;

    /** Road distance for this leg in metres, as reported by ORS. */
    @Column(name = "distance_meters")
    private Double distanceMeters;

    /**
     * Estimated driving duration for this leg in seconds, as reported by ORS. Used by the
     * simulator to derive a per-leg step size so relative speeds between legs look right.
     */
    @Column(name = "duration_seconds")
    private Double durationSeconds;

    public TripLegRoute() {
        // Required no-arg constructor for JPA/Hibernate
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public Integer getFromStopOrder() { return fromStopOrder; }
    public void setFromStopOrder(Integer fromStopOrder) { this.fromStopOrder = fromStopOrder; }

    public Integer getToStopOrder() { return toStopOrder; }
    public void setToStopOrder(Integer toStopOrder) { this.toStopOrder = toStopOrder; }

    public String getRouteGeometry() { return routeGeometry; }
    public void setRouteGeometry(String routeGeometry) { this.routeGeometry = routeGeometry; }

    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }

    public Double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Double durationSeconds) { this.durationSeconds = durationSeconds; }
}
