package com.example1.getyourride.service;

import com.example1.getyourride.dto.request.VehicleRequest;
import com.example1.getyourride.dto.response.VehicleResponse;
import java.util.List;

public interface VehicleService {
    VehicleResponse registerVehicle(VehicleRequest request);
    List<VehicleResponse> getMyVehicles();
    List<VehicleResponse> getAllVehicles();
}
