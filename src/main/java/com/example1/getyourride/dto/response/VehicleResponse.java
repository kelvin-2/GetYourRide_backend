package com.example1.getyourride.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {
    private Long vehicleId;
    private String registrationNumber;
    private String model;
    private Integer vehicleYear;
    private String colour;
    private Integer capacity;
    private Long driverId;
}
