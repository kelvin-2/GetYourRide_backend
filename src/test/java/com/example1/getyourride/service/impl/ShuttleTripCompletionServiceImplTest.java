package com.example1.getyourride.service.impl;

import com.example1.getyourride.entity.*;
import com.example1.getyourride.repository.BoardingLogRepository;
import com.example1.getyourride.repository.BookingRepository;
import com.example1.getyourride.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShuttleTripCompletionServiceImplTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BoardingLogRepository boardingLogRepository;

    @InjectMocks
    private ShuttleTripCompletionServiceImpl completionService;

    private Trip expiredTrip;
    private Trip activeTrip;
    private ShuttleTimeSlot timeSlot;

    @BeforeEach
    void setUp() {
        timeSlot = new ShuttleTimeSlot();
        timeSlot.setDeparts(LocalTime.of(8, 0));
        timeSlot.setArrives(LocalTime.of(9, 0));

        expiredTrip = new Trip();
        expiredTrip.setTripId(1L);
        expiredTrip.setTripType("SHUTTLE");
        expiredTrip.setStatus("SCHEDULED");
        expiredTrip.setTimeSlot(timeSlot);
        // Set departure to yesterday
        expiredTrip.setDepartureTime(LocalDateTime.now().minusDays(1).withHour(8).withMinute(0));

        activeTrip = new Trip();
        activeTrip.setTripId(2L);
        activeTrip.setTripType("SHUTTLE");
        activeTrip.setStatus("SCHEDULED");
        activeTrip.setTimeSlot(timeSlot);
        // Set departure to tomorrow
        activeTrip.setDepartureTime(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0));
    }

    @Test
    void completeExpiredShuttleTrips_ShouldCompleteExpiredTrip() {
        when(tripRepository.findByTripTypeIgnoreCase("SHUTTLE")).thenReturn(Collections.singletonList(expiredTrip));
        when(bookingRepository.findByTrip(expiredTrip)).thenReturn(new ArrayList<>());

        completionService.completeExpiredShuttleTrips();

        assertEquals("COMPLETED", expiredTrip.getStatus());
        verify(tripRepository).save(expiredTrip);
    }

    @Test
    void completeExpiredShuttleTrips_ShouldNotTouchActiveTrip() {
        when(tripRepository.findByTripTypeIgnoreCase("SHUTTLE")).thenReturn(Collections.singletonList(activeTrip));

        completionService.completeExpiredShuttleTrips();

        assertEquals("SCHEDULED", activeTrip.getStatus());
        verify(tripRepository, never()).save(activeTrip);
    }

    @Test
    void completeExpiredShuttleTrips_ShouldMarkUnboardedConfirmedBookingAsNoShow() {
        Booking confirmedBooking = new Booking();
        confirmedBooking.setBookingId(10L);
        confirmedBooking.setBookingStatus(BookingStatus.CONFIRMED);
        confirmedBooking.setTrip(expiredTrip);

        when(tripRepository.findByTripTypeIgnoreCase("SHUTTLE")).thenReturn(Collections.singletonList(expiredTrip));
        when(bookingRepository.findByTrip(expiredTrip)).thenReturn(Collections.singletonList(confirmedBooking));
        when(boardingLogRepository.findByBooking(confirmedBooking)).thenReturn(Optional.empty());

        completionService.completeExpiredShuttleTrips();

        assertEquals(BookingStatus.NO_SHOW, confirmedBooking.getBookingStatus());
        verify(bookingRepository).save(confirmedBooking);
    }

    @Test
    void completeExpiredShuttleTrips_ShouldLeaveBoardedBookingConfirmed() {
        Booking boardedBooking = new Booking();
        boardedBooking.setBookingId(11L);
        boardedBooking.setBookingStatus(BookingStatus.CONFIRMED);
        boardedBooking.setTrip(expiredTrip);

        BoardingLog log = new BoardingLog();
        log.setBoardedAt(LocalDateTime.now().minusHours(2));

        when(tripRepository.findByTripTypeIgnoreCase("SHUTTLE")).thenReturn(Collections.singletonList(expiredTrip));
        when(bookingRepository.findByTrip(expiredTrip)).thenReturn(Collections.singletonList(boardedBooking));
        when(boardingLogRepository.findByBooking(boardedBooking)).thenReturn(Optional.of(log));

        completionService.completeExpiredShuttleTrips();

        assertEquals(BookingStatus.CONFIRMED, boardedBooking.getBookingStatus());
        verify(bookingRepository, never()).save(boardedBooking);
    }

    @Test
    void completeExpiredShuttleTrips_ShouldLeaveCancelledBookingUntouched() {
        Booking cancelledBooking = new Booking();
        cancelledBooking.setBookingId(12L);
        cancelledBooking.setBookingStatus(BookingStatus.CANCELLED);
        cancelledBooking.setTrip(expiredTrip);

        when(tripRepository.findByTripTypeIgnoreCase("SHUTTLE")).thenReturn(Collections.singletonList(expiredTrip));
        when(bookingRepository.findByTrip(expiredTrip)).thenReturn(Collections.singletonList(cancelledBooking));

        completionService.completeExpiredShuttleTrips();

        assertEquals(BookingStatus.CANCELLED, cancelledBooking.getBookingStatus());
        verify(bookingRepository, never()).save(cancelledBooking);
    }

    @Test
    void completeExpiredShuttleTrips_ShouldSkipTripWithNullTimeSlot() {
        expiredTrip.setTimeSlot(null);
        when(tripRepository.findByTripTypeIgnoreCase("SHUTTLE")).thenReturn(Collections.singletonList(expiredTrip));

        completionService.completeExpiredShuttleTrips();

        assertEquals("SCHEDULED", expiredTrip.getStatus());
        verify(tripRepository, never()).save(expiredTrip);
    }

    @Test
    void completeExpiredShuttleTrips_ShouldBeIdempotent() {
        expiredTrip.setStatus("COMPLETED");
        when(tripRepository.findByTripTypeIgnoreCase("SHUTTLE")).thenReturn(Collections.singletonList(expiredTrip));

        completionService.completeExpiredShuttleTrips();

        verify(tripRepository, never()).save(expiredTrip);
    }
}
