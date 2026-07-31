package com.example1.getyourride.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO containing structured driver profile and vehicle details.
 */
@Getter
@Builder
@AllArgsConstructor
public class DriverProfileResponse {

    // 1. Personal Details
    private String firstName;
    private String surname;
    private String studentNumber;
    private String email;
    private String contactNumber;

    // 2. Vehicle Details
    private String vehicleMake;
    private String vehicleModel;
    private String registrationNumber;
    private String vehicleColour;
    private int seatingCapacity;

    // 3. Document & Application Status
    private String applicationStatus;
    private String driversLicenceStatus;
    private String vehicleRegistrationStatus;
}