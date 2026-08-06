package com.example1.getyourride.controller;

import com.example1.getyourride.dto.request.MarkAsBoardedRequest;
import com.example1.getyourride.dto.response.*;
import com.example1.getyourride.service.ShuttleDriverService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for shuttle-driver-specific endpoints:
 * - Profile (GET /api/shuttle-driver/profile/{driverId})
 * - Active trip (GET /api/shuttle-driver/{driverId}/active-trip)
 * - Booked students (GET /api/shuttle-driver/trip/{tripId}/students)
 * - Mark as boarded (POST /api/shuttle-driver/boarding/mark)
 *
 * NOTE: Login is handled by DriverAuthController at POST /api/auth/driver/login.
 * It returns type = "SHUTTLE_DRIVER" for shuttle drivers automatically.
 */
@RestController
@RequestMapping("/api")
public class ShuttleDriverController {

    private final ShuttleDriverService shuttleDriverService;

    public ShuttleDriverController(ShuttleDriverService shuttleDriverService) {
        this.shuttleDriverService = shuttleDriverService;
    }

    /**
     * GET /api/shuttle-driver/profile/{driverId}
     *
     * Returns the full shuttle driver profile including:
     * - Driver info (name, email, phone, role, join date, total trips)
     * - Assigned vehicle (registration number, model, year, colour, capacity)
     * - Trip statistics (scheduled, in progress, completed, cancelled)
     */
    @GetMapping("/shuttle-driver/profile/{driverId}")
    public ResponseEntity<?> getProfile(@PathVariable Long driverId) {
        try {
            ShuttleDriverProfileResponse profile = shuttleDriverService.getProfile(driverId);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * GET /api/shuttle-driver/{driverId}/active-trip
     *
     * Returns the current/next active trip for this shuttle driver.
     * The Android boarding screen calls this when it opens.
     * Returns 404 if no active trip exists (app shows "No Active Trip" screen).
     */
    @GetMapping("/shuttle-driver/{driverId}/active-trip")
    public ResponseEntity<?> getActiveTrip(@PathVariable Long driverId) {
        try {
            ActiveTripResponse trip = shuttleDriverService.getActiveTrip(driverId);
            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * GET /api/shuttle-driver/trip/{tripId}/students
     *
     * Returns all booked students for a specific trip with boarding status.
     * Called after active-trip returns a tripId.
     */
    @GetMapping("/shuttle-driver/trip/{tripId}/students")
    public ResponseEntity<?> getBookedStudents(@PathVariable Long tripId) {
        try {
            List<BoardedStudentResponse> students = shuttleDriverService.getBookedStudents(tripId);
            return ResponseEntity.ok(students);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * POST /api/shuttle-driver/boarding/mark
     *
     * Marks a student as boarded.
     * Creates/updates boarding_log.boarded_at for this booking.
     *
     * Request body: { "bookingId": 123 }
     * Response: { "success": true, "message": "...", "boardedAt": "12:35" }
     */
    @PostMapping("/shuttle-driver/boarding/mark")
    public ResponseEntity<?> markAsBoarded(@RequestBody MarkAsBoardedRequest request) {
        try {
            MarkAsBoardedResponse result = shuttleDriverService.markAsBoarded(request.getBookingId());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * DELETE /api/shuttle-driver/profile/{driverId}
     *
     * Deletes a shuttle driver's profile and all associated data:
     * - Boarding logs
     * - Bookings
     * - Trips (and their stops)
     * - Vehicles
     * - Driver record
     *
     * This is a permanent, irreversible operation.
     */
    @DeleteMapping("/shuttle-driver/profile/{driverId}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long driverId) {
        try {
            shuttleDriverService.deleteProfile(driverId);
            return ResponseEntity.ok("Driver profile and all associated data deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
