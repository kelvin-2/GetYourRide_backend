package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.response.ShuttleStopResponse;
import com.example1.getyourride.dto.response.ShuttleTimeSlotResponse;
import com.example1.getyourride.entity.ShuttleStop;
import com.example1.getyourride.entity.ShuttleTimeSlot;
import com.example1.getyourride.repository.ShuttleStopRepository;
import com.example1.getyourride.repository.ShuttleTimeSlotRepository;
import com.example1.getyourride.service.ShuttleStopService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShuttleStopServiceImpl implements ShuttleStopService {

    private final ShuttleStopRepository shuttleStopRepository;
    private final ShuttleTimeSlotRepository shuttleTimeSlotRepository;

    // constructor injection - no @Autowired needed on a single constructor
    public ShuttleStopServiceImpl(ShuttleStopRepository shuttleStopRepository,
                                  ShuttleTimeSlotRepository shuttleTimeSlotRepository) {
        this.shuttleStopRepository = shuttleStopRepository;
        this.shuttleTimeSlotRepository = shuttleTimeSlotRepository;
    }

    @Override
    public List<ShuttleStopResponse> getAllStops() {
        return shuttleStopRepository.findAll()
                .stream()
                .map(this::toStopResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShuttleTimeSlotResponse> getAllTimeSlots() {
        return shuttleTimeSlotRepository.findAll()
                .stream()
                .map(this::toTimeSlotResponse)
                .collect(Collectors.toList());
    }

    // ---- mapping helpers: entity -> DTO ----

    private ShuttleStopResponse toStopResponse(ShuttleStop stop) {
        return new ShuttleStopResponse(
                stop.getStopId(),
                stop.getStopName(),
                stop.getArea(),
                stop.getLocation(),
                stop.getLatitude(),
                stop.getLongitude()
        );
    }

    private ShuttleTimeSlotResponse toTimeSlotResponse(ShuttleTimeSlot slot) {
        return new ShuttleTimeSlotResponse(
                slot.getSlotId(),
                slot.getPeriod().name(), // enum -> "Morning" / "Afternoon" string
                slot.getDeparts(),
                slot.getArrives()
        );
    }
}