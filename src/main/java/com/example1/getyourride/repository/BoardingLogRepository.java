package com.example1.getyourride.repository;

import com.example1.getyourride.entity.BoardingLog;
import com.example1.getyourride.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardingLogRepository extends JpaRepository<BoardingLog, Long> {
    Optional<BoardingLog> findByBooking(Booking booking);
}
