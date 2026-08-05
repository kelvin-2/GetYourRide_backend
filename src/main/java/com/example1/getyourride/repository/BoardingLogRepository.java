package com.example1.getyourride.repository;

import com.example1.getyourride.entity.BoardingLog;
import com.example1.getyourride.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardingLogRepository extends JpaRepository<BoardingLog, Long> {
    Optional<BoardingLog> findByBooking(Booking booking);

    // Find all boarding logs for a list of bookings (used for cascade deletion)
    List<BoardingLog> findByBookingIn(List<Booking> bookings);

    // Delete all boarding logs for a list of bookings (used for cascade deletion)
    @Modifying
    @Query("DELETE FROM BoardingLog bl WHERE bl.booking IN :bookings")
    void deleteByBookingIn(@Param("bookings") List<Booking> bookings);
}
