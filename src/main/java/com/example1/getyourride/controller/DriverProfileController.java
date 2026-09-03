package com.example1.getyourride.controller;

import com.example1.getyourride.dto.response.DriverProfileDeleteResponse;
import com.example1.getyourride.dto.response.DriverProfileResponse;
import com.example1.getyourride.service.DriverApplicationService;
import com.example1.getyourride.service.DriverProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller exposing endpoints for profile retrieval, document uploads, and account deactivation.
 */
@RestController
@RequestMapping("/api/driver-profile")
public class DriverProfileController {

    private final DriverProfileService profileService;
    private final DriverApplicationService applicationService;

    public DriverProfileController(DriverProfileService profileService, DriverApplicationService applicationService) {
        this.profileService = profileService;
        this.applicationService = applicationService;
    }

    /**
     * GET /api/driver-profile
     * Fetches details for the currently authenticated driver extracted via JWT token.
     */
    @GetMapping
    public ResponseEntity<DriverProfileResponse> getProfile(Authentication authentication) {
        String email = authentication.getName();
        DriverProfileResponse response = profileService.getProfile(email);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/driver-profile/upload-document
     * Allows uploading missing documents directly from the driver profile screen.
     * Backend uses the JWT to find the driver's application and attaches the document.
     * The uploaded file goes to Cloudinary and the secure URL is stored in the DB
     * so admin can view it when reviewing the application.
     */
    @PostMapping("/upload-document")
    public ResponseEntity<?> uploadDocumentFromProfile(
            Authentication authentication,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            String email = authentication.getName();
            Long applicationId = profileService.getApplicationIdByEmail(email);
            applicationService.uploadDocument(applicationId, documentType, file);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    java.util.Map.of("message", "Failed to upload document: " + e.getMessage())
            );
        }
    }

    /**
     * DELETE /api/driver-profile
     * Deactivates/soft-deletes the active driver profile.
     */
    @DeleteMapping
    public ResponseEntity<DriverProfileDeleteResponse> deleteProfile(Authentication authentication) {
        String email = authentication.getName();
        DriverProfileDeleteResponse response = profileService.deactivateProfile(email);
        return ResponseEntity.ok(response);
    }
}