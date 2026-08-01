package com.example1.getyourride.repository;

import com.example1.getyourride.entity.ShuttleRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShuttleRouteRepository extends JpaRepository<ShuttleRoute, Long> {
}
