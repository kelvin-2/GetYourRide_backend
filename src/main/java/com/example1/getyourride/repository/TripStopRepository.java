package com.example1.getyourride.repository;

import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.entity.TripStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TripStopRepository extends JpaRepository<TripStop, Long> {
    List<TripStop> findByTripTripIdOrderByStopOrderAsc(Long tripId);

    /**
     * Removes stops for every given trip. The trip -> stops cascade only fires for entity-level
     * removes; the profile delete removes trips with a bulk JPQL delete that bypasses cascading,
     * and the {@code trip_id} FK is NOT NULL, so these rows must be deleted explicitly first.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    void deleteByTripIn(List<Trip> trips);
}
