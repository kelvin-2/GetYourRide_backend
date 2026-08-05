package com.example1.getyourride.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping for the 'vehicle' table in MySQL shuttle_db.
 */
@Entity
@Table(name = "vehicle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long vehicleId;

    // FK relationship mapping to Driver entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "registration_number", nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Column(name = "model")
    private String model;

    @Column(name = "vehicle_year")
    private Integer vehicleYear;

    @Column(name = "colour")
    private String colour;

    @Column(name = "capacity", nullable = false)
    private int capacity;
}