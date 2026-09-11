-- =====================================================================
-- 06_notification_table.sql
-- Creates the `notification` table for in-app student notifications.
--
-- Context: when a driver cancels a trip, every student with a CONFIRMED
-- booking on that trip gets an in-app notification (shown via the bell on
-- the carpool home screen). There was no notification storage before this.
--
-- Run this ONCE against the database before starting the application with
-- the notification feature. spring.jpa.hibernate.ddl-auto=none, so Hibernate
-- will NOT create this table itself — the entity maps to a table that must
-- already exist. Follows the manual-migration pattern of the earlier numbered
-- files in this folder. Run against a copy first.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Create the table
--
-- Column names match the JPA mapping in entity/Notification.java exactly:
--   notification_id  -> @Id notificationId
--   student_id       -> recipient (ManyToOne Student), NOT NULL
--   message          -> message, up to 500 chars
--   trip_id          -> tripId, nullable (context only; no FK so a cancelled
--                        trip can be pruned later without orphaning history)
--   type             -> type, e.g. 'RIDE_CANCELLED'
--   is_read          -> read flag, default 0 (false)
--   created_at       -> set by @PrePersist on the entity
--
-- FK to student mirrors trip_booking.student_id. ON DELETE CASCADE so a
-- student's notifications go with them if the student row is removed.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notification` (
    `notification_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `student_id`      BIGINT       NOT NULL,
    `message`         VARCHAR(500) NOT NULL,
    `trip_id`         BIGINT       NULL,
    `type`            VARCHAR(50)  NOT NULL,
    `is_read`         TINYINT(1)   NOT NULL DEFAULT 0,
    `created_at`      DATETIME     NOT NULL,
    PRIMARY KEY (`notification_id`),
    KEY `idx_notification_student` (`student_id`),
    KEY `idx_notification_student_unread` (`student_id`, `is_read`),
    CONSTRAINT `fk_notification_student`
        FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 2. Verification -- run these and check the output before moving on
-- ---------------------------------------------------------------------

-- Expect: the 7 columns above with the right types/nullability
SHOW COLUMNS FROM `notification`;

-- Expect: fk_notification_student referencing student(student_id)
SELECT `CONSTRAINT_NAME`, `REFERENCED_TABLE_NAME`, `REFERENCED_COLUMN_NAME`
FROM `information_schema`.`KEY_COLUMN_USAGE`
WHERE `TABLE_NAME` = 'notification' AND `REFERENCED_TABLE_NAME` IS NOT NULL;

-- ---------------------------------------------------------------------
-- Rollback, if needed
-- ---------------------------------------------------------------------
-- DROP TABLE IF EXISTS `notification`;
