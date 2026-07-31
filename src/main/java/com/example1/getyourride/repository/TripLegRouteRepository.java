package com.example1.getyourride.repository;

import com.example1.getyourride.entity.TripLegRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Data access for precomputed per-leg route geometry.
 */
@Repository
public interface TripLegRouteRepository extends JpaRepository<TripLegRoute, Long> {

    /**
     * All legs for a trip in travel order. {@code fromStopOrder} is the ordering key, so the
     * zero-based position in this list is the leg index the simulator tracks in
     * {@code trip.current_leg_index}.
     */
    List<TripLegRoute> findByTripTripIdOrderByFromStopOrderAsc(Long tripId);

    /** Look up a single leg by the stop it departs from. */
    Optional<TripLegRoute> findByTripTripIdAndFromStopOrder(Long tripId, Integer fromStopOrder);

    long countByTripTripId(Long tripId);

    /**
     * Removes every leg for a trip so precomputation can be re-run without duplicating rows.
     *
     * <p>{@code @Modifying} plus {@code @Transactional} are required because a derived delete
     * is a write operation and Spring Data's default repository transaction is read-only.
     */
    @Modifying
    @Transactional
    void deleteByTripTripId(Long tripId);
}
