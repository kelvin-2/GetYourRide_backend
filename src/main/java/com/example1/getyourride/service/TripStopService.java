package com.example1.getyourride.service;

import com.example1.getyourride.dto.request.TripStopRequest;
import com.example1.getyourride.dto.response.TripStopResponse;

import java.util.List;

public interface TripStopService {
    TripStopResponse addStopToTrip(Long tripId, TripStopRequest request);
    TripStopResponse addStudentStopToTrip(Long tripId, TripStopRequest request);
    List<TripStopResponse> getStopsByTrip(Long tripId);
    void removeStop(Long stopId);
}
