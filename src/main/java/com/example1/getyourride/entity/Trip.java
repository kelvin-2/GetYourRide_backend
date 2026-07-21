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
    @JoinColumn(name = "registration_number", referencedColumnName = "registration_number", nullable = false)
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