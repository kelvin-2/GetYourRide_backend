package com.example1.getyourride.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "trip_stop")
public class TripStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links this stop back to its parent trip.
    // Many stops belong to one trip — this is the "many" side.
    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    // The actual coordinates of the stop — this is what you'll
    // run your Haversine distance calculation against.
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // Friendly name shown in the UI, e.g. "Missionvale Gate 2"
    private String stopName;

    // Position of this stop in the route sequence (1st, 2nd, 3rd...)
    // Useful later when inserting a new pickup stop between existing ones.
    @Column(nullable = false)
    private Integer stopOrder;

    /**
     * Whether the vehicle has reached this stop. Driven by the simulation engine, which flips it to
     * ARRIVED as it completes the leg terminating here.
     *
     * <p>Mapped as STRING, not ORDINAL: the underlying column is
     * {@code ENUM('PENDING','ARRIVED')}, and ordinal mapping would write 0/1 into an enum column
     * and silently break the moment a value is inserted in the middle of the Java enum.
     *
     * <p>{@code columnDefinition} is declared so that a schema generated from these entities matches
     * the hand-written migration. Against an existing database Hibernate leaves the column alone.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "enum('PENDING','ARRIVED')")
    private TripStopStatus status = TripStopStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    // --- Constructors ---
    public TripStop() {
        // Required no-arg constructor for JPA/Hibernate
    }

    // --- Getters and setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }

    public Integer getStopOrder() { return stopOrder; }
    public void setStopOrder(Integer stopOrder) { this.stopOrder = stopOrder; }

    public TripStopStatus getStatus() { return status; }
    public void setStatus(TripStopStatus status) { this.status = status; }
}