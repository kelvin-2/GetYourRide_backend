package com.example1.getyourride.dto.request;
import lombok.Getter;

import lombok.Setter;



@Getter

@Setter

public class DriverApplicationRequest {
     private String firstName;

    private String surname;

    private String studentNumber;

    private String contactNumber;

    private String universityEmail;

    private String password;

    private String vehicleMakeModel;

    private String registrationNumber;

    private int seatingCapacity;

    private String vehicleColor;
}
