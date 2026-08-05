package com.example1.getyourride.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShuttleVehicleResponse {
    private Long vehicleId;
    private String registrationNumber;
    private String model;
    private Integer vehicleYear;
    private String colour;
    private Integer capacity;
}
