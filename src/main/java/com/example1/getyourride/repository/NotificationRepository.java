package com.example1.getyourride.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example1.getyourride.entity.Notification;
import com.example1.getyourride.entity.Student;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** All notifications for a student, newest first. */
    @EntityGraph(attributePaths = {"recipient"})
    List<Notification> findByRecipientOrderByCreatedAtDesc(Student recipient);

    /** Count of unread notifications, used to drive the bell badge. */
    long countByRecipientAndReadFalse(Student recipient);
}
