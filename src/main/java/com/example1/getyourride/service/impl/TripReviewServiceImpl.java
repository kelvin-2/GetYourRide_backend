package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.request.TripRatingRequest;
import com.example1.getyourride.entity.Booking;
import com.example1.getyourride.entity.BookingStatus;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.entity.TripReview;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.repository.BookingRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.repository.TripReviewRepository;
import com.example1.getyourride.service.TripReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripReviewServiceImpl implements TripReviewService {

    private final TripReviewRepository tripReviewRepository;
    private final BookingRepository bookingRepository;
    private final StudentRepository studentRepository;

    public TripReviewServiceImpl(TripReviewRepository tripReviewRepository,
                                 BookingRepository bookingRepository,
                                 StudentRepository studentRepository) {
        this.tripReviewRepository = tripReviewRepository;
        this.bookingRepository = bookingRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional
    public TripReview rateTrip(TripRatingRequest request, String studentEmail) {
        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getStudent().getStudentId().equals(student.getStudentId())) {
            throw new BadRequestException("You can only rate your own trips");
        }

        // A trip can be rated once the student has actually taken it. Two signals count:
        //   1. The booking was marked BOARDED (driver scanned/boarded the student), or
        //   2. The trip itself has COMPLETED — the ride happened and now sits in the student's
        //      history. Bookings that were never scanned stay CONFIRMED even after the trip runs,
        //      so relying on BOARDED alone wrongly blocked rating for trips shown as "past".
        // Cancelled or expired bookings can never be rated.
        if (booking.getBookingStatus() == BookingStatus.CANCELLED
                || booking.getBookingStatus() == BookingStatus.EXPIRED) {
            throw new BadRequestException("You cannot rate a cancelled trip");
        }

        boolean boarded = booking.getBookingStatus() == BookingStatus.BOARDED;
        boolean tripCompleted = booking.getTrip() != null
                && "COMPLETED".equalsIgnoreCase(booking.getTrip().getStatus());
        if (!boarded && !tripCompleted) {
            throw new BadRequestException("You can only rate a trip once it has been completed");
        }

        if (tripReviewRepository.findByBookingBookingId(request.getBookingId()).isPresent()) {
            throw new BadRequestException("You have already rated this trip");
        }

        TripReview review = new TripReview();
        review.setBooking(booking);
        review.setRating(request.getRating());
        review.setReview(request.getReview());
        review.setTags(request.getTags());

        return tripReviewRepository.save(review);
    }
}
