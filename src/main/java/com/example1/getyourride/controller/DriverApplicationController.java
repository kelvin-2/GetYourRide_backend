package com.example1.getyourride.controller;

import com.example1.getyourride.dto.response.AuthResponse;

import com.example1.getyourride.dto.request.DriverApplicationRequest;

import com.example1.getyourride.dto.response.DriverApplicationResponse;

import com.example1.getyourride.service.DriverApplicationService;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;



@RestController

@RequestMapping("/api/driver-applications")
public class DriverApplicationController {
    private final DriverApplicationService service;



    public DriverApplicationController(DriverApplicationService service) {

        this.service = service;

    }



    /**

     * Phase 1: Submit application (personal + vehicle info).

     * POST /api/driver-applications

     */

    @PostMapping

    public ResponseEntity<DriverApplicationResponse> submitApplication(

            @RequestBody DriverApplicationRequest request

    ) {

        DriverApplicationResponse response = service.submitApplication(request);

        return ResponseEntity.ok(response);

    }



    /**

     * Phase 2: Upload a document (driver's licence or vehicle registration).

     * POST /api/driver-applications/{applicationId}/documents

     */

    @PostMapping("/{applicationId}/documents")

    public ResponseEntity<Void> uploadDocument(

            @PathVariable Long applicationId,

            @RequestParam("documentType") String documentType,

            @RequestParam("file") MultipartFile file

    ) {

        try {

            service.uploadDocument(applicationId, documentType, file);

            return ResponseEntity.ok().build();

        } catch (Exception e) {

            return ResponseEntity.badRequest().build();

        }

    }



    /**

     * Phase 3: Finalize — auto-login, returns JWT + student info + role=DRIVER_PENDING.

     * POST /api/driver-applications/{applicationId}/finalize

     */

    @PostMapping("/{applicationId}/finalize")

    public ResponseEntity<AuthResponse> finalizeApplication(

            @PathVariable Long applicationId

    ) {

        AuthResponse response = service.finalizeApplication(applicationId);

        return ResponseEntity.ok(response);

    }
}
