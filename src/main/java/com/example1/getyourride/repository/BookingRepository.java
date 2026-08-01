package com.example1.getyourride.repository;

import com.example1.getyourride.entity.Booking;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStudent(Student student);
    Optional<Booking> findByTripAndStudent(Trip trip, Student student);
}
