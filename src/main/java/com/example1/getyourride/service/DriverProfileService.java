package com.example1.getyourride.service;

import com.example1.getyourride.dto.request.UpdateDriverProfileRequest;
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
import com.example1.getyourride.repository.TripReviewRepository;
import com.example1.getyourride.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final TripReviewRepository tripReviewRepo;

    public DriverProfileService(
            DriverRepository driverRepo,
            DriverApplicationRepository driverAppRepo,
            VehicleRepository vehicleRepo,
            TripRepository tripRepo,
            BookingRepository bookingRepo,
            BoardingLogRepository boardingLogRepo,
            TripReviewRepository tripReviewRepo
    ) {
        this.driverRepo = driverRepo;
        this.driverAppRepo = driverAppRepo;
        this.vehicleRepo = vehicleRepo;
        this.tripRepo = tripRepo;
        this.bookingRepo = bookingRepo;
        this.boardingLogRepo = boardingLogRepo;
        this.tripReviewRepo = tripReviewRepo;
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

        Long driverId = driver.getDriverId();

        // ── Ratings & reviews aggregation (empty-safe) ──
        Double avg = tripReviewRepo.findAverageRatingByDriverId(driverId);
        double averageRating = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0; // 1dp, 0 when none
        int reviewCount = tripReviewRepo.countByDriverId(driverId);
        List<String> reviewTags = tripReviewRepo.findDistinctTagsByDriverId(driverId);
        if (reviewTags == null) {
            reviewTags = new ArrayList<>();
        }

        // ── Trip activity counts (status compared uppercase in the query) ──
        int completedTrips = tripRepo.countByDriverIdAndStatus(driverId, "COMPLETED");
        int cancelledTrips = tripRepo.countByDriverIdAndStatus(driverId, "CANCELLED");
        int upcomingTrips = tripRepo.countByDriverIdAndStatus(driverId, "SCHEDULED")
                + tripRepo.countByDriverIdAndStatus(driverId, "CONFIRMED")
                + tripRepo.countByDriverIdAndStatus(driverId, "IN_PROGRESS");
        int totalPassengers = bookingRepo.countPassengersByDriverId(driverId);

        // ── Application detail (document URLs + make/model as applied) ──
        String applicationVehicleMakeModel = app != null && app.getVehicleMakeModel() != null
                ? app.getVehicleMakeModel() : "";
        String driversLicenceUrl = app != null && app.getLicenseImagePath() != null
                ? app.getLicenseImagePath() : "";
        String vehicleRegistrationUrl = app != null && app.getRegistrationFilePath() != null
                ? app.getRegistrationFilePath() : "";

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
                // 4. Driver stats
                .totalTrips(driver.getTotalTrips())
                .joinDate(driver.getJoinDate() != null ? driver.getJoinDate().toString() : null)
                .verified(Boolean.TRUE.equals(driver.getIsVerified()))
                .accountStatus(driver.getStatus() != null ? driver.getStatus() : "Active")
                // 5. Ratings & reviews
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .reviewTags(reviewTags)
                // 6. Trip activity
                .completedTrips(completedTrips)
                .cancelledTrips(cancelledTrips)
                .upcomingTrips(upcomingTrips)
                .totalPassengers(totalPassengers)
                // 7. Extra vehicle detail
                .vehicleYear(vehicle != null ? vehicle.getVehicleYear() : null)
                .vehicleStatus(vehicle != null && vehicle.getStatus() != null ? vehicle.getStatus() : "")
                // 8. Application detail
                .applicationVehicleMakeModel(applicationVehicleMakeModel)
                .driversLicenceUrl(driversLicenceUrl)
                .vehicleRegistrationUrl(vehicleRegistrationUrl)
                .build();
    }

    /**
     * Updates the authenticated student driver's own profile.
     *
     * <p>Editable fields only: contact number and all vehicle details. Name, surname, email and
     * student number are identity fields and are never changed here.
     *
     * <p>Saving ALWAYS sends the application back for review: the driver is marked unverified
     * ({@code is_verified = false}) and the application status is reset to "Pending Review", so an
     * admin must re-approve the updated details before the driver can be treated as verified again.
     * The {@code driver.status} column (shuttle-side availability) is intentionally left untouched.
     *
     * <p>The vehicle registration document is handled separately by the existing document-upload
     * endpoint; this method only touches the structured fields.
     */
    @Transactional
    public DriverProfileResponse updateProfile(String email, UpdateDriverProfileRequest request) {
        Driver driver = driverRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Driver profile not found."));

        // 1. Personal: contact number is the only editable personal field.
        if (request.getContactNumber() != null) {
            driver.setPhone(request.getContactNumber());
        }

        // 2. Vehicle details. Guard the unique registration number so a readable error is thrown
        //    instead of a raw DB constraint violation if the plate already belongs to someone else.
        List<Vehicle> vehicles = vehicleRepo.findByDriverDriverId(driver.getDriverId());
        Vehicle vehicle = vehicles.isEmpty() ? null : vehicles.get(0);

        String newReg = request.getRegistrationNumber();
        if (newReg != null && !newReg.isBlank()) {
            vehicleRepo.findByRegistrationNumber(newReg).ifPresent(existing -> {
                boolean belongsToSomeoneElse = existing.getDriver() == null
                        || !existing.getDriver().getDriverId().equals(driver.getDriverId());
                if (belongsToSomeoneElse) {
                    throw new IllegalArgumentException(
                            "That registration number is already in use by another vehicle.");
                }
            });
        }

        if (vehicle != null) {
            if (request.getVehicleMakeModel() != null) {
                vehicle.setModel(request.getVehicleMakeModel());
            }
            if (newReg != null && !newReg.isBlank()) {
                vehicle.setRegistrationNumber(newReg);
            }
            if (request.getVehicleColor() != null) {
                vehicle.setColour(request.getVehicleColor());
            }
            if (request.getSeatingCapacity() > 0) {
                vehicle.setCapacity(request.getSeatingCapacity());
            }
            vehicle.setVehicleYear(request.getVehicleYear()); // nullable; may clear the year
            vehicleRepo.save(vehicle);
        }

        // 3. Keep the application record in sync with the edited details.
        DriverApplication app = driverAppRepo.findByDriverId(driver.getDriverId()).orElse(null);
        if (app != null) {
            if (request.getContactNumber() != null) {
                app.setContactNumber(request.getContactNumber());
            }
            if (request.getVehicleMakeModel() != null) {
                app.setVehicleMakeModel(request.getVehicleMakeModel());
            }
            if (newReg != null && !newReg.isBlank()) {
                app.setRegistrationNumber(newReg);
            }
            if (request.getSeatingCapacity() > 0) {
                app.setSeatingCapacity(request.getSeatingCapacity());
            }
            if (request.getVehicleColor() != null) {
                app.setVehicleColor(request.getVehicleColor());
            }
            // Re-review: always send back to the admin after an edit.
            app.setApplicationStatus("Pending Review");
            driverAppRepo.save(app);
        }

        // Re-review: remove the verified flag until the admin re-approves.
        driver.setIsVerified(false);
        driverRepo.save(driver);

        // Return the freshly recomputed profile (rating, trip activity and document status all
        // reflect current data via the existing getProfile mapping).
        return getProfile(email);
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