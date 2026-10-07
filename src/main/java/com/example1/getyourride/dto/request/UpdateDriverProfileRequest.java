package com.example1.getyourride.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Request body for a student driver updating their own profile.
 *
 * Only the editable fields are accepted: contact number and the vehicle details.
 * Name, surname, email and student number are NOT here — they are identity fields and
 * stay read-only. Saving any of these changes resets the driver's verification and sends
 * the application back for admin review (handled in the service).
 */
@Getter
@Setter
public class UpdateDriverProfileRequest {
    private String contactNumber;
    private String vehicleMakeModel;
    private String registrationNumber;
    private int seatingCapacity;
    private String vehicleColor;
    private Integer vehicleYear;   // optional, may be null
}
