package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.response.QrCodeResponse;
import com.example1.getyourride.entity.Booking;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.repository.BookingRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.security.QrTokenUtil;
import com.example1.getyourride.service.QrCodeService;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    private final BookingRepository bookingRepository;
    private final StudentRepository studentRepository;
    private final QrTokenUtil qrTokenUtil;

    public QrCodeServiceImpl(BookingRepository bookingRepository,
                             StudentRepository studentRepository,
                             QrTokenUtil qrTokenUtil) {
        this.bookingRepository = bookingRepository;
        this.studentRepository = studentRepository;
        this.qrTokenUtil = qrTokenUtil;
    }

    @Override
    public QrCodeResponse generateBoardingToken(Long bookingId, String studentEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!booking.getStudent().getStudentId().equals(student.getStudentId())) {
            throw new BadRequestException("Unauthorized access to booking");
        }

        String expiry = booking.getTrip().getDepartureTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String token = qrTokenUtil.generateToken(booking.getBookingId(), student.getStudentId(), expiry);

        return new QrCodeResponse(token, expiry);
    }
}
