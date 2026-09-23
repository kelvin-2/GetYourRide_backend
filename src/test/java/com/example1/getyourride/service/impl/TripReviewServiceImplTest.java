package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.request.TripRatingRequest;
import com.example1.getyourride.entity.Booking;
import com.example1.getyourride.entity.BookingStatus;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.entity.TripReview;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.repository.BookingRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.repository.TripReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TripReviewServiceImplTest {

    @Mock
    private TripReviewRepository tripReviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private TripReviewServiceImpl tripReviewService;

    private Student student;
    private Booking booking;
    private TripRatingRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        student = new Student();
        student.setStudentId(1L);
        student.setEmail("student@test.com");

        booking = new Booking();
        booking.setBookingId(1L);
        booking.setStudent(student);
        booking.setBookingStatus(BookingStatus.BOARDED);

        request = new TripRatingRequest(1L, 5, "Great driver!", new java.util.ArrayList<>());
    }

    @Test
    void rateTrip_Success() {
        java.util.List<String> tags = java.util.Arrays.asList("On time", "Friendly");
        request.setTags(tags);

        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        when(tripReviewRepository.findByBookingBookingId(anyLong())).thenReturn(Optional.empty());
        when(tripReviewRepository.save(any(TripReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripReview result = tripReviewService.rateTrip(request, "student@test.com");

        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Great driver!", result.getReview());
        assertEquals(booking, result.getBooking());
        assertEquals(tags, result.getTags());
        verify(tripReviewRepository, times(1)).save(any(TripReview.class));
    }

    @Test
    void rateTrip_StudentNotFound() {
        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            tripReviewService.rateTrip(request, "student@test.com"));
    }

    @Test
    void rateTrip_BookingNotFound() {
        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            tripReviewService.rateTrip(request, "student@test.com"));
    }

    @Test
    void rateTrip_NotOwnBooking() {
        Student otherStudent = new Student();
        otherStudent.setStudentId(2L);
        booking.setStudent(otherStudent);

        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> 
            tripReviewService.rateTrip(request, "student@test.com"));
    }

    @Test
    void rateTrip_NotBoardedAndTripNotCompleted() {
        // Booking never boarded and the trip has not run yet — rating stays blocked.
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        Trip trip = new Trip();
        trip.setStatus("IN_PROGRESS");
        booking.setTrip(trip);

        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> 
            tripReviewService.rateTrip(request, "student@test.com"));
    }

    @Test
    void rateTrip_ConfirmedButTripCompleted_Succeeds() {
        // The student booked but was never scanned as boarded, yet the trip completed and now
        // shows in their history. They should be able to rate it.
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        Trip trip = new Trip();
        trip.setStatus("COMPLETED");
        booking.setTrip(trip);

        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        when(tripReviewRepository.findByBookingBookingId(anyLong())).thenReturn(Optional.empty());
        when(tripReviewRepository.save(any(TripReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripReview result = tripReviewService.rateTrip(request, "student@test.com");

        assertNotNull(result);
        assertEquals(5, result.getRating());
        verify(tripReviewRepository, times(1)).save(any(TripReview.class));
    }

    @Test
    void rateTrip_CancelledBooking_Blocked() {
        booking.setBookingStatus(BookingStatus.CANCELLED);
        Trip trip = new Trip();
        trip.setStatus("COMPLETED");
        booking.setTrip(trip);

        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> 
            tripReviewService.rateTrip(request, "student@test.com"));
    }

    @Test
    void rateTrip_AlreadyRated() {
        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        when(tripReviewRepository.findByBookingBookingId(anyLong())).thenReturn(Optional.of(new TripReview()));

        assertThrows(BadRequestException.class, () -> 
            tripReviewService.rateTrip(request, "student@test.com"));
    }
}
