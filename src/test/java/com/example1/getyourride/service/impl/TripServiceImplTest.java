package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.response.TripResponse;
import com.example1.getyourride.entity.BookingStatus;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.repository.VehicleRepository;
import com.example1.getyourride.service.GeocodingService;
import com.example1.getyourride.service.TripSimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class TripServiceImplTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private com.example1.getyourride.repository.BookingRepository bookingRepository;
    @Mock
    private GeocodingService geocodingService;
    @Mock
    private TripSimulationService tripSimulationService;
    @Mock
    private com.example1.getyourride.service.TripRouteService tripRouteService;
    @Mock
    private com.example1.getyourride.service.NotificationService notificationService;
    @Mock
    private com.example1.getyourride.repository.BoardingLogRepository boardingLogRepository;
    @Mock
    private com.example1.getyourride.repository.TripReviewRepository tripReviewRepository;

    @InjectMocks
    private TripServiceImpl tripService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void searchTrips_NonFundedStudent_ExcludesShuttles() {
        String email = "nonfunded@test.com";
        Student student = new Student();
        student.setIsFunded(false);
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));
        
        com.example1.getyourride.dto.response.GeocodeResponse geocodeResponse = com.example1.getyourride.dto.response.GeocodeResponse.notFound();
        when(geocodingService.geocode(anyString())).thenReturn(geocodeResponse);

        when(tripRepository.findByDepartureAndDestination(anyString(), anyString(), eq(false)))
                .thenReturn(Collections.emptyList());

        tripService.searchTrips("A", "B", email);

        org.mockito.Mockito.verify(tripRepository).findByDepartureAndDestination(anyString(), anyString(), eq(false));
    }

    @Test
    void cancelTrip_Driver_CancelsEntireTrip() {
        String email = "driver@test.com";
        Long tripId = 1L;

        // Mock security context
        org.springframework.security.core.Authentication auth = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn(email);
        org.springframework.security.core.context.SecurityContext securityContext = org.mockito.Mockito.mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

        when(studentRepository.findByEmail(email)).thenReturn(Optional.empty());

        Trip trip = new Trip();
        trip.setTripId(tripId);
        trip.setStatus("CONFIRMED");
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any())).thenReturn(trip);

        // Setup driver so mapToResponse doesn't fail
        com.example1.getyourride.entity.Driver driver = new com.example1.getyourride.entity.Driver();
        driver.setDriverId(10L);
        driver.setFirstName("Driver");
        driver.setLastName("One");
        trip.setDriver(driver);

        tripService.cancelTrip(tripId);

        // Verify that cancelTrip DID update the trip status to CANCELLED
        assertEquals("CANCELLED", trip.getStatus());
    }

    @Test
    void cancelTrip_Student_RedirectsToCancelBooking() {
        String email = "student@test.com";
        Long tripId = 1L;

        // Mock security context
        org.springframework.security.core.Authentication auth = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn(email);
        org.springframework.security.core.context.SecurityContext securityContext = org.mockito.Mockito.mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

        Student student = new Student();
        student.setIsFunded(false); // Non-funded student
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));

        Trip trip = new Trip();
        trip.setTripId(tripId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        com.example1.getyourride.entity.Booking booking = new com.example1.getyourride.entity.Booking();
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findByTripAndStudent(any(), any())).thenReturn(Optional.of(booking));

        // Setup driver so mapToResponse doesn't fail
        com.example1.getyourride.entity.Driver driver = new com.example1.getyourride.entity.Driver();
        driver.setDriverId(10L);
        driver.setFirstName("Driver");
        driver.setLastName("One");
        trip.setDriver(driver);
        trip.setAvailableSeats(3);

        tripService.cancelTrip(tripId);

        // Verify that cancelTrip did NOT update the trip status to CANCELLED
        assertNotEquals("CANCELLED", trip.getStatus());
        // Verify that booking status WAS updated to CANCELLED (via redirect to cancelBooking)
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
    }

    @Test
    void searchTripsByCoordinates_CalculatesDistances() {
        String email = "funded@test.com";
        Student student = new Student();
        student.setIsFunded(true);
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));

        Trip trip = new Trip();
        trip.setTripId(1L);
        trip.setTripType("SHUTTLE");
        trip.setAvailableSeats(10);
        trip.setDepartureLat(-34.0);
        trip.setDepartureLng(25.0);
        trip.setDestinationLat(-34.1);
        trip.setDestinationLng(25.1);
        trip.setDriver(new com.example1.getyourride.entity.Driver());
        trip.getDriver().setFirstName("John");
        trip.getDriver().setLastName("Doe");

        when(tripRepository.findNearbyTrips(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString(), eq(true)))
                .thenReturn(List.of(trip));

        List<TripResponse> results = tripService.searchTripsByCoordinates(-34.001, 25.001, -34.101, 25.101, 2.0, email);

        assertFalse(results.isEmpty());
        assertNotNull(results.get(0).getPickupDistance());
        assertNotNull(results.get(0).getDropOffDistance());
        assertTrue(results.get(0).getPickupDistance() > 0);
    }

    @Test
    void getMyTrips_Student_ReturnsBookedTrips() {
        String email = "student@test.com";
        Student student = new Student();
        student.setEmail(email);
        student.setStudentId(1L);
        when(driverRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));

        Trip trip = new Trip();
        trip.setTripId(101L);
        trip.setTripType("SHUTTLE");
        trip.setDepartureTime(java.time.LocalDateTime.now());
        trip.setDriver(new com.example1.getyourride.entity.Driver());
        trip.getDriver().setFirstName("Sam");
        trip.getDriver().setLastName("Driver");

        com.example1.getyourride.entity.Booking booking = new com.example1.getyourride.entity.Booking();
        booking.setTrip(trip);
        booking.setStudent(student);
        booking.setBookingStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findByStudent(student)).thenReturn(List.of(booking));
        when(tripRepository.findTripsByStudentInStops(anyLong())).thenReturn(List.of());

        List<TripResponse> results = tripService.getMyTrips(email);

        assertEquals(1, results.size());
        assertEquals(101L, results.get(0).getTripId());
        assertEquals("CONFIRMED", results.get(0).getBookingStatus());
    }

    @Test
    void getMyTrips_Inclusive_ReturnsDriverAndStudentTrips() {
        String email = "both@test.com";
        Student student = new Student();
        student.setStudentId(1L);
        student.setEmail(email);
        
        com.example1.getyourride.entity.Driver driver = new com.example1.getyourride.entity.Driver();
        driver.setDriverId(10L);
        driver.setFirstName("Both");
        driver.setLastName("Roles");
        driver.setEmail(email);

        when(driverRepository.findByEmail(email)).thenReturn(Optional.of(driver));
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));

        Trip tripAsDriver = new Trip();
        tripAsDriver.setTripId(1001L);
        tripAsDriver.setDepartureTime(java.time.LocalDateTime.now().plusHours(1));
        tripAsDriver.setDriver(driver);

        Trip tripAsStudentBooking = new Trip();
        tripAsStudentBooking.setTripId(2001L);
        tripAsStudentBooking.setDepartureTime(java.time.LocalDateTime.now().plusHours(2));
        tripAsStudentBooking.setDriver(new com.example1.getyourride.entity.Driver());
        tripAsStudentBooking.getDriver().setFirstName("Other");
        tripAsStudentBooking.getDriver().setLastName("Driver");

        Trip tripAsStudentStop = new Trip();
        tripAsStudentStop.setTripId(3001L);
        tripAsStudentStop.setDepartureTime(java.time.LocalDateTime.now().plusHours(3));
        tripAsStudentStop.setDriver(new com.example1.getyourride.entity.Driver());
        tripAsStudentStop.getDriver().setFirstName("Another");
        tripAsStudentStop.getDriver().setLastName("Driver");

        when(tripRepository.findByDriverDriverIdOrderByDepartureTimeDesc(10L)).thenReturn(List.of(tripAsDriver));
        
        com.example1.getyourride.entity.Booking booking = new com.example1.getyourride.entity.Booking();
        booking.setTrip(tripAsStudentBooking);
        booking.setBookingStatus(BookingStatus.PENDING);
        when(bookingRepository.findByStudent(student)).thenReturn(List.of(booking));
        
        when(tripRepository.findTripsByStudentInStops(1L)).thenReturn(List.of(tripAsStudentStop));

        List<TripResponse> results = tripService.getMyTrips(email);

        assertEquals(3, results.size());
        assertEquals(3001L, results.get(0).getTripId());
        assertEquals(2001L, results.get(1).getTripId());
        assertEquals("PENDING", results.get(1).getBookingStatus());
        assertEquals(1001L, results.get(2).getTripId());
    }

    @Test
    void getTripsByStatus_Scheduled_ExcludesTripsWithZeroSeats() {
        String email = "student@test.com";
        Student student = new Student();
        student.setIsFunded(false);
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));
        when(driverRepository.findByEmail(email)).thenReturn(Optional.empty());

        Trip tripWithSeats = new Trip();
        tripWithSeats.setTripId(1L);
        tripWithSeats.setAvailableSeats(3);
        tripWithSeats.setStatus("SCHEDULED");
        tripWithSeats.setDriver(new com.example1.getyourride.entity.Driver());
        tripWithSeats.getDriver().setDriverId(10L);
        tripWithSeats.getDriver().setFirstName("Driver");
        tripWithSeats.getDriver().setLastName("One");

        Trip tripWithNoSeats = new Trip();
        tripWithNoSeats.setTripId(2L);
        tripWithNoSeats.setAvailableSeats(0);
        tripWithNoSeats.setStatus("SCHEDULED");
        tripWithNoSeats.setDriver(new com.example1.getyourride.entity.Driver());
        tripWithNoSeats.getDriver().setDriverId(11L);
        tripWithNoSeats.getDriver().setFirstName("Driver");
        tripWithNoSeats.getDriver().setLastName("Two");

        when(tripRepository.findByStatus("SCHEDULED")).thenReturn(List.of(tripWithSeats, tripWithNoSeats));

        List<TripResponse> results = tripService.getTripsByStatus("SCHEDULED", email);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getTripId());
    }

    @Test
    void getTripsByStatus_IncludesDriverRating() {
        String email = "student@test.com";
        Student student = new Student();
        student.setIsFunded(false);
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));

        Trip trip = new Trip();
        trip.setTripId(1L);
        trip.setAvailableSeats(3);
        trip.setStatus("SCHEDULED");
        
        com.example1.getyourride.entity.Driver driver = new com.example1.getyourride.entity.Driver();
        driver.setDriverId(10L);
        driver.setFirstName("John");
        driver.setLastName("Doe");
        trip.setDriver(driver);

        when(tripRepository.findByStatus("SCHEDULED")).thenReturn(List.of(trip));
        when(tripReviewRepository.findAverageRatingByDriverId(10L)).thenReturn(4.55);

        List<TripResponse> results = tripService.getTripsByStatus("SCHEDULED", email);

        assertEquals(1, results.size());
        assertEquals(4.6, results.get(0).getDriverRating()); // Rounded to 1dp
    }

    @Test
    void getTripsByStatus_NullDriverRating_WhenNoReviews() {
        String email = "student@test.com";
        Student student = new Student();
        student.setIsFunded(false);
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));

        Trip trip = new Trip();
        trip.setTripId(1L);
        trip.setAvailableSeats(3);
        trip.setStatus("SCHEDULED");
        
        com.example1.getyourride.entity.Driver driver = new com.example1.getyourride.entity.Driver();
        driver.setDriverId(10L);
        driver.setFirstName("John");
        driver.setLastName("Doe");
        trip.setDriver(driver);

        when(tripRepository.findByStatus("SCHEDULED")).thenReturn(List.of(trip));
        when(tripReviewRepository.findAverageRatingByDriverId(10L)).thenReturn(null);

        List<TripResponse> results = tripService.getTripsByStatus("SCHEDULED", email);

        assertEquals(1, results.size());
        assertNull(results.get(0).getDriverRating());
    }
}
