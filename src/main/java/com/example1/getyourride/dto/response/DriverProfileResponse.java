package com.example1.getyourride.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

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

    // ── NEW: feature additions (all read-only, from existing shuttle_db data) ──

    // 4. Driver stats (from the `driver` row)
    private int totalTrips;          // driver.total_trips
    private String joinDate;         // driver.join_date (ISO yyyy-MM-dd, may be null)
    private boolean verified;        // driver.is_verified
    private String accountStatus;    // driver.status ("Active", "On Break", "Deactivated")

    // 5. Ratings & reviews (aggregated from trip_review via trip_booking -> trip)
    private double averageRating;    // mean rating 1-5, 0 when no reviews yet
    private int reviewCount;         // number of ratings
    private List<String> reviewTags; // distinct tags across this driver's reviews

    // 6. Trip activity (counts from the `trip` table for this driver)
    private int completedTrips;      // status = COMPLETED
    private int cancelledTrips;      // status = CANCELLED
    private int upcomingTrips;       // status in SCHEDULED / CONFIRMED / IN_PROGRESS
    private int totalPassengers;     // confirmed bookings carried across this driver's trips

    // 7. Extra vehicle detail (already stored, previously not surfaced)
    private Integer vehicleYear;     // vehicle.vehicle_year (may be null)
    private String vehicleStatus;    // vehicle.status

    // 8. Full application detail (from `driverapplications`)
    private String applicationVehicleMakeModel; // as entered on the application
    private String driversLicenceUrl;           // Cloudinary URL, empty when not uploaded
    private String vehicleRegistrationUrl;       // Cloudinary URL, empty when not uploaded
}