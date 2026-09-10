package com.example1.getyourride.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * An in-app notification addressed to a single student.
 *
 * <p>Currently created when a driver cancels a trip the student had booked, but the
 * {@code type} field keeps the table open to other notification kinds later without a
 * schema change.
 *
 * <p>The table is created by the manual migration doc/06_notification_table.sql —
 * Hibernate does not manage this schema (spring.jpa.hibernate.ddl-auto=none).
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    /** The student who receives this notification. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student recipient;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /** The trip this notification refers to, for context. Nullable for non-trip notifications. */
    @Column(name = "trip_id")
    private Long tripId;

    /** e.g. "RIDE_CANCELLED". A plain string so new kinds don't need a schema migration. */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
