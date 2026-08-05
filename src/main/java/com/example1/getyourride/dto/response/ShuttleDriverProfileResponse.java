package com.example1.getyourride.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShuttleDriverProfileResponse {
    private Long driverId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private String joinDate;
    private Integer totalTrips;
    private Boolean isVerified;
    private ShuttleVehicleResponse vehicle;
    private TripSummaryResponse tripSummary;
}
