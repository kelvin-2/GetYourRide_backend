-- =====================================================================
-- 07_trip_review_tags.sql
-- Adds the trip_review_tags table for the tags field in trip reviews.
-- =====================================================================

CREATE TABLE `trip_review_tags` (
    `trip_review_id` BIGINT NOT NULL,
    `tag` VARCHAR(255) NOT NULL,
    CONSTRAINT `fk_trip_review_tags_review` FOREIGN KEY (`trip_review_id`) 
        REFERENCES `trip_review` (`review_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verification
SHOW TABLES LIKE 'trip_review_tags';
DESCRIBE `trip_review_tags`;
