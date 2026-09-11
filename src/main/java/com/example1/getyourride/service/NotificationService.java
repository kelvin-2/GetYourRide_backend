package com.example1.getyourride.service;

import java.util.List;

import com.example1.getyourride.dto.response.NotificationResponse;
import com.example1.getyourride.entity.Trip;

/**
 * Creates and reads in-app notifications for students.
 */
public interface NotificationService {

    /**
     * Notify every student with an active (CONFIRMED) booking on this trip that it was cancelled.
     * If no students are booked, nothing is created.
     *
     * @param trip The trip that has just been cancelled.
     */
    void notifyRideCancelled(Trip trip);

    /**
     * All notifications for the authenticated student, newest first.
     * @param email Student's email from the JWT.
     */
    List<NotificationResponse> getMyNotifications(String email);

    /**
     * Count of the authenticated student's unread notifications (drives the bell badge).
     * @param email Student's email from the JWT.
     */
    long getUnreadCount(String email);

    /**
     * Mark one notification as read. The notification must belong to the authenticated student.
     * @param notificationId The notification to mark read.
     * @param email Student's email from the JWT.
     */
    NotificationResponse markAsRead(Long notificationId, String email);
}
