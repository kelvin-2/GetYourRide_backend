package com.example1.getyourride.controller;

import com.example1.getyourride.dto.response.AdminDriverApplicationResponse;
import com.example1.getyourride.service.AdminDriverApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for reviewing and managing driver applications.
 *
 * Document URLs in the responses are direct Cloudinary HTTPS links.
 * Admin can open driversLicenceUrl or vehicleRegistrationUrl in any browser
 * and the uploaded image will render immediately — no extra auth needed
 * because Cloudinary serves them publicly via their CDN.
 *
 * All endpoints are gated behind ADMIN/STAFF role via SecurityConfig
 * (.requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "STAFF"))
 */
@RestController
@RequestMapping("/api/admin/driver-applications")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminDriverApplicationController {

    private final AdminDriverApplicationService service;

    public AdminDriverApplicationController(AdminDriverApplicationService service) {
        this.service = service;
    }

    /**
     * GET /api/admin/driver-applications
     * List all driver applications with their document URLs.
     *
     * Optional query param: ?status=Pending Review (or Approved, Rejected)
     */
    @GetMapping
    public ResponseEntity<List<AdminDriverApplicationResponse>> getAllApplications(
            @RequestParam(value = "status", required = false) String status
    ) {
        List<AdminDriverApplicationResponse> applications;
        if (status != null && !status.isBlank()) {
            applications = service.getApplicationsByStatus(status);
        } else {
            applications = service.getAllApplications();
        }
        return ResponseEntity.ok(applications);
    }

    /**
     * GET /api/admin/driver-applications/{applicationId}
     * Get a single application with full details and document URLs.
     *
     * The driversLicenceUrl and vehicleRegistrationUrl fields in the response
     * are direct links — admin clicks them and the document opens in the browser.
     */
    @GetMapping("/{applicationId}")
    public ResponseEntity<?> getApplicationById(@PathVariable Long applicationId) {
        try {
            AdminDriverApplicationResponse response = service.getApplicationById(applicationId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * GET /api/admin/driver-applications/{applicationId}/documents/licence
     * Redirects admin directly to the Cloudinary URL for the driver's licence image.
     * Opens the image in the browser.
     */
    @GetMapping("/{applicationId}/documents/licence")
    public ResponseEntity<?> viewDriversLicence(@PathVariable Long applicationId) {
        try {
            AdminDriverApplicationResponse app = service.getApplicationById(applicationId);
            String url = app.getDriversLicenceUrl();
            if (url == null || url.isBlank()) {
                return ResponseEntity.notFound().build();
            }
            // Redirect to Cloudinary URL — browser will display the image
            return ResponseEntity.status(302)
                    .header("Location", url)
                    .build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * GET /api/admin/driver-applications/{applicationId}/documents/registration
     * Redirects admin directly to the Cloudinary URL for the vehicle registration image.
     * Opens the image in the browser.
     */
    @GetMapping("/{applicationId}/documents/registration")
    public ResponseEntity<?> viewVehicleRegistration(@PathVariable Long applicationId) {
        try {
            AdminDriverApplicationResponse app = service.getApplicationById(applicationId);
            String url = app.getVehicleRegistrationUrl();
            if (url == null || url.isBlank()) {
                return ResponseEntity.notFound().build();
            }
            // Redirect to Cloudinary URL — browser will display the image
            return ResponseEntity.status(302)
                    .header("Location", url)
                    .build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * PUT /api/admin/driver-applications/{applicationId}/approve
     * Approve a driver application. This also marks the driver as verified.
     */
    @PutMapping("/{applicationId}/approve")
    public ResponseEntity<?> approveApplication(@PathVariable Long applicationId) {
        try {
            AdminDriverApplicationResponse response = service.approveApplication(applicationId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * PUT /api/admin/driver-applications/{applicationId}/reject
     * Reject a driver application.
     */
    @PutMapping("/{applicationId}/reject")
    public ResponseEntity<?> rejectApplication(
            @PathVariable Long applicationId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        try {
            String reason = (body != null) ? body.getOrDefault("reason", "") : "";
            AdminDriverApplicationResponse response = service.rejectApplication(applicationId, reason);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
