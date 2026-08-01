package com.example1.getyourride.controller;

import com.example1.getyourride.dto.request.BoardingRequest;
import com.example1.getyourride.dto.response.BookingResponse;
import com.example1.getyourride.dto.response.QrCodeResponse;
import com.example1.getyourride.dto.response.ShuttleBookingSummaryResponse;
import com.example1.getyourride.service.QrCodeService;
import com.example1.getyourride.service.ShuttleBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
