package com.example1.getyourride.controller;

import com.example1.getyourride.dto.request.BoardingRequest;
import com.example1.getyourride.dto.response.BookingResponse;
import com.example1.getyourride.dto.response.QrCodeResponse;
import com.example1.getyourride.dto.response.ShuttleAvailabilityResponse;
import com.example1.getyourride.dto.response.ShuttleBookingSummaryResponse;
import com.example1.getyourride.service.QrCodeService;
import com.example1.getyourride.service.ShuttleBookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/shuttle")
public class ShuttleBookingController {

    private final ShuttleBookingService shuttleBookingService;
    private final QrCodeService qrCodeService;

    public ShuttleBookingController(ShuttleBookingService shuttleBookingService,
                                   QrCodeService qrCodeService) {
        this.shuttleBookingService = shuttleBookingService;
        this.qrCodeService = qrCodeService;
    }

    /** Lists shuttles for a day; returns available=false plus a message when none run. */
    @GetMapping("/available")
    public ResponseEntity<ShuttleAvailabilityResponse> getAvailableShuttles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(shuttleBookingService.getAvailableShuttles(date));
    }

    @PostMapping("/book/{tripId}")
    public ResponseEntity<ShuttleBookingSummaryResponse> bookShuttle(@PathVariable Long tripId, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(shuttleBookingService.bookShuttle(tripId, email));
    }

    @GetMapping("/booking/{bookingId}/qr")
    public ResponseEntity<QrCodeResponse> getQrToken(@PathVariable Long bookingId, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(qrCodeService.generateBoardingToken(bookingId, email));
    }

    @PostMapping("/board")
    public ResponseEntity<BookingResponse> boardShuttle(@RequestBody BoardingRequest request) {
        return ResponseEntity.ok(shuttleBookingService.verifyAndBoard(
                request.getQrToken(),
                request.getBookingId(),
                request.getStudentId(),
                request.getExpiry()
        ));
    }
}
