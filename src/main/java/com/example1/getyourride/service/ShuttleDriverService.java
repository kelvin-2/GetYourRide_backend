package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.*;
import com.example1.getyourride.entity.*;
import com.example1.getyourride.repository.*;
import com.example1.getyourride.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service handling shuttle-driver-specific authentication, profile retrieval, and boarding operations.
 */
@Service
public class ShuttleDriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final BoardingLogRepository boardingLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public ShuttleDriverService(DriverRepository driverRepository,
                                VehicleRepository vehicleRepository,
                                TripRepository tripRepository,
                                BookingRepository bookingRepository,
                                BoardingLogRepository boardingLogRepository,
                                PasswordEncoder passwordEncoder,
                                JwtUtil jwtUtil) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.boardingLogRepository = boardingLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticate a shuttle driver by email + password.
     * Only drivers with role = 'SHUTTLE_DRIVER' can use this endpoint.
     */
    @Transactional(readOnly = true)
    public AuthResponse authenticate(String email, String password) {
        Driver driver = driverRepository.findByEmailAndRole(email, "SHUTTLE_DRIVER")
                .orElseThrow(() -> new IllegalArgumentException("No shuttle driver account found with this email"));

        // Support both BCrypt-encoded passwords and plain-text (legacy seed data)
        if (!passwordEncoder.matches(password, driver.getPassword()) && !password.equals(driver.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(driver.getIsVerified())) {
            throw new SecurityException("Account not verified. Contact admin.");
        }

        // Generate JWT with SHUTTLE_DRIVER type
        String token = jwtUtil.generateToken(
                driver.getDriverId(),
                driver.getEmail(),
                "SHUTTLE_DRIVER",
                Map.of("role", "SHUTTLE_DRIVER")
        );

        return AuthResponse.builder()
                .token(token)
                .type("SHUTTLE_DRIVER")
                .id(driver.getDriverId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail())
                .studentNumber("")
                .phone(driver.getPhone())
                .isFunded(null)
                .role(driver.getRole())
                .isVerified(driver.getIsVerified())
                .build();
    }

    /**
     * Build the full profile for a shuttle driver including vehicle and trip stats.
     */
    @Transactional(readOnly = true)
    public ShuttleDriverProfileResponse getProfile(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        // --- Vehicle ---
        ShuttleVehicleResponse vehicleResponse = null;
        Optional<Vehicle> vehicleOpt = vehicleRepository.findFirstByDriverDriverId(driverId);
        if (vehicleOpt.isPresent()) {
            Vehicle v = vehicleOpt.get();
            vehicleResponse = ShuttleVehicleResponse.builder()
                    .vehicleId(v.getVehicleId())
                    .registrationNumber(v.getRegistrationNumber())
                    .model(v.getModel())
                    .vehicleYear(v.getVehicleYear())
                    .colour(v.getColour())
                    .capacity(v.getCapacity())
                    .build();
        }

        // --- Trip Summary ---
        int scheduled = tripRepository.countByDriverIdAndStatus(driverId, "SCHEDULED");
        int inProgress = tripRepository.countByDriverIdAndStatus(driverId, "IN_PROGRESS");
        int completed = tripRepository.countByDriverIdAndStatus(driverId, "COMPLETED");
        int cancelled = tripRepository.countByDriverIdAndStatus(driverId, "CANCELLED");

        String currentTripRoute = null;
        String currentTripStatus = null;

        List<Trip> activeTrips = tripRepository.findActiveTrips(driverId);
        if (!activeTrips.isEmpty()) {
            Trip current = activeTrips.get(0);
            currentTripRoute = current.getDepartureStop() + " \u2192 " + current.getDestinationStop();
            currentTripStatus = current.getStatus();
        }

        TripSummaryResponse tripSummary = TripSummaryResponse.builder()
                .currentTripRoute(currentTripRoute)
                .currentTripStatus(currentTripStatus)
                .scheduledTrips(scheduled)
                .inProgressTrips(inProgress)
                .completedTrips(completed)
                .cancelledTrips(cancelled)
                .studentsBookedToday(0)
                .studentsBoardedToday(0)
                .build();

        return ShuttleDriverProfileResponse.builder()
                .driverId(driver.getDriverId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail())
                .phone(driver.getPhone())
                .role(driver.getRole())
                .joinDate(driver.getJoinDate() != null ? driver.getJoinDate().toString() : null)
                .totalTrips(driver.getTotalTrips())
                .isVerified(driver.getIsVerified())
                .vehicle(vehicleResponse)
                .tripSummary(tripSummary)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // BOARDING METHODS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get the current/next active trip for this shuttle driver.
     * Returns the nearest scheduled or in-progress trip.
     */
    @Transactional(readOnly = true)
    public ActiveTripResponse getActiveTrip(Long driverId) {
        List<Trip> activeTrips = tripRepository.findActiveTrips(driverId);

        if (activeTrips.isEmpty()) {
            throw new IllegalArgumentException("No active trip assigned. Check your schedule.");
        }

        Trip trip = activeTrips.get(0); // Nearest upcoming/active trip

        // Get vehicle capacity
        Optional<Vehicle> vehicleOpt = vehicleRepository.findFirstByDriverDriverId(driverId);
        int capacity = vehicleOpt.map(Vehicle::getCapacity).orElse(0);

        // Count booked and boarded students
        List<Booking> bookings = bookingRepository.findByTrip(trip);
        int totalBooked = bookings.size();
        int totalBoarded = 0;

        for (Booking booking : bookings) {
            Optional<BoardingLog> log = boardingLogRepository.findByBooking(booking);
            if (log.isPresent() && log.get().getBoardedAt() != null) {
                totalBoarded++;
            }
        }

        return ActiveTripResponse.builder()
                .tripId(trip.getTripId())
                .departureStop(trip.getDepartureStop())
                .destinationStop(trip.getDestinationStop())
                .departureTime(formatDateTime(trip.getDepartureTime()))
                .arrivalTime(formatDateTime(trip.getArrivalTime()))
                .status(trip.getStatus())
                .capacity(capacity)
                .registrationNumber(trip.getVehicle() != null
                        ? trip.getVehicle().getRegistrationNumber() : "")
                .totalBooked(totalBooked)
                .totalBoarded(totalBoarded)
                .build();
    }

    /**
     * Get all booked students for a trip, with their boarding status.
     */
    @Transactional(readOnly = true)
    public List<BoardedStudentResponse> getBookedStudents(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        List<Booking> bookings = bookingRepository.findByTrip(trip);
        List<BoardedStudentResponse> result = new ArrayList<>();

        for (Booking booking : bookings) {
            Student student = booking.getStudent();
            if (student == null) continue;

            // Check boarding status
            Optional<BoardingLog> logOpt = boardingLogRepository.findByBooking(booking);
            String boardedAt = null;
            if (logOpt.isPresent() && logOpt.get().getBoardedAt() != null) {
                boardedAt = formatDateTime(logOpt.get().getBoardedAt());
            }

            result.add(BoardedStudentResponse.builder()
                    .bookingId(booking.getBookingId())
                    .studentId(student.getStudentId())
                    .firstName(student.getFirstName())
                    .lastName(student.getLastName())
                    .studentNumber(student.getStudentNumber())
                    .bookingStatus(booking.getBookingStatus() != null
                            ? booking.getBookingStatus().name() : "PENDING")
                    .boardedAt(boardedAt)
                    .build());
        }

        return result;
    }

    /**
     * Mark a student as boarded.
     * Creates a boarding_log entry if one doesn't exist,
     * or updates the existing one with boarded_at = now.
     */
    @Transactional
    public MarkAsBoardedResponse markAsBoarded(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));

        // Find or create boarding log
        Optional<BoardingLog> existingLog = boardingLogRepository.findByBooking(booking);
        BoardingLog log;

        if (existingLog.isPresent()) {
            log = existingLog.get();
        } else {
            log = new BoardingLog();
            log.setBooking(booking);
        }

        // Set boarded time
        LocalDateTime now = LocalDateTime.now();
        log.setBoardedAt(now);
        boardingLogRepository.save(log);

        return MarkAsBoardedResponse.builder()
                .success(true)
                .message("Student marked as boarded successfully")
                .boardedAt(formatDateTime(now))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // DELETE PROFILE
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Delete a shuttle driver's profile and all associated data.
     * Cascade order:
     * 1. Boarding logs (for all bookings on the driver's trips)
     * 2. Bookings (for all the driver's trips)
     * 3. Trip stops (auto-cascaded by JPA via Trip entity)
     * 4. Trips
     * 5. Vehicles
     * 6. Driver
     */
    @Transactional
    public void deleteProfile(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        // 1. Get all trips for this driver
        List<Trip> driverTrips = tripRepository.findByDriverDriverId(driverId);

        if (!driverTrips.isEmpty()) {
            // 2. Get all bookings for those trips
            List<Booking> tripBookings = bookingRepository.findByTripIn(driverTrips);

            if (!tripBookings.isEmpty()) {
                // 3. Delete all boarding logs for those bookings
                boardingLogRepository.deleteByBookingIn(tripBookings);

                // 4. Delete all bookings for those trips
                bookingRepository.deleteByTripIn(driverTrips);
            }

            // 5. Delete all trips (trip_stops cascade automatically via CascadeType.ALL)
            tripRepository.deleteByDriverId(driverId);
        }

        // 6. Delete all vehicles assigned to this driver
        vehicleRepository.deleteByDriver(driver);

        // 7. Delete the driver record
        driverRepository.delete(driver);
    }

    /**
     * Helper to format LocalDateTime as a readable string (HH:mm).
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
