package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.request.TripRatingRequest;
import com.example1.getyourride.dto.response.TripReviewDetailResponse;
import com.example1.getyourride.entity.Booking;
import com.example1.getyourride.entity.BookingStatus;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.entity.TripReview;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.repository.BookingRepository;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.repository.TripReviewRepository;
import com.example1.getyourride.service.TripReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TripReviewServiceImpl implements TripReviewService {

    private final TripReviewRepository tripReviewRepository;
    private final BookingRepository bookingRepository;
    private final StudentRepository studentRepository;
    private final TripRepository tripRepository;
    private final DriverRepository driverRepository;

    public TripReviewServiceImpl(TripReviewRepository tripReviewRepository,
                                 BookingRepository bookingRepository,
                                 StudentRepository studentRepository,
                                 TripRepository tripRepository,
                                 DriverRepository driverRepository) {
        this.tripReviewRepository = tripReviewRepository;
        this.bookingRepository = bookingRepository;
        this.studentRepository = studentRepository;
        this.tripRepository = tripRepository;
        this.driverRepository = driverRepository;
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

        if (booking.getBookingStatus() != BookingStatus.BOARDED && !"COMPLETED".equalsIgnoreCase(booking.getTrip().getStatus())) {
            throw new BadRequestException("You can only rate trips that have been completed or boarded");
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

    @Override
    @Transactional(readOnly = true)
    public List<TripReviewDetailResponse> getReviewsForTrip(Long tripId, String driverEmail) {
        Driver driver = driverRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        // Ownership check: a driver may only view reviews for their own trips.
        if (trip.getDriver() == null
                || !trip.getDriver().getDriverId().equals(driver.getDriverId())) {
            throw new BadRequestException("You can only view ratings for your own trips");
        }

        return tripReviewRepository.findByBooking_Trip_TripIdOrderByReviewDateDesc(tripId)
                .stream()
                .map(this::mapToDetailResponse)
                .collect(Collectors.toList());
    }

    private TripReviewDetailResponse mapToDetailResponse(TripReview review) {
        Booking booking = review.getBooking();
        Student student = booking != null ? booking.getStudent() : null;
        String studentName = student != null
                ? (student.getFirstName() + " " + student.getLastName()).trim()
                : "Student";

        return TripReviewDetailResponse.builder()
                .reviewId(review.getReviewId())
                .bookingId(booking != null ? booking.getBookingId() : null)
                .studentName(studentName)
                .rating(review.getRating())
                .review(review.getReview())
                .tags(review.getTags())
                .reviewDate(review.getReviewDate())
                .build();
    }
}
