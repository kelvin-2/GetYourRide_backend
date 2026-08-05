# Driver Application Review — Backend Build Guide

## Overview

This document provides step-by-step instructions for building the **admin review endpoints**
in the Spring Boot backend that handle approving and rejecting student driver applications.

An external admin website (`review-application.html`) calls these endpoints to manage
driver applications. This guide explains the database relationship, the expected behavior,
and exactly what code needs to be added.

**Important Rules:**
- DO NOT remove or overwrite existing code
- CHECK if a class/method already exists before creating it
- ADD new methods to existing services/controllers rather than replacing them
- Keep existing endpoints working — this is additive work only

---

## Database Relationship: `driver` ↔ `driverapplications`

```
┌─────────────────────────────┐         ┌─────────────────────────────────┐
│         driver              │         │      driverapplications         │
├─────────────────────────────┤         ├─────────────────────────────────┤
│ driver_id (PK)              │◄────────│ driver_id (FK, CASCADE DELETE)  │
│ first_name                  │         │ ApplicationID (PK)              │
│ last_name                   │         │ contact_number                  │
│ email (UNIQUE)              │         │ vehicle_make_model              │
│ phone                       │         │ registration_number             │
│ student_number              │         │ seating_capacity                │
│ role                        │         │ vehicle_color                   │
│ is_verified  ◄── KEY FIELD  │         │ license_image_path              │
│ join_date                   │         │ registration_file_path          │
│ password                    │         │ application_status ◄── KEY FIELD│
│ total_trips                 │         └─────────────────────────────────┘
└─────────────────────────────┘
```

### How they connect:
- When a student submits a driver application, a row is created in **both** tables.
- `driverapplications.driver_id` is a FK referencing `driver.driver_id` with `ON DELETE CASCADE`.
- This means: if the driver row is deleted, the application row is automatically deleted by MySQL.

### Key fields that change during review:
| Field | Table | On Submit | On Approve | On Reject |
|-------|-------|-----------|------------|-----------|
| `is_verified` | `driver` | `0` (false) | `1` (true) | stays `0` |
| `application_status` | `driverapplications` | `Pending Review` | `Approved` | `Rejected` |

---

## Application Lifecycle

```
Student submits application
        │
        ▼
┌─────────────────────────┐
│  driver.is_verified = 0 │
│  app.status = "Pending  │
│               Review"   │
└─────────────────────────┘
        │
        ├── Admin APPROVES ──────────► driver.is_verified = 1
        │                              app.application_status = "Approved"
        │                              (driver can now offer rides)
        │
        ├── Admin REJECTS ───────────► driver.is_verified = 0  (unchanged)
        │                              app.application_status = "Rejected"
        │                              (driver can still log in, but cannot offer rides)
        │
        └── Driver DEACTIVATES ──────► driver.is_verified = 0
            (self-delete from app)     driver.role = "DEACTIVATED"
                                       app.application_status = "Deactivated"
```

---

## How Login Role is Determined (existing code)

In `DriverAuthServiceImpl.login()`, the login response role depends on `is_verified`:

```java
// For STUDENT_DRIVER role:
type = "DRIVER";
role = isVerified ? "DRIVER_APPROVED" : "DRIVER_PENDING";
```

This means:
- **Approved drivers** (is_verified = true) → login returns `role = "DRIVER_APPROVED"`
- **Pending/Rejected drivers** (is_verified = false) → login returns `role = "DRIVER_PENDING"`
- **Rejected drivers CAN still log in** — they get `DRIVER_PENDING` role. The app shows them their application status.
- **Deactivated drivers** — role is "DEACTIVATED", they can still technically authenticate but the app handles the state.

---

## What Needs to Be Built

### Endpoints Required

| Method | URL | Purpose |
|--------|-----|---------|
| GET | `/api/admin/applications` | List all applications (for admin review page) |
| GET | `/api/admin/applications/{applicationId}` | Get single application detail |
| PUT | `/api/admin/applications/{applicationId}/approve` | Approve application |
| PUT | `/api/admin/applications/{applicationId}/reject` | Reject application |

---

## Step 1: Add query method to DriverApplicationRepository

**File:** `src/main/java/com/example1/getyourride/repository/DriverApplicationRepository.java`

**ADD** these methods to the existing interface (do NOT replace the file):

```java
import java.util.List;

// Add to existing DriverApplicationRepository interface:

List<DriverApplication> findByApplicationStatus(String applicationStatus);

List<DriverApplication> findAllByOrderByApplicationIdDesc();
```

---

## Step 2: Create AdminApplicationService

**This is a NEW file.**

**File:** `src/main/java/com/example1/getyourride/service/AdminApplicationService.java`

```java
package com.example1.getyourride.service;

import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.DriverApplication;
import com.example1.getyourride.repository.DriverApplicationRepository;
import com.example1.getyourride.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for admin operations on driver applications (approve/reject).
 */
@Service
public class AdminApplicationService {

    private final DriverApplicationRepository driverAppRepo;
    private final DriverRepository driverRepo;

    public AdminApplicationService(DriverApplicationRepository driverAppRepo,
                                   DriverRepository driverRepo) {
        this.driverAppRepo = driverAppRepo;
        this.driverRepo = driverRepo;
    }

    /**
     * Returns all driver applications, newest first.
     */
    @Transactional(readOnly = true)
    public List<DriverApplication> getAllApplications() {
        return driverAppRepo.findAllByOrderByApplicationIdDesc();
    }

    /**
     * Returns applications filtered by status (e.g. "Pending Review", "Approved", "Rejected").
     */
    @Transactional(readOnly = true)
    public List<DriverApplication> getApplicationsByStatus(String status) {
        return driverAppRepo.findByApplicationStatus(status);
    }

    /**
     * Returns a single application by ID.
     */
    @Transactional(readOnly = true)
    public DriverApplication getApplication(Long applicationId) {
        return driverAppRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found with ID: " + applicationId));
    }

    /**
     * APPROVE a driver application.
     *
     * What happens:
     * 1. driver.is_verified → true (1)
     * 2. driverapplications.application_status → "Approved"
     *
     * After this, the driver's next login will return role = "DRIVER_APPROVED"
     * and they can offer rides.
     */
    @Transactional
    public DriverApplication approveApplication(Long applicationId) {
        DriverApplication app = driverAppRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found with ID: " + applicationId));

        Driver driver = driverRepo.findById(app.getDriverId())
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + app.getDriverId()));

        // 1. Set driver as verified
        driver.setIsVerified(true);
        driverRepo.save(driver);

        // 2. Update application status
        app.setApplicationStatus("Approved");
        driverAppRepo.save(app);

        return app;
    }

    /**
     * REJECT a driver application.
     *
     * What happens:
     * 1. driver.is_verified → stays false (0) — do NOT change it
     * 2. driverapplications.application_status → "Rejected"
     *
     * The driver can still log in (returns role = "DRIVER_PENDING")
     * but the app will show them that their application was rejected.
     */
    @Transactional
    public DriverApplication rejectApplication(Long applicationId) {
        DriverApplication app = driverAppRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found with ID: " + applicationId));

        Driver driver = driverRepo.findById(app.getDriverId())
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + app.getDriverId()));

        // 1. Ensure is_verified stays false (do NOT set to true)
        driver.setIsVerified(false);
        driverRepo.save(driver);

        // 2. Update application status to Rejected
        app.setApplicationStatus("Rejected");
        driverAppRepo.save(app);

        return app;
    }
}
```

---

## Step 3: Create AdminApplicationController

**This is a NEW file.**

**File:** `src/main/java/com/example1/getyourride/controller/AdminApplicationController.java`

```java
package com.example1.getyourride.controller;

import com.example1.getyourride.entity.DriverApplication;
import com.example1.getyourride.service.AdminApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for admin operations on driver applications.
 * Called by the admin website (review-application.html).
 *
 * Base path: /api/admin/applications
 */
@RestController
@RequestMapping("/api/admin/applications")
public class AdminApplicationController {

    private final AdminApplicationService adminService;

    public AdminApplicationController(AdminApplicationService adminService) {
        this.adminService = adminService;
    }

    /**
     * GET /api/admin/applications
     * Returns all applications (optionally filtered by status query param).
     *
     * Examples:
     *   GET /api/admin/applications              → all applications
     *   GET /api/admin/applications?status=Pending Review  → only pending
     */
    @GetMapping
    public ResponseEntity<List<DriverApplication>> getApplications(
            @RequestParam(required = false) String status) {
        List<DriverApplication> apps;
        if (status != null && !status.isBlank()) {
            apps = adminService.getApplicationsByStatus(status);
        } else {
            apps = adminService.getAllApplications();
        }
        return ResponseEntity.ok(apps);
    }

    /**
     * GET /api/admin/applications/{applicationId}
     * Returns a single application's details.
     */
    @GetMapping("/{applicationId}")
    public ResponseEntity<?> getApplication(@PathVariable Long applicationId) {
        try {
            DriverApplication app = adminService.getApplication(applicationId);
            return ResponseEntity.ok(app);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * PUT /api/admin/applications/{applicationId}/approve
     *
     * Approves the application:
     * - Sets driver.is_verified = true
     * - Sets application_status = "Approved"
     */
    @PutMapping("/{applicationId}/approve")
    public ResponseEntity<?> approveApplication(@PathVariable Long applicationId) {
        try {
            DriverApplication app = adminService.approveApplication(applicationId);
            return ResponseEntity.ok(app);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * PUT /api/admin/applications/{applicationId}/reject
     *
     * Rejects the application:
     * - driver.is_verified stays false
     * - Sets application_status = "Rejected"
     */
    @PutMapping("/{applicationId}/reject")
    public ResponseEntity<?> rejectApplication(@PathVariable Long applicationId) {
        try {
            DriverApplication app = adminService.rejectApplication(applicationId);
            return ResponseEntity.ok(app);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
```

---

## Step 4: Update SecurityConfig

**File:** `src/main/java/com/example1/getyourride/config/SecurityConfig.java`

The admin endpoints are already configured to require role ADMIN or STAFF:

```java
.requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "STAFF")
```

**HOWEVER** — if the admin website does NOT have authentication (no JWT), you need to
temporarily permit these endpoints publicly for development/testing:

**ADD** this line BEFORE the `.anyRequest().authenticated()` line:

```java
// Temporarily permit admin endpoints for development (remove in production)
.requestMatchers("/api/admin/**").permitAll()
```

**OR** keep the existing role-based check if your admin website sends a JWT with ADMIN role.

---

## Step 5: Verify Existing Login Logic Still Works

The existing `DriverAuthServiceImpl.login()` already handles the role correctly:

```java
// STUDENT_DRIVER with is_verified = true → role = "DRIVER_APPROVED"
// STUDENT_DRIVER with is_verified = false → role = "DRIVER_PENDING"
```

**No changes needed** to the login logic. After approval:
- Driver logs in → gets `role = "DRIVER_APPROVED"` → app shows full driver features
- Rejected driver logs in → gets `role = "DRIVER_PENDING"` → app shows application status

---

## Expected Behavior Summary

### When admin clicks "Approve" on review-application.html:

```
PUT /api/admin/applications/{applicationId}/approve

Database changes:
  driver table:              is_verified = 1  (was 0)
  driverapplications table:  application_status = "Approved"  (was "Pending Review")

Next driver login returns:
  type = "DRIVER"
  role = "DRIVER_APPROVED"
  isVerified = true
```

### When admin clicks "Reject" on review-application.html:

```
PUT /api/admin/applications/{applicationId}/reject

Database changes:
  driver table:              is_verified = 0  (stays unchanged)
  driverapplications table:  application_status = "Rejected"  (was "Pending Review")

Next driver login returns:
  type = "DRIVER"
  role = "DRIVER_PENDING"
  isVerified = false
```

### When driver deletes their own profile (from the app):

```
DELETE /api/driver-profile

Database changes:
  driver table:              is_verified = 0, role = "DEACTIVATED"
  driverapplications table:  application_status = "Deactivated"
```

### When admin uses the delete driver endpoint:

```
DELETE /api/shuttle-driver/profile/{driverId}

Database changes:
  ALL related data removed:
  - boarding_log entries (for bookings on driver's trips)
  - trip_booking entries (for driver's trips)
  - trip entries (driver's trips)
  - trip_stop entries (cascade from trip)
  - vehicle entries (driver's vehicles)
  - driverapplications entry (CASCADE from driver FK)
  - driver entry (deleted)
```

---

## Testing

### Approve a pending application:

```bash
# Get all pending applications
GET http://localhost:8080/api/admin/applications?status=Pending Review

# Approve one (use the ApplicationID from the response)
PUT http://localhost:8080/api/admin/applications/{applicationId}/approve
```

**Verify in database:**
```sql
SELECT d.driver_id, d.first_name, d.is_verified, da.application_status
FROM driver d
JOIN driverapplications da ON da.driver_id = d.driver_id
WHERE da.ApplicationID = {applicationId};
-- Expected: is_verified = 1, application_status = 'Approved'
```

### Reject a pending application:

```bash
PUT http://localhost:8080/api/admin/applications/{applicationId}/reject
```

**Verify in database:**
```sql
SELECT d.driver_id, d.first_name, d.is_verified, da.application_status
FROM driver d
JOIN driverapplications da ON da.driver_id = d.driver_id
WHERE da.ApplicationID = {applicationId};
-- Expected: is_verified = 0, application_status = 'Rejected'
```

### Login after approval (confirm role changes):

```bash
POST http://localhost:8080/api/auth/driver/login
Content-Type: application/json

{
  "email": "zanele.mbeki@mandela.ac.za",
  "password": "password123"
}
```

**Expected response after approval:**
```json
{
  "token": "...",
  "type": "DRIVER",
  "role": "DRIVER_APPROVED",
  "isVerified": true,
  ...
}
```

**Expected response when rejected or pending:**
```json
{
  "token": "...",
  "type": "DRIVER",
  "role": "DRIVER_PENDING",
  "isVerified": false,
  ...
}
```

---

## File Checklist

| # | File | Action |
|---|------|--------|
| 1 | `repository/DriverApplicationRepository.java` | Add `findByApplicationStatus` and `findAllByOrderByApplicationIdDesc` |
| 2 | `service/AdminApplicationService.java` | Create new file |
| 3 | `controller/AdminApplicationController.java` | Create new file |
| 4 | `config/SecurityConfig.java` | Permit `/api/admin/**` for dev (or keep role-gated) |

---

## Existing Code That Should NOT Change

These files already work correctly and should NOT be modified:

- `DriverAuthServiceImpl.java` — login role logic is correct
- `DriverApplicationService.java` — submission flow is correct
- `DriverProfileService.java` — deactivation flow is correct
- `DriverApplicationController.java` — submit/upload/finalize flow is correct
- `DriverProfileController.java` — profile GET/DELETE is correct

---

## Admin Website Integration

The admin `review-application.html` should call:

```javascript
// Fetch all pending applications
fetch('/api/admin/applications?status=Pending Review')
  .then(res => res.json())
  .then(apps => { /* render list */ });

// Approve button click
fetch(`/api/admin/applications/${applicationId}/approve`, { method: 'PUT' })
  .then(res => res.json())
  .then(app => { /* update UI */ });

// Reject button click
fetch(`/api/admin/applications/${applicationId}/reject`, { method: 'PUT' })
  .then(res => res.json())
  .then(app => { /* update UI */ });
```

---

## Notes

- The `driverapplications` table has `ON DELETE CASCADE` on `driver_id`, so if a driver
  record is deleted, the application record is automatically removed by MySQL.
- The `vehicle` table does NOT have `ON DELETE CASCADE` on `driver_id`, so vehicles must
  be explicitly deleted when removing a driver profile (already handled by the delete endpoint).
- All application status values are strings: `"Pending Review"`, `"Approved"`, `"Rejected"`, `"Deactivated"`.
  Use exact casing — the admin website and Android app check these exact values.
- A rejected driver is NOT blocked from logging in. They can still access the app,
  but the app shows them their rejection status on the profile screen.
