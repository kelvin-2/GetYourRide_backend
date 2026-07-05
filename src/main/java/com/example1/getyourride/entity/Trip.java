package com.example1.getyourride.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

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

    // fk_trip_driver
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    // fk_trip_vehicle
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_number", referencedColumnName = "registration_number", nullable = false)
    private Vehicle vehicle;

    @Column(name = "trip_type", nullable = false)
    private String tripType; // e.g. "SHUTTLE" or "STUDENT_DRIVER"

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
    private String status; // e.g. "SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
}