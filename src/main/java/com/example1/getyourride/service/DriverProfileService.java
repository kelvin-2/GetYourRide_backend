package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.DriverProfileDeleteResponse;
import com.example1.getyourride.dto.response.DriverProfileResponse;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.DriverApplication;
import com.example1.getyourride.entity.Vehicle;
import com.example1.getyourride.repository.DriverApplicationRepository;
import com.example1.getyourride.repository.DriverRepository;
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

    public DriverProfileService(
            DriverRepository driverRepo,
            DriverApplicationRepository driverAppRepo,
            VehicleRepository vehicleRepo
    ) {
        this.driverRepo = driverRepo;
        this.driverAppRepo = driverAppRepo;
        this.vehicleRepo = vehicleRepo;
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
     * Soft-deletes and deactivates the active driver profile.
     */
    @Transactional
    public DriverProfileDeleteResponse deactivateProfile(String email) {
        Driver driver = driverRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found."));

        // Deactivate driver role & verification
        driver.setIsVerified(false);
        driver.setRole("DEACTIVATED");
        driverRepo.save(driver);

        // Deactivate application entry
        driverAppRepo.findByDriverId(driver.getDriverId()).ifPresent(app -> {
            app.setApplicationStatus("Deactivated");
            driverAppRepo.save(app);
        });

        return new DriverProfileDeleteResponse("Driver profile deactivated successfully.");
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