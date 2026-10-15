package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.response.BookingResponse;
import com.example1.getyourride.dto.response.ShuttleAvailabilityResponse;
import com.example1.getyourride.dto.response.ShuttleBookingSummaryResponse;
import com.example1.getyourride.dto.response.TripResponse;
import com.example1.getyourride.dto.response.TripStopResponse;
import com.example1.getyourride.entity.*;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.exception.CampusRequiredException;
import com.example1.getyourride.exception.ConflictException;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.repository.BoardingLogRepository;
import com.example1.getyourride.repository.BookingRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.security.QrTokenUtil;
import com.example1.getyourride.service.ShuttleBookingService;
import com.example1.getyourride.validation.CampusValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShuttleBookingServiceImpl implements ShuttleBookingService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final StudentRepository studentRepository;
    private final BoardingLogRepository boardingLogRepository;
    private final QrTokenUtil qrTokenUtil;

    public ShuttleBookingServiceImpl(TripRepository tripRepository,
                                   BookingRepository bookingRepository,
                                   StudentRepository studentRepository,
                                   BoardingLogRepository boardingLogRepository,
                                   QrTokenUtil qrTokenUtil) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.studentRepository = studentRepository;
        this.boardingLogRepository = boardingLogRepository;
        this.qrTokenUtil = qrTokenUtil;
    }

    @Override
    @Transactional
    public ShuttleBookingSummaryResponse bookShuttle(Long tripId, String studentEmail) {
        // Hard requirement: Pessimistic write lock
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with ID: " + tripId));

        if (!"SHUTTLE".equalsIgnoreCase(trip.getTripType())) {
            throw new BadRequestException("This endpoint is only for shuttle bookings");
        }

        // Seeded shuttle trips were created with available_seats = NULL. Unboxing that into
        // the <= 0 check below threw a NullPointerException (HTTP 500) and no booking was ever
        // created. Initialise the seat count from the vehicle capacity the first time a shuttle
        // is booked, so capacity is still enforced from then on.
        if (trip.getAvailableSeats() == null) {
            int capacity = trip.getVehicle() != null ? trip.getVehicle().getCapacity() : 0;
            trip.setAvailableSeats(capacity);
        }

        if (trip.getAvailableSeats() <= 0) {
            throw new ConflictException("There are no more shuttles try next slot");
        }

        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (student.getIsFunded() == null || !student.getIsFunded()) {
            throw new BadRequestException("Only funded students can book shuttles");
        }

<<<<<<< HEAD
        // Duplicate check. A CANCELLED booking does not count, otherwise a student who cancelled
        // could never book this shuttle again. The cancelled row is reused (one row per
        // trip + student) and goes back to CONFIRMED so the driver's list shows them again.
        Optional<Booking> existingBooking = bookingRepository.findByTripAndStudent(trip, student);
        if (existingBooking.isPresent() && existingBooking.get().getBookingStatus() != BookingStatus.CANCELLED) {
=======
        // Validate that at least one stop is a campus
        if (!CampusValidationUtils.hasCampusStop(trip)) {
            throw new CampusRequiredException();
        }

        // Check for duplicate booking
        if (bookingRepository.findByTripAndStudent(trip, student).isPresent()) {
>>>>>>> aa93e2609bcca29f1f15d1854a8ba51ed52efa17
            throw new ConflictException("Student already has a booking for this trip");
        }

        Booking booking = existingBooking.orElseGet(Booking::new);
        booking.setTrip(trip);
        booking.setStudent(student);
        booking.setBookingDate(LocalDateTime.now());
        booking.setBookingStatus(BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);

        // Decrement seats and update status
        trip.setAvailableSeats(trip.getAvailableSeats() - 1);
        if ("SCHEDULED".equalsIgnoreCase(trip.getStatus())) {
            trip.setStatus("CONFIRMED");
        }
        tripRepository.save(trip);

        BookingResponse confirmation = new BookingResponse(savedBooking.getBookingId(), trip.getTripId(),
                savedBooking.getBookingStatus().name(), "Shuttle seat booked successfully!");

        // Fetch user's shuttle trips (all statuses)
        List<TripResponse> myShuttleTrips = bookingRepository.findByStudent(student).stream()
                .filter(b -> "SHUTTLE".equalsIgnoreCase(b.getTrip().getTripType()))
                .map(b -> {
                    TripResponse resp = mapToTripResponse(b.getTrip());
                    resp.setBookingStatus(b.getBookingStatus().name());
                    return resp;
                })
                .collect(Collectors.toList());

        // Fetch all shuttle trips
        List<TripResponse> allShuttleTrips = tripRepository.findByTripTypeIgnoreCase("SHUTTLE").stream()
                .map(this::mapToTripResponse)
                .collect(Collectors.toList());

        return ShuttleBookingSummaryResponse.builder()
                .bookingConfirmation(confirmation)
                .myShuttleTrips(myShuttleTrips)
                .allShuttleTrips(allShuttleTrips)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ShuttleAvailabilityResponse getAvailableShuttles(LocalDate date) {
        List<TripResponse> trips = tripRepository
                .findActiveShuttlesBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream()
                .map(this::mapToTripResponse)
                .collect(Collectors.toList());

        boolean available = !trips.isEmpty();
        String message = available ? null
                : "No shuttles are running on " + date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
                + ". Please try another day.";

        return ShuttleAvailabilityResponse.builder()
                .date(date)
                .available(available)
                .message(message)
                .trips(trips)
                .build();
    }

    private TripResponse mapToTripResponse(Trip trip) {
        TripResponse.TripResponseBuilder builder = TripResponse.builder()
                .tripId(trip.getTripId())
                .driverId(trip.getDriver().getDriverId())
                .driverName(trip.getDriver().getFirstName() + " " + trip.getDriver().getLastName())
                .tripType(trip.getTripType())
                .departureStop(trip.getDepartureStop())
                .departureLat(trip.getDepartureLat())
                .departureLng(trip.getDepartureLng())
                .destinationStop(trip.getDestinationStop())
                .destinationLat(trip.getDestinationLat())
                .destinationLng(trip.getDestinationLng())
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .availableSeats(trip.getAvailableSeats())
                .price(trip.getPrice())
                .status(trip.getStatus());

        if (trip.getVehicle() != null) {
            builder.registrationNumber(trip.getVehicle().getRegistrationNumber())
                    .vehicleModel(trip.getVehicle().getModel())
                    .vehicleColour(trip.getVehicle().getColour())
                    .vehicleCapacity(trip.getVehicle().getCapacity());
        }

        if (trip.getRoute() != null) {
            builder.routeName(trip.getRoute().getRouteName());
        }

        if (trip.getTimeSlot() != null) {
            String slotStr = trip.getTimeSlot().getDeparts() + " - " + trip.getTimeSlot().getArrives();
            builder.slotTime(slotStr);
        }

        if (trip.getStops() != null) {
            builder.stops(trip.getStops().stream()
                    .map(stop -> TripStopResponse.builder()
                            .id(stop.getId())
                            .stopName(stop.getStopName())
                            .latitude(stop.getLatitude())
                            .longitude(stop.getLongitude())
                            .stopOrder(stop.getStopOrder())
                            .status(stop.getStatus())
                            .studentId(stop.getStudent() != null ? stop.getStudent().getStudentId() : null)
                            .studentName(stop.getStudent() != null ? stop.getStudent().getFirstName() + " " + stop.getStudent().getLastName() : null)
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    @Override
    @Transactional
    public BookingResponse verifyAndBoard(String qrToken, Long bookingId, Long studentId, String expiry) {
        if (!qrTokenUtil.validateToken(qrToken, bookingId, studentId, expiry)) {
            throw new BadRequestException("Invalid or expired QR token");
        }

        LocalDateTime expiryTime = LocalDateTime.parse(expiry);
        if (LocalDateTime.now().isAfter(expiryTime)) {
            throw new BadRequestException("QR token has expired (shuttle already departed)");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getStudent().getStudentId().equals(studentId)) {
            throw new BadRequestException("Token/Booking mismatch");
        }

        if (booking.getBookingStatus() == BookingStatus.BOARDED) {
            throw new ConflictException("Student already boarded");
        }

        if (booking.getBookingStatus() == BookingStatus.NO_SHOW || 
            booking.getBookingStatus() == BookingStatus.CANCELLED ||
            "COMPLETED".equalsIgnoreCase(booking.getTrip().getStatus())) {
            throw new BadRequestException("Cannot board: booking or trip is in a final state");
        }

        // Additional check for slot time expiration
        Trip trip = booking.getTrip();
        if (trip.getTimeSlot() != null && trip.getTimeSlot().getArrives() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime slotEnd = trip.getDepartureTime().toLocalDate().atTime(trip.getTimeSlot().getArrives());
            if (slotEnd.isBefore(now)) {
                throw new BadRequestException("Cannot board: Time slot has ended");
            }
        }

        booking.setBookingStatus(BookingStatus.BOARDED);
        bookingRepository.save(booking);

        BoardingLog log = new BoardingLog();
        log.setBooking(booking);
        log.setBoardedAt(LocalDateTime.now());
        boardingLogRepository.save(log);

        return new BookingResponse(booking.getBookingId(), booking.getTrip().getTripId(),
                booking.getBookingStatus().name(), "Boarding successful!");
    }
}
