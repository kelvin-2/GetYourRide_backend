package com.example1.getyourride.entity;

import jakarta.persistence.*;

import lombok.Getter;

import lombok.Setter;

import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;



@Entity

@Table(name = "driverapplications")

@Getter

@Setter

@NoArgsConstructor

@AllArgsConstructor
public class DriverApplication {
    @Id

@GeneratedValue(strategy = GenerationType.IDENTITY)

@Column(name = "ApplicationID")

private Long applicationId;



@Column(name = "student_id", nullable = false)

private Long studentId;



@Column(name = "contact_number", nullable = false)

private String contactNumber;



@Column(name = "vehicle_make_model", nullable = false)

private String vehicleMakeModel;



@Column(name = "registration_number", nullable = false)

private String registrationNumber;



@Column(name = "seating_capacity", nullable = false)

private int seatingCapacity;



@Column(name = "vehicle_color", nullable = false)

private String vehicleColor;



@Column(name = "license_image_path", nullable = false)

private String licenseImagePath = "";



@Column(name = "registration_file_path", nullable = false)

private String registrationFilePath = "";



@Column(name = "application_status")

private String applicationStatus = "Pending Review";
}
