package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.AdminDriverApplicationResponse;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.DriverApplication;
import com.example1.getyourride.repository.DriverApplicationRepository;
import com.example1.getyourride.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for admin operations on driver applications.
 * Provides methods to list, review, and approve/reject applications.
 * Document URLs stored in the database are direct Cloudinary links —
 * admin can open them in any browser to view the uploaded images.
 */
@Service
public class AdminDriverApplicationService {

    private final DriverApplicationRepository driverAppRepo;
    private final DriverRepository driverRepo;

    public AdminDriverApplicationService(
            DriverApplicationRepository driverAppRepo,
            DriverRepository driverRepo
    ) {
        this.driverAppRepo = driverAppRepo;
        this.driverRepo = driverRepo;
    }

    /**
     * Get all driver applications for admin review.
     * Each response includes the Cloudinary document URLs so admin can view them directly.
     */
    @Transactional(readOnly = true)
    public List<AdminDriverApplicationResponse> getAllApplications() {
        List<DriverApplication> applications = driverAppRepo.findAll();
        return applications.stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get applications filtered by status (e.g. "Pending Review", "Approved", "Rejected").
     */
    @Transactional(readOnly = true)
    public List<AdminDriverApplicationResponse> getApplicationsByStatus(String status) {
        List<DriverApplication> applications = driverAppRepo.findByApplicationStatus(status);
        return applications.stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single application by ID with full details and document URLs.
     */
    @Transactional(readOnly = true)
    public AdminDriverApplicationResponse getApplicationById(Long applicationId) {
        DriverApplication app = driverAppRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found with ID: " + applicationId));
        return toAdminResponse(app);
    }

    /**
     * Approve a driver application — updates status and marks the driver as verified.
     */
    @Transactional
    public AdminDriverApplicationResponse approveApplication(Long applicationId) {
        DriverApplication app = driverAppRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found with ID: " + applicationId));

        app.setApplicationStatus("Approved");
        driverAppRepo.save(app);

        // Also mark the driver as verified so they can offer rides
        Driver driver = driverRepo.findById(app.getDriverId()).orElse(null);
        if (driver != null) {
            driver.setIsVerified(true);
            driver.setRole("STUDENT_DRIVER");
            driverRepo.save(driver);
        }

        return toAdminResponse(app);
    }

    /**
     * Reject a driver application.
     */
    @Transactional
    public AdminDriverApplicationResponse rejectApplication(Long applicationId, String reason) {
        DriverApplication app = driverAppRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found with ID: " + applicationId));

        app.setApplicationStatus("Rejected");
        driverAppRepo.save(app);

        return toAdminResponse(app);
    }

    /**
     * Maps a DriverApplication entity to the admin response DTO.
     * Includes the Cloudinary URLs — these are direct links that render
     * the uploaded image when opened in a browser.
     */
    private AdminDriverApplicationResponse toAdminResponse(DriverApplication app) {
        // Fetch the associated driver for personal details
        Driver driver = driverRepo.findById(app.getDriverId()).orElse(null);

        String firstName = driver != null ? driver.getFirstName() : "Unknown";
        String surname = driver != null ? driver.getLastName() : "Unknown";
        String studentNumber = driver != null ? driver.getStudentNumber() : "";
        String email = driver != null ? driver.getEmail() : "";

        // Determine document upload status from the stored URLs
        String licenceStatus = (app.getLicenseImagePath() != null && !app.getLicenseImagePath().isBlank())
                ? "Uploaded" : "Not Uploaded";
        String registrationStatus = (app.getRegistrationFilePath() != null && !app.getRegistrationFilePath().isBlank())
                ? "Uploaded" : "Not Uploaded";

        return AdminDriverApplicationResponse.builder()
                .applicationId(app.getApplicationId())
                .applicationStatus(app.getApplicationStatus())
                .driverId(app.getDriverId())
                .firstName(firstName)
                .surname(surname)
                .studentNumber(studentNumber)
                .email(email)
                .contactNumber(app.getContactNumber())
                .vehicleMakeModel(app.getVehicleMakeModel())
                .registrationNumber(app.getRegistrationNumber())
                .seatingCapacity(app.getSeatingCapacity())
                .vehicleColor(app.getVehicleColor())
                .driversLicenceUrl(app.getLicenseImagePath())
                .vehicleRegistrationUrl(app.getRegistrationFilePath())
                .driversLicenceStatus(licenceStatus)
                .vehicleRegistrationStatus(registrationStatus)
                .build();
    }
}
