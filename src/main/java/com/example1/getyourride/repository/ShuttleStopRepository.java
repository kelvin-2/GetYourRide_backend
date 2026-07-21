package com.example1.getyourride.repository;

import com.example1.getyourride.entity.ShuttleStop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShuttleStopRepository extends JpaRepository<ShuttleStop, Long> {
    // findAll() from JpaRepository is enough for now - no custom queries needed yet
}