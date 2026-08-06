package com.example1.getyourride.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example1.getyourride.entity.Booking;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.entity.Trip;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * CHANGED: Added @EntityGraph so that when getMyBookings calls this, the trip's driver,
     * vehicle, and stops are loaded in one query instead of lazy-loading. Without this the
     * serializer would either hit a LazyInitializationException (if the session is closed)
     * or fire N+1 queries per booking, and the trip details would come back null/empty in
     * the JSON response.
     */
    @EntityGraph(attributePaths = {"trip", "trip.driver", "trip.vehicle", "trip.stops"})
    List<Booking> findByStudent(Student student);

    @EntityGraph(attributePaths = {"trip", "trip.driver", "trip.vehicle"})
    Optional<Booking> findByTripAndStudent(Trip trip, Student student);

    // Find all bookings for a specific trip (used by boarding screen)
    List<Booking> findByTrip(Trip trip);
}
