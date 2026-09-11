package com.example1.getyourride.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What the app receives for a single notification.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long notificationId;
    private String message;
    private Long tripId;
    private String type;
    private boolean read;
    private LocalDateTime createdAt;
}
