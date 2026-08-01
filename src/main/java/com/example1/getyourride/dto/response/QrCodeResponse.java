package com.example1.getyourride.dto.response;

public class QrCodeResponse {
    private String qrToken;
    private String expiry;

    public QrCodeResponse(String qrToken, String expiry) {
        this.qrToken = qrToken;
        this.expiry = expiry;
    }

    public String getQrToken() {
        return qrToken;
    }

    public String getExpiry() {
        return expiry;
    }
}
