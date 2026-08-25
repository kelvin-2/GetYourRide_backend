package com.example1.getyourride.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO returned to admin when listing/reviewing driver applications.
 * Contains all application details plus the Cloudinary URLs for uploaded documents.
 * Admin can open the document URLs directly in a browser to view the images.
 */
@Getter
@Builder
@AllArgsConstructor
public class AdminDriverApplicationResponse {

    // Application info
    private Long applicationId;
    private String applicationStatus;

    // Driver personal details
    private Long driverId;
    private String firstName;
    private String surname;
    private String studentNumber;
    private String email;
    private String contactNumber;

    // Vehicle details
    private String vehicleMakeModel;
    private String registrationNumber;
    private int seatingCapacity;
    private String vehicleColor;

    // Document URLs — these are direct Cloudinary links the admin can open to view
    private String driversLicenceUrl;
    private String vehicleRegistrationUrl;

    // Document upload status for quick reference
    private String driversLicenceStatus;
    private String vehicleRegistrationStatus;
}
