package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.QrCodeResponse;

public interface QrCodeService {
    QrCodeResponse generateBoardingToken(Long bookingId, String studentEmail);
}
