package com.example1.getyourride.service.impl;

import com.example1.getyourride.entity.*;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.exception.ConflictException;
import com.example1.getyourride.repository.BoardingLogRepository;
import com.example1.getyourride.repository.BookingRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.security.QrTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShuttleBookingServiceImplTest {

    private ShuttleBookingServiceImpl shuttleBookingService;

    @Mock
    private TripRepository tripRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private BoardingLogRepository boardingLogRepository;
    @Mock
    private QrTokenUtil qrTokenUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        shuttleBookingService = new ShuttleBookingServiceImpl(
                tripRepository, bookingRepository, studentRepository, boardingLogRepository, qrTokenUtil);
    }

    @Test
    void bookShuttle_NonFundedStudent_ThrowsBadRequest() {
        Trip trip = new Trip();
        trip.setTripId(1L);
        trip.setTripType("SHUTTLE");
        trip.setAvailableSeats(5);

        Student student = new Student();
        student.setEmail("nonfunded@test.com");
        student.setIsFunded(false);

        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(studentRepository.findByEmail("nonfunded@test.com")).thenReturn(Optional.of(student));

        assertThrows(BadRequestException.class, () -> shuttleBookingService.bookShuttle(1L, "nonfunded@test.com"));
    }

    @Test
    void bookShuttle_FundedStudent_Succeeds() {
        Trip trip = new Trip();
        trip.setTripId(1L);
        trip.setTripType("SHUTTLE");
        trip.setAvailableSeats(5);
        trip.setDriver(new Driver());
        trip.getDriver().setFirstName("John");
        trip.getDriver().setLastName("Doe");

        Student student = new Student();
        student.setEmail("funded@test.com");
        student.setIsFunded(true);

        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(studentRepository.findByEmail("funded@test.com")).thenReturn(Optional.of(student));
        when(bookingRepository.findByTripAndStudent(any(), any())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> {
            Booking b = (Booking) i.getArguments()[0];
            b.setBookingId(10L);
            return b;
        });
        when(bookingRepository.findByStudent(student)).thenReturn(java.util.List.of());
        when(tripRepository.findByTripTypeIgnoreCase("SHUTTLE")).thenReturn(java.util.List.of(trip));

        com.example1.getyourride.dto.response.ShuttleBookingSummaryResponse response = shuttleBookingService.bookShuttle(1L, "funded@test.com");

        assertNotNull(response);
        assertNotNull(response.getBookingConfirmation());
        assertEquals(10L, response.getBookingConfirmation().getBookingId());
        verify(tripRepository).save(trip);
        verify(bookingRepository).save(any());
    }

    @Test
    void bookShuttle_FullShuttle_ThrowsConflictWithNewMessage() {
        Trip trip = new Trip();
        trip.setTripId(1L);
        trip.setTripType("SHUTTLE");
        trip.setAvailableSeats(0);

        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        ConflictException ex = assertThrows(ConflictException.class, () -> shuttleBookingService.bookShuttle(1L, "funded@test.com"));
        assert(ex.getMessage().contains("There are no more shuttles try next slot"));
    }
}
