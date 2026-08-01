package com.example1.getyourride.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class QrTokenUtil {

    private final String secretKey;

    public QrTokenUtil(@Value("${qr.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String generateToken(Long bookingId, Long studentId, String expiry) {
        String data = bookingId + ":" + studentId + ":" + expiry;
        String signature = hmacSha256(data);
        return Base64.getUrlEncoder().encodeToString((data + ":" + signature).getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token, Long bookingId, Long studentId, String expiry) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 4) return false;

            String bId = parts[0];
            String sId = parts[1];
            String exp = parts[2];
            String signature = parts[3];

            if (!bId.equals(bookingId.toString()) || !sId.equals(studentId.toString()) || !exp.equals(expiry)) {
                return false;
            }

            String expectedSignature = hmacSha256(bId + ":" + sId + ":" + exp);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSha256(String data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error signing QR token", e);
        }
    }
}
