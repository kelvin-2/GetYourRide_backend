package com.example1.getyourride.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example1.getyourride.dto.request.DriverApplicationRequest;
import com.example1.getyourride.dto.response.AuthResponse;
import com.example1.getyourride.dto.response.DriverApplicationResponse;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.DriverApplication;
import com.example1.getyourride.entity.Vehicle;
import com.example1.getyourride.repository.DriverApplicationRepository;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.VehicleRepository;
import com.example1.getyourride.security.JwtUtil;

import org.aspectj.weaver.ast.And;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

@Service
public class DriverApplicationService {

    private final DriverApplicationRepository driverAppRepo;
    private final DriverRepository driverRepo;
    private final VehicleRepository vehicleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final Cloudinary cloudinary;

    public DriverApplicationService(
            DriverApplicationRepository driverAppRepo,
            DriverRepository driverRepo,
            VehicleRepository vehicleRepo,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            Cloudinary cloudinary
    ) {
        this.driverAppRepo = driverAppRepo;
        this.driverRepo = driverRepo;
        this.vehicleRepo = vehicleRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.cloudinary = cloudinary;
    }

    /**
     * Phase 1: Validate NMU email, create driver record, create application & vehicle.
     */
    @Transactional
    public DriverApplicationResponse submitApplication(DriverApplicationRequest request) {
        String normalizedEmail = request.getUniversityEmail() == null
                ? null
                : request.getUniversityEmail().trim().toLowerCase(Locale.ROOT);

        // 1. Validate NMU email
        if (normalizedEmail == null || !normalizedEmail.endsWith("@mandela.ac.za")) {
            throw new IllegalArgumentException("Only NMU emails (@mandela.ac.za) are accepted.");
        }

        // 2. Check if this email already exists in the driver table
        if (driverRepo.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalStateException("A driver account with this email already exists.");
        }

        // 3. Create a new driver record
        Driver driver = new Driver();
        driver.setFirstName(request.getFirstName());
        driver.setLastName(request.getSurname());
        driver.setEmail(normalizedEmail);
        driver.setPhone(request.getContactNumber());
        driver.setStudentNumber(request.getStudentNumber());
        driver.setPassword(request.getPassword());
        driver.setRole("STUDENT_DRIVER");
        driver.setIsVerified(false);
        driver.setJoinDate(LocalDate.now());
        driver.setTotalTrips(0);

        Driver savedDriver = driverRepo.save(driver);

        // 4. Create vehicle record (To keep vehicle table synced for trip postings)
        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(savedDriver);
        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setModel(request.getVehicleMakeModel());
        vehicle.setColour(request.getVehicleColor());
        vehicle.setCapacity(request.getSeatingCapacity());
        vehicleRepo.save(vehicle);

        // 5. Create driver application record
        DriverApplication app = new DriverApplication();
        app.setDriverId(savedDriver.getDriverId());
        app.setContactNumber(request.getContactNumber());
        app.setVehicleMakeModel(request.getVehicleMakeModel());
        app.setRegistrationNumber(request.getRegistrationNumber());
        app.setSeatingCapacity(request.getSeatingCapacity());
        app.setVehicleColor(request.getVehicleColor());
        app.setLicenseImagePath("");
        app.setRegistrationFilePath("");
        app.setApplicationStatus("Pending Review");

        DriverApplication savedApp = driverAppRepo.save(app);

        return new DriverApplicationResponse(
                savedApp.getApplicationId().toString(),
                "PENDING"
        );
    }

    /**
     * Phase 2: Upload document to Cloudinary and save the URL in the DB.
     */
    @Transactional
    public void uploadDocument(Long applicationId, String documentType, MultipartFile file) throws IOException {
        DriverApplication app = driverAppRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found with ID: " + applicationId));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "getyourride/driver-applications/" + applicationId,
                        "public_id", documentType,
                        "resource_type", "auto", // Handles both PDF and Image files
                        "overwrite", true
                ));

        String cloudUrl = (String) uploadResult.get("secure_url");

        if ("DriversLicence".equalsIgnoreCase(documentType)) {
            app.setLicenseImagePath(cloudUrl);
        } else if ("VehicleRegistration".equalsIgnoreCase(documentType)) {
            app.setRegistrationFilePath(cloudUrl);
        } else {
            throw new IllegalArgumentException("Unknown document type: " + documentType);
        }

        driverAppRepo.save(app);
    }

    /**
     * Phase 3: Finalize — generate JWT and return AuthResponse (auto-login).
     */
    @Transactional(readOnly = true)
    public AuthResponse finalizeApplication(Long applicationId) {
        DriverApplication app = driverAppRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found with ID: " + applicationId));

        Driver driver = driverRepo.findById(app.getDriverId())
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + app.getDriverId()));

        // Generate JWT
        String token = jwtUtil.generateToken(
                driver.getDriverId(),
                driver.getEmail(),
                "DRIVER",
                Map.of("role", "DRIVER_PENDING")
        );

        return AuthResponse.builder()
                .token(token)
                .type("DRIVER")
                .id(driver.getDriverId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail())
                .isFunded(null)
                .role("DRIVER_PENDING")
                .isVerified(false)
                .build();
    }
}