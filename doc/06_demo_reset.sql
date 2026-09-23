-- =====================================================================
-- 06_demo_reset.sql
-- Puts the demo student's trips back to a not-yet-started state so each one can be
-- started again and watched moving. Run this before a demo, or any time a trip has
-- finished and you want to replay it.
--
-- This is the SQL equivalent of calling PATCH /api/trips/{id}/schedule on each trip.
-- Prefer the endpoint when the app is running; use this when you want to reset in bulk
-- or the backend is not up.
--
-- WHY THE EXTRA COLUMNS MATTER
-- Setting status alone is not enough. A finished trip keeps current_lat/current_lng at
-- its final point, current_leg_index at the last leg, arrival_time set, and its stops
-- marked ARRIVED. If only the status is changed, the app draws a stationary car parked
-- at the destination and reports the trip as live even though the simulator will never
-- move it (the scheduler only advances trips with status = 'IN_PROGRESS').
--
-- SAFE UPDATE MODE
-- These statements filter on trip_id / student_id, which are indexed key columns, so
-- MySQL Workbench's safe-update mode accepts them. (Error 1175 only appears when a
-- WHERE clause uses no key column.)
-- =====================================================================

USE `shuttle_db`;

-- ---------------------------------------------------------------------
-- 1. See the current state before changing anything
-- ---------------------------------------------------------------------
SELECT t.`trip_id`, t.`status` AS trip_status, t.`arrival_time`,
       t.`current_lat`, t.`current_leg_index`,
       COUNT(ts.`id`) AS stops,
       SUM(ts.`status` = 'ARRIVED') AS arrived_stops,
       b.`booking_status`
FROM `trip` t
JOIN `trip_booking` b ON b.`trip_id` = t.`trip_id`
LEFT JOIN `trip_stop` ts ON ts.`trip_id` = t.`trip_id`
WHERE b.`student_id` = (SELECT `student_id` FROM `student` WHERE `email` = 'test@mandela.ac.za')
GROUP BY t.`trip_id`, b.`booking_status`
ORDER BY t.`trip_id`;

-- ---------------------------------------------------------------------
-- 2. Reset the trips this student has booked
--
-- Scoped to the demo student's bookings so no unrelated trip is touched. Swap the
-- email, or replace the whole subquery with an explicit list, e.g.
--     WHERE `trip_id` IN (552, 555, 561, 562, 563)
-- ---------------------------------------------------------------------
UPDATE `trip`
SET `status`              = 'SCHEDULED',
    `current_lat`         = NULL,
    `current_lng`         = NULL,
    `current_leg_index`   = 0,
    `current_point_index` = 0,
    `dwell_until`         = NULL,
    `arrival_time`        = NULL
WHERE `trip_id` IN (
    SELECT b.`trip_id`
    FROM `trip_booking` b
    JOIN `student` s ON s.`student_id` = b.`student_id`
    WHERE s.`email` = 'test@mandela.ac.za'
);

-- Clear arrivals so the stop list does not render as already-visited.
UPDATE `trip_stop`
SET `status` = 'PENDING'
WHERE `trip_id` IN (
    SELECT b.`trip_id`
    FROM `trip_booking` b
    JOIN `student` s ON s.`student_id` = b.`student_id`
    WHERE s.`email` = 'test@mandela.ac.za'
);

-- Drop the breadcrumb trail from previous runs, so the next run's history is clean.
DELETE FROM `trip_location_history`
WHERE `trip_id` IN (
    SELECT b.`trip_id`
    FROM `trip_booking` b
    JOIN `student` s ON s.`student_id` = b.`student_id`
    WHERE s.`email` = 'test@mandela.ac.za'
);

-- Bookings are deliberately NOT touched. The student did hold those seats, and the
-- booking staying CONFIRMED is correct; the app decides what to display from the trip's
-- status, not the booking's.

-- ---------------------------------------------------------------------
-- 3. Verify — every row should read SCHEDULED, with NULL position,
--    NULL arrival_time and arrived_stops = 0
-- ---------------------------------------------------------------------
SELECT t.`trip_id`, t.`status` AS trip_status, t.`arrival_time`,
       t.`current_lat`, t.`current_leg_index`,
       COUNT(ts.`id`) AS stops,
       IFNULL(SUM(ts.`status` = 'ARRIVED'), 0) AS arrived_stops,
       t.`departure_stop`, t.`destination_stop`
FROM `trip` t
JOIN `trip_booking` b ON b.`trip_id` = t.`trip_id`
LEFT JOIN `trip_stop` ts ON ts.`trip_id` = t.`trip_id`
WHERE b.`student_id` = (SELECT `student_id` FROM `student` WHERE `email` = 'test@mandela.ac.za')
GROUP BY t.`trip_id`
ORDER BY t.`trip_id`;

-- ---------------------------------------------------------------------
-- 4. Starting a trip during the demo
--
-- Starting must go through the API, not SQL — it precomputes the road route into
-- trip_leg_route and seeds the tracking cursor. Setting status = 'IN_PROGRESS' by hand
-- leaves a trip with no route, and the vehicle will not move.
--
--     POST /api/trips/{tripId}/start
--
-- In the app this is the driver's "Start Trip" button.
-- ---------------------------------------------------------------------
