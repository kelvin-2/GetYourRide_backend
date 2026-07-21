package com.example1.getyourride.controller;

import com.example1.getyourride.dto.response.ShuttleStopResponse;
import com.example1.getyourride.dto.response.ShuttleTimeSlotResponse;
import com.example1.getyourride.service.ShuttleStopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shuttle-stops")
public class ShuttleStopController {

    private final ShuttleStopService shuttleStopService;

    public ShuttleStopController(ShuttleStopService shuttleStopService) {
        this.shuttleStopService = shuttleStopService;
    }

    // GET /api/shuttle-stops - all pickup points, for populating a dropdown/list
    @GetMapping
    public List<ShuttleStopResponse> getAllStops() {
        return shuttleStopService.getAllStops();
    }

    // GET /api/shuttle-stops/time-slots - all departure/arrival slots
    @GetMapping("/time-slots")
    public List<ShuttleTimeSlotResponse> getAllTimeSlots() {
        return shuttleStopService.getAllTimeSlots();
    }
}