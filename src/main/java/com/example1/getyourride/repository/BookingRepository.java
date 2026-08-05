package com.example1.getyourride.repository;

import com.example1.getyourride.entity.Booking;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStudent(Student student);
    Optional<Booking> findByTripAndStudent(Trip trip, Student student);

    // Find all bookings for a specific trip (used by boarding screen)
    List<Booking> findByTrip(Trip trip);

    // Find all bookings for a list of trips (used for cascade deletion)
    List<Booking> findByTripIn(List<Trip> trips);

    // Delete all bookings for a list of trips (used for cascade deletion)
    @Modifying
    @Query("DELETE FROM Booking b WHERE b.trip IN :trips")
    void deleteByTripIn(@Param("trips") List<Trip> trips);
}
