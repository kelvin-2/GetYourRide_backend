package com.example1.getyourride.service;
import com.cloudinary.Cloudinary;

import com.cloudinary.utils.ObjectUtils;

import com.example1.getyourride.dto.request.DriverApplicationRequest;

import com.example1.getyourride.dto.response.AuthResponse;

import com.example1.getyourride.dto.response.DriverApplicationResponse;

import com.example1.getyourride.entity.DriverApplication;

import com.example1.getyourride.entity.Student;

import com.example1.getyourride.repository.DriverApplicationRepository;

import com.example1.getyourride.repository.StudentRepository;

import com.example1.getyourride.security.JwtUtil;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;



import java.io.IOException;

import java.util.Map;



@Service
public class DriverApplicationService {
     private final DriverApplicationRepository driverAppRepo;

    private final StudentRepository studentRepo;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final Cloudinary cloudinary;



    public DriverApplicationService(

            DriverApplicationRepository driverAppRepo,

            StudentRepository studentRepo,

            PasswordEncoder passwordEncoder,

            JwtUtil jwtUtil,

            Cloudinary cloudinary

    ) {

        this.driverAppRepo = driverAppRepo;

        this.studentRepo = studentRepo;

        this.passwordEncoder = passwordEncoder;

        this.jwtUtil = jwtUtil;

        this.cloudinary = cloudinary;

    }



    /**

     * Phase 1: Validate NMU email, register student if new, create application.

     */

    public DriverApplicationResponse submitApplication(DriverApplicationRequest request) {

        // 1. Validate NMU email

        if (!request.getUniversityEmail().toLowerCase().endsWith("@mandela.ac.za")) {

            throw new RuntimeException("Only NMU emails (@mandela.ac.za) are accepted.");

        }



        // 2. Find existing student or register a new one

        Student student = studentRepo.findByEmail(request.getUniversityEmail())

                .orElseGet(() -> {

                    Student newStudent = new Student();

                    newStudent.setFirstName(request.getFirstName());

                    newStudent.setLastName(request.getSurname());

                    newStudent.setEmail(request.getUniversityEmail());

                    newStudent.setStudentNumber(request.getStudentNumber());

                    newStudent.setPhone(request.getContactNumber());

                    newStudent.setPassword(passwordEncoder.encode(request.getPassword()));

                    newStudent.setIsFunded(false);

                    return studentRepo.save(newStudent);

                });



        // 3. Check if student already has a pending application

        driverAppRepo.findByStudentId(student.getStudentId()).ifPresent(existing -> {

            throw new RuntimeException("You already have a pending application.");

        });



        // 4. Create driver application record

        DriverApplication app = new DriverApplication();

        app.setStudentId(student.getStudentId());

        app.setContactNumber(request.getContactNumber());

        app.setVehicleMakeModel(request.getVehicleMakeModel());

        app.setRegistrationNumber(request.getRegistrationNumber());

        app.setSeatingCapacity(request.getSeatingCapacity());

        app.setVehicleColor(request.getVehicleColor());

        app.setLicenseImagePath("");           // filled in Phase 2

        app.setRegistrationFilePath("");       // filled in Phase 2

        app.setApplicationStatus("Pending Review");



        DriverApplication saved = driverAppRepo.save(app);



        return new DriverApplicationResponse(

                saved.getApplicationId().toString(),

                "PENDING"

        );

    }



    /**

     * Phase 2: Upload document to Cloudinary and save the URL in the DB.

     * The admin can access these URLs from any device.

     */

    public void uploadDocument(Long applicationId, String documentType, MultipartFile file) throws IOException {

        DriverApplication app = driverAppRepo.findById(applicationId)

                .orElseThrow(() -> new RuntimeException("Application not found."));



        // Upload to Cloudinary in a folder organized by applicationId

        @SuppressWarnings("unchecked")

        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),

                ObjectUtils.asMap(

                        "folder", "getyourride/driver-applications/" + applicationId,

                        "public_id", documentType,

                        "resource_type", "image",

                        "overwrite", true

                ));



        String cloudUrl = (String) uploadResult.get("secure_url");



        // Save the cloud URL in the correct column

        if ("DriversLicence".equalsIgnoreCase(documentType)) {

            app.setLicenseImagePath(cloudUrl);

        } else if ("VehicleRegistration".equalsIgnoreCase(documentType)) {

            app.setRegistrationFilePath(cloudUrl);

        } else {

            throw new RuntimeException("Unknown document type: " + documentType);

        }



        driverAppRepo.save(app);

    }



    /**

     * Phase 3: Finalize — verify docs uploaded, generate JWT, return AuthResponse.

     * This is the AUTO-LOGIN step. The Android app saves this token and

     * navigates directly to Driver Home without a second login.

     */

    public AuthResponse finalizeApplication(Long applicationId) {

        DriverApplication app = driverAppRepo.findById(applicationId)

                .orElseThrow(() -> new RuntimeException("Application not found."));



        // Verify both documents are uploaded

        



        // Get the student

        Student student = studentRepo.findById(app.getStudentId())

                .orElseThrow(() -> new RuntimeException("Student not found."));



        // Generate JWT — matches your existing JwtUtil signature

        String token = jwtUtil.generateToken(

                student.getStudentId(),

                student.getEmail(),

                "DRIVER",

                Map.of("role", "DRIVER_PENDING")

        );



        // Return AuthResponse — same shape the Android app expects

        return AuthResponse.builder()

                .token(token)

                .type("DRIVER")

                .id(student.getStudentId())

                .firstName(student.getFirstName())

                .lastName(student.getLastName())

                .email(student.getEmail())

                .isFunded(null)                // not relevant for drivers

                .role("DRIVER_PENDING")

                .isVerified(false)

                .build();

    }
}
