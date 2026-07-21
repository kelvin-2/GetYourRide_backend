package com.example1.getyourride.repository;

import com.example1.getyourride.entity.ShuttleTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShuttleTimeSlotRepository extends JpaRepository<ShuttleTimeSlot, Long> {
    // findAll() is enough here too - only 8 rows total, no pagination needed
}