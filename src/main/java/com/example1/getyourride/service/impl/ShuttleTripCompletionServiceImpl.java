package com.example1.getyourride.service.impl;

import com.example1.getyourride.entity.BoardingLog;
import com.example1.getyourride.entity.Booking;
import com.example1.getyourride.entity.BookingStatus;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.repository.BoardingLogRepository;
import com.example1.getyourride.repository.BookingRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.service.ShuttleTripCompletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class ShuttleTripCompletionServiceImpl implements ShuttleTripCompletionService {

    private static final Logger log = LoggerFactory.getLogger(ShuttleTripCompletionServiceImpl.class);

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final BoardingLogRepository boardingLogRepository;

    public ShuttleTripCompletionServiceImpl(TripRepository tripRepository,
                                           BookingRepository bookingRepository,
                                           BoardingLogRepository boardingLogRepository) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.boardingLogRepository = boardingLogRepository;
    }

    @Override
    @Transactional
    public void completeExpiredShuttleTrips() {
        log.debug("Checking for expired SHUTTLE trips to complete");

        List<String> activeStatuses = Arrays.asList("SCHEDULED", "CONFIRMED", "ACTIVE", "IN_PROGRESS");
        List<Trip> shuttleTrips = tripRepository.findByTripTypeIgnoreCase("SHUTTLE");

        LocalDateTime now = LocalDateTime.now();

        for (Trip trip : shuttleTrips) {
            if (!activeStatuses.contains(trip.getStatus().toUpperCase())) {
                continue;
            }

            if (trip.getTimeSlot() == null) {
                log.warn("SHUTTLE trip {} has no time slot assigned, skipping expiration check", trip.getTripId());
                continue;
            }

            LocalTime endTime = trip.getTimeSlot().getArrives();
            if (endTime == null) {
                log.warn("SHUTTLE trip {} has a time slot with no arrival time, skipping expiration check", trip.getTripId());
                continue;
            }

            LocalDateTime slotEndDateTime = trip.getDepartureTime().toLocalDate().atTime(endTime);

            if (slotEndDateTime.isBefore(now)) {
                processExpiredTrip(trip);
            }
        }
    }

    private void processExpiredTrip(Trip trip) {
        log.info("Completing expired SHUTTLE trip {} (slot ended at {})", trip.getTripId(), 
                trip.getDepartureTime().toLocalDate().atTime(trip.getTimeSlot().getArrives()));

        trip.setStatus("COMPLETED");
        tripRepository.save(trip);

        List<Booking> bookings = bookingRepository.findByTrip(trip);
        for (Booking booking : bookings) {
            if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
                Optional<BoardingLog> logOpt = boardingLogRepository.findByBooking(booking);
                boolean boarded = logOpt.isPresent() && logOpt.get().getBoardedAt() != null;

                if (!boarded) {
                    log.info("Marking unboarded booking {} as NO_SHOW for trip {}", booking.getBookingId(), trip.getTripId());
                    booking.setBookingStatus(BookingStatus.NO_SHOW);
                    bookingRepository.save(booking);
                }
            }
        }
    }
}
