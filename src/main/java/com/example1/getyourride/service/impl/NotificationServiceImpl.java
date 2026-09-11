package com.example1.getyourride.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example1.getyourride.dto.response.NotificationResponse;
import com.example1.getyourride.entity.Booking;
import com.example1.getyourride.entity.BookingStatus;
import com.example1.getyourride.entity.Notification;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.repository.BookingRepository;
import com.example1.getyourride.repository.NotificationRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.service.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private static final DateTimeFormatter WHEN_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM 'at' HH:mm");

    private final NotificationRepository notificationRepository;
    private final BookingRepository bookingRepository;
    private final StudentRepository studentRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   BookingRepository bookingRepository,
                                   StudentRepository studentRepository) {
        this.notificationRepository = notificationRepository;
        this.bookingRepository = bookingRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional
    public void notifyRideCancelled(Trip trip) {
        List<Booking> bookings = bookingRepository.findByTrip(trip);

        String message = String.format(
                "Ride cancelled: %s \u2192 %s (%s).",
                trip.getDepartureStop(),
                trip.getDestinationStop(),
                trip.getDepartureTime() != null ? trip.getDepartureTime().format(WHEN_FORMAT) : "scheduled time"
        );

        int created = 0;
        for (Booking booking : bookings) {
            // Only students with an active (CONFIRMED) seat are affected by the cancellation.
            if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
                continue;
            }
            Student student = booking.getStudent();
            if (student == null) {
                continue;
            }

            Notification notification = new Notification();
            notification.setRecipient(student);
            notification.setMessage(message);
            notification.setTripId(trip.getTripId());
            notification.setType("RIDE_CANCELLED");
            notification.setRead(false);
            notificationRepository.save(notification);
            created++;
        }

        log.info("Created {} cancellation notification(s) for trip {}", created, trip.getTripId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String email) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with email: " + email));

        return notificationRepository.findByRecipientOrderByCreatedAtDesc(student).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with email: " + email));
        return notificationRepository.countByRecipientAndReadFalse(student);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, String email) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with email: " + email));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        // Ownership check — a student can only mark their own notifications read.
        if (notification.getRecipient() == null
                || !notification.getRecipient().getStudentId().equals(student.getStudentId())) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }

        notification.setRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getNotificationId(),
                n.getMessage(),
                n.getTripId(),
                n.getType(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
