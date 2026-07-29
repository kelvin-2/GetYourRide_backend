package com.example1.getyourride.controller;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.*;



@Entity

@Table(name = "driverapplications")
public class DriverApplicationController {
    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "ApplicationID")

    private Long applicationId;



    @Column(name = "UserID", nullable = false)

    private Long userId;



    @Column(name = "ContactNumber", nullable = false)

    private String contactNumber;



    @Column(name = "VehicleMakeModel", nullable = false)

    private String vehicleMakeModel;



    @Column(name = "RegistrationNumber", nullable = false)

    private String registrationNumber;



    @Column(name = "SeatingCapacity", nullable = false)

    private int seatingCapacity;



    @Column(name = "VehicleColor", nullable = false)

    private String vehicleColor;



    @Column(name = "LicenseImagePath", nullable = false)

    private String licenseImagePath;



    @Column(name = "RegistrationFilePath", nullable = false)

    private String registrationFilePath;



    @Column(name = "ApplicationStatus")

    private String applicationStatus = "Pending Review";
     // Getters and Setters

    public Long getApplicationId() { return applicationId; }

    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }



    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }



    public String getContactNumber() { return contactNumber; }

    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }



    public String getVehicleMakeModel() { return vehicleMakeModel; }

    public void setVehicleMakeModel(String vehicleMakeModel) { this.vehicleMakeModel = vehicleMakeModel; }



    public String getRegistrationNumber() { return registrationNumber; }

    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }



    public int getSeatingCapacity() { return seatingCapacity; }

    public void setSeatingCapacity(int seatingCapacity) { this.seatingCapacity = seatingCapacity; }



    public String getVehicleColor() { return vehicleColor; }

    public void setVehicleColor(String vehicleColor) { this.vehicleColor = vehicleColor; }



    public String getLicenseImagePath() { return licenseImagePath; }

    public void setLicenseImagePath(String licenseImagePath) { this.licenseImagePath = licenseImagePath; }



    public String getRegistrationFilePath() { return registrationFilePath; }

    public void setRegistrationFilePath(String registrationFilePath) { this.registrationFilePath = registrationFilePath; }



    public String getApplicationStatus() { return applicationStatus; }

    public void setApplicationStatus(String applicationStatus) { this.applicationStatus = applicationStatus; }
}
