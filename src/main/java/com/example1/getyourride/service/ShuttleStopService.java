package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.ShuttleStopResponse;
import com.example1.getyourride.dto.response.ShuttleTimeSlotResponse;

import java.util.List;

public interface ShuttleStopService {
    List<ShuttleStopResponse> getAllStops();
    List<ShuttleTimeSlotResponse> getAllTimeSlots();
}