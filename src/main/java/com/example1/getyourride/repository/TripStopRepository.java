package com.example1.getyourride.repository;

import com.example1.getyourride.entity.TripStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripStopRepository extends JpaRepository<TripStop, Long> {
    List<TripStop> findByTripTripIdOrderByStopOrderAsc(Long tripId);
}
