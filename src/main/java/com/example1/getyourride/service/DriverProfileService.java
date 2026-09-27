package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.DriverProfileDeleteResponse;
import com.example1.getyourride.dto.response.DriverProfileResponse;
import com.example1.getyourride.entity.Booking;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.DriverApplication;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.entity.Vehicle;
import com.example1.getyourride.repository.BoardingLogRepository;
import com.example1.getyourride.repository.BookingRepository;
import com.example1.getyourride.repository.DriverApplicationRepository;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing profile retrieval and profile deactivation.
 */
@Service
public class DriverProfileService {

    private final DriverRepository driverRepo;
    private final DriverApplicationRepository driverAppRepo;
    private final VehicleRepository vehicleRepo;
    private final TripRepository tripRepo;
    private final BookingRepository bookingRepo;
    private final BoardingLogRepository boardingLogRepo;

    public DriverProfileService(
            DriverRepository driverRepo,
            DriverApplicationRepository driverAppRepo,
            VehicleRepository vehicleRepo,
            TripRepository tripRepo,
            BookingRepository bookingRepo,
            BoardingLogRepository boardingLogRepo
    ) {
        this.driverRepo = driverRepo;
        this.driverAppRepo = driverAppRepo;
        this.vehicleRepo = vehicleRepo;
        this.tripRepo = tripRepo;
        this.bookingRepo = bookingRepo;
        this.boardingLogRepo = boardingLogRepo;
    }

    /**
     * Retrieves unified profile details using the authenticated user's email.
     */
    @Transactional(readOnly = true)
    public DriverProfileResponse getProfile(String email) {
        Driver driver = driverRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Driver profile not found."));

        // Fetch application state
        DriverApplication app = driverAppRepo.findByDriverId(driver.getDriverId())
                .orElse(null);

        // Fetch vehicle record
        List<Vehicle> vehicles = vehicleRepo.findByDriverDriverId(driver.getDriverId());
        Vehicle vehicle = vehicles.isEmpty() ? null : vehicles.get(0);

        // Evaluate document status
        String licenceStatus = "Not Uploaded";
        String registrationStatus = "Not Uploaded";
        String applicationStatus = "Pending Review";

        if (app != null) {
            applicationStatus = app.getApplicationStatus();
            if (app.getLicenseImagePath() != null && !app.getLicenseImagePath().isBlank()) {
                licenceStatus = "Uploaded";
            }
            if (app.getRegistrationFilePath() != null && !app.getRegistrationFilePath().isBlank()) {
                registrationStatus = "Uploaded";
            }
        }

        // Separate Make and Model
        String vehicleMake = "";
        String vehicleModel = "";
        if (vehicle != null && vehicle.getModel() != null) {
            String[] parts = vehicle.getModel().split(" ", 2);
            vehicleMake = parts.length > 0 ? parts[0] : "";
            vehicleModel = parts.length > 1 ? parts[1] : "";
        }

        return DriverProfileResponse.builder()
                .firstName(driver.getFirstName())
                .surname(driver.getLastName())
                .studentNumber(driver.getEmail() != null ? driver.getEmail().split("@")[0] : "") // Extracts student number prefix
                .email(driver.getEmail())
                .contactNumber(driver.getPhone() != null ? driver.getPhone() : "")
                .studentNumber(driver.getStudentNumber() != null ? driver.getStudentNumber() : "")
                .vehicleMake(vehicleMake)
                .vehicleModel(vehicleModel)
                .registrationNumber(vehicle != null ? vehicle.getRegistrationNumber() : "")
                .vehicleColour(vehicle != null && vehicle.getColour() != null ? vehicle.getColour() : "")
                .seatingCapacity(vehicle != null ? vehicle.getCapacity() : 0)
                .applicationStatus(applicationStatus)
                .driversLicenceStatus(licenceStatus)
                .vehicleRegistrationStatus(registrationStatus)
                .build();
    }

    /**
     * Permanently deletes the driver profile and everything connected to it.
     *
     * <p>This is a HARD delete — irreversible. It removes, in dependency order:
     * <ol>
     *   <li>boarding logs for the driver's bookings</li>
     *   <li>bookings on the driver's trips (their reviews cascade via fk_review_booking)</li>
     *   <li>the driver's trips (trip_stops, leg routes and location history cascade via their
     *       ON DELETE CASCADE / JPA CascadeType.ALL)</li>
     *   <li>the driver's vehicles</li>
     *   <li>the driver record (the driver application row cascades via fk_application_driver)</li>
     * </ol>
     *
     * <p>Order matters: fk_trip_driver, fk_trip_vehicle and fk_vehicle_driver do NOT cascade,
     * so trips and vehicles must be removed before the driver, or the delete is rejected.
     * Mirrors the proven ShuttleDriverService.deleteProfile flow.
     *
     * <p>Note: uploaded document images live on Cloudinary, not in this database, so they are
     * not removed here.
     */
    @Transactional
    public DriverProfileDeleteResponse deactivateProfile(String email) {
        Driver driver = driverRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found."));

        Long driverId = driver.getDriverId();

        // 1. All trips this driver posted.
        List<Trip> driverTrips = tripRepo.findByDriverDriverId(driverId);

        if (!driverTrips.isEmpty()) {
            // 2. All bookings on those trips.
            List<Booking> tripBookings = bookingRepo.findByTripIn(driverTrips);

            if (!tripBookings.isEmpty()) {
                // 3. Boarding logs for those bookings (no DB cascade from booking side here).
                boardingLogRepo.deleteByBookingIn(tripBookings);

                // 4. The bookings themselves (trip_review cascades via fk_review_booking).
                bookingRepo.deleteByTripIn(driverTrips);
            }

            // 5. The trips (trip_stops / leg routes / location history cascade off the trip).
            tripRepo.deleteByDriverId(driverId);
        }

        // 6. Vehicles assigned to this driver.
        vehicleRepo.deleteByDriver(driver);

        // 7. The driver record (driverapplications cascades via fk_application_driver).
        driverRepo.delete(driver);

        return new DriverProfileDeleteResponse("Driver profile and all associated data deleted successfully.");
    }
    /**
 * Resolves the application ID associated with a driver's email.
 */
@Transactional(readOnly = true)
public Long getApplicationIdByEmail(String email) {
    Driver driver = driverRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Driver not found."));

    DriverApplication app = driverAppRepo.findByDriverId(driver.getDriverId())
            .orElseThrow(() -> new IllegalArgumentException("No application record found for driver."));

    return app.getApplicationId();
}
}