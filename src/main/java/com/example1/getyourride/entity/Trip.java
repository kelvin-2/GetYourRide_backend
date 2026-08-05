package com.example1.getyourride.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_number", referencedColumnName = "registration_number", nullable = false,
            columnDefinition = "varchar(20)")
    private Vehicle vehicle;

    @Column(name = "trip_type", nullable = false)
    private String tripType;

    @Column(name = "departure_stop", nullable = false)
    private String departureStop;

    @Column(name = "departure_lat")
    private Double departureLat;

    @Column(name = "departure_lng")
    private Double departureLng;

    @Column(name = "destination_stop", nullable = false)
    private String destinationStop;

    @Column(name = "destination_lat")
    private Double destinationLat;

    @Column(name = "destination_lng")
    private Double destinationLng;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(name = "price", precision = 8, scale = 2)
    private BigDecimal price;

    @Column(name = "status", nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private ShuttleTimeSlot timeSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private ShuttleRoute route;

    // --- Live tracking state -------------------------------------------------
    // These columns were added by the Phase 0 migration but stayed unmapped until
    // Phase 4 needed them. Together they are the simulation engine's resume point:
    // every tick reads them, advances a step, and writes them back, so a restart
    // picks up where it left off rather than teleporting the vehicle.

    /** Vehicle's most recently published latitude. Null until the trip starts. */
    @Column(name = "current_lat")
    private Double currentLat;

    /** Vehicle's most recently published longitude. Null until the trip starts. */
    @Column(name = "current_lng")
    private Double currentLng;

    /**
     * Zero-based index into this trip's {@code trip_leg_route} rows ordered by
     * {@code from_stop_order} — which leg the vehicle is currently driving.
     */
    @Column(name = "current_leg_index")
    private Integer currentLegIndex;

    /** Zero-based index into the current leg's polyline — how far along that leg the vehicle is. */
    @Column(name = "current_point_index")
    private Integer currentPointIndex;

    /**
     * While set and in the future, the vehicle is parked at a stop simulating boarding time and
     * ticks are skipped. Cleared once the dwell expires. Null means "moving".
     */
    @Column(name = "dwell_until")
    private LocalDateTime dwellUntil;

    // --- One-to-many side: a Trip can have many TripStops ---
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<TripStop> stops = new ArrayList<>();

    // Convenience methods to keep both sides of the relationship in sync
    public void addStop(TripStop stop) {
        stops.add(stop);
        stop.setTrip(this);
    }

    public void removeStop(TripStop stop) {
        stops.remove(stop);
        stop.setTrip(null);
    }

}