-- =====================================================================
-- 02_trip_stop_status.sql
-- Adds the per-stop lifecycle column the Phase 4 simulation engine needs.
--
-- Context: doc/Task Phase 4 requires the simulator to "mark trip_stop.status
-- = ARRIVED" on stop arrival, but trip_stop had no status column in the
-- documented schema (doc/shuttle_db.sql). This migration adds it.
--
-- Run this BEFORE starting the application. spring.jpa.hibernate.ddl-auto is
-- set to `update`, so if Hibernate sees the column missing it will create it
-- as varchar(255) instead of the ENUM used elsewhere in this schema. Creating
-- it here first means Hibernate finds it and leaves it alone -- `update` adds
-- missing columns but never alters existing ones.
--
-- Follows the manual-migration pattern established by Phase 0
-- (01_cleanup_and_simulation_schema.sql). Run against a copy first.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Add the column
--
-- ENUM rather than varchar, matching trip.status and trip_booking.booking_status.
-- The database then rejects a typo'd status outright instead of storing it.
--
-- Values are deliberately limited to what Phase 4 actually needs. PENDING is
-- the default because every existing row describes a stop that has not been
-- reached, and NOT NULL requires a default for the backfill to succeed.
-- Adding a value later (SKIPPED, DEPARTED) needs another ALTER TABLE, which is
-- a conscious decision rather than speculative schema.
-- ---------------------------------------------------------------------
ALTER TABLE `trip_stop`
    ADD COLUMN `status` ENUM('PENDING','ARRIVED') NOT NULL DEFAULT 'PENDING'
    AFTER `stop_order`;

-- ---------------------------------------------------------------------
-- 2. Backfill historical trips
--
-- Stops on trips that already finished should read ARRIVED, not PENDING --
-- otherwise a completed trip renders with every stop still outstanding.
-- Cancelled trips are intentionally left PENDING: those stops were never
-- actually reached.
-- ---------------------------------------------------------------------
UPDATE `trip_stop` ts
JOIN `trip` t ON t.`trip_id` = ts.`trip_id`
SET ts.`status` = 'ARRIVED'
WHERE t.`status` = 'COMPLETED';

-- ---------------------------------------------------------------------
-- 3. Verification -- run these and check the output before moving on
-- ---------------------------------------------------------------------

-- Expect: status column present, type enum('PENDING','ARRIVED'), NOT NULL, default PENDING
SHOW COLUMNS FROM `trip_stop` LIKE 'status';

-- Expect: no PENDING rows against a COMPLETED trip
SELECT ts.`trip_id`, ts.`stop_order`, ts.`status`, t.`status` AS trip_status
FROM `trip_stop` ts
JOIN `trip` t ON t.`trip_id` = ts.`trip_id`
WHERE t.`status` = 'COMPLETED' AND ts.`status` <> 'ARRIVED';

-- Expect: only PENDING and ARRIVED
SELECT `status`, COUNT(*) AS row_count FROM `trip_stop` GROUP BY `status`;

-- ---------------------------------------------------------------------
-- Rollback, if needed
-- ---------------------------------------------------------------------
-- ALTER TABLE `trip_stop` DROP COLUMN `status`;
