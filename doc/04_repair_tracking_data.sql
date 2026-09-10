-- =====================================================================
-- 04_repair_tracking_data.sql
-- Makes the existing trip data simulatable.
--
-- WHY THIS EXISTS
-- The simulation engine (Phase 4) was complete and unit-tested, but could not
-- move a single vehicle against the real database. Measured on shuttle_db:
--
--   * 353 trips, of which 0 were IN_PROGRESS  -> the scheduler had nothing to pick up
--   * only 11 of 353 trips had departure_lat  -> 342 could not be routed at all
--   * 19 shuttle_stop rows, 0 with lat/lng    -> the shuttle path has no coordinates
--   * trip_leg_route: 0 rows                  -> nothing had ever been precomputed
--   * trip.status was varchar(255), not ENUM  -> Phase 0's constraint had been undone
--   * several trips had stops repeated at identical coordinates
--
-- This file fixes what can be fixed in data. Two changes outside SQL are
-- required as well and are NOT done here:
--   1. spring.jpa.hibernate.ddl-auto=none in application.properties, or step 1
--      below is undone on the next application start (see 03_restore_trip_status_enum.sql).
--   2. Leg routes now span departure -> stops -> destination rather than stop
--      pairs only, which is a code change in TripRouteServiceImpl.
--
-- Run order: after 01, 02 and 03. Idempotent -- safe to run more than once.
-- Run against a copy first, per the convention in the earlier migrations.
-- =====================================================================

USE `shuttle_db`;

-- ---------------------------------------------------------------------
-- 1. Restore the trip.status ENUM
--
-- Repeats 03_restore_trip_status_enum.sql because the downgrade recurs on every
-- application start while ddl-auto=update is set, and it was measured as varchar
-- again on the live database. Harmless if the column is already an ENUM.
-- ---------------------------------------------------------------------
UPDATE `trip` SET `status` = UPPER(`status`) WHERE `status` <> UPPER(`status`);

ALTER TABLE `trip`
    MODIFY COLUMN `status`
    ENUM('SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED')
    NOT NULL DEFAULT 'SCHEDULED';

-- ---------------------------------------------------------------------
-- 2. Remove stops that repeat the location of the stop before them
--
-- Trip 25 held three stops at -34.0021,25.6601; trips 552 and 555 held two each
-- at -34.00809,25.67319. Routing between two points at the same place returns a
-- zero-length route with no duration, and the simulator derives its step size
-- from that duration -- so the vehicle stood still for the length of a leg
-- instead of moving.
--
-- The application now merges such waypoints at route-build time, so this step is
-- about the stored data rather than about making the simulation work: a duplicate
-- stop still renders as a duplicate marker on the client's map and still shows up
-- in the stop list the student sees.
--
-- Keeps the lowest id in each group of identical coordinates on the same trip.
-- Rounded to 5 decimal places (~1 m) so coordinates that differ only in float
-- noise are treated as equal.
-- ---------------------------------------------------------------------
DELETE ts FROM `trip_stop` ts
JOIN (
    SELECT `trip_id`,
           ROUND(`latitude`, 5)  AS lat_key,
           ROUND(`longitude`, 5) AS lng_key,
           MIN(`id`)             AS keep_id
    FROM `trip_stop`
    GROUP BY `trip_id`, lat_key, lng_key
    HAVING COUNT(*) > 1
) dup
  ON  ts.`trip_id` = dup.`trip_id`
  AND ROUND(ts.`latitude`, 5)  = dup.lat_key
  AND ROUND(ts.`longitude`, 5) = dup.lng_key
  AND ts.`id` <> dup.keep_id;

-- Close the gaps the delete left in stop_order. The simulator orders legs by
-- stop_order and treats 0 as the trip's departure, so the sequence has to stay
-- contiguous and start at 1.
SET @trip := NULL;
SET @seq  := 0;

UPDATE `trip_stop` ts
JOIN (
    SELECT `id`,
           @seq := IF(@trip = `trip_id`, @seq + 1, 1) AS new_order,
           @trip := `trip_id`                         AS current_trip
    FROM `trip_stop`
    ORDER BY `trip_id`, `stop_order`, `id`
) renumbered ON renumbered.`id` = ts.`id`
SET ts.`stop_order` = renumbered.new_order
WHERE ts.`stop_order` <> renumbered.new_order;

-- ---------------------------------------------------------------------
-- 3. Clear tracking state on trips that are not running
--
-- current_lat/current_lng on a SCHEDULED trip is left over from an earlier run
-- and would draw a vehicle on the map for a trip that has not departed.
-- ---------------------------------------------------------------------
UPDATE `trip`
SET `current_lat` = NULL,
    `current_lng` = NULL,
    `current_leg_index` = 0,
    `current_point_index` = 0,
    `dwell_until` = NULL
WHERE `status` IN ('SCHEDULED','CONFIRMED','CANCELLED');

-- ---------------------------------------------------------------------
-- 4. Verification -- check these before moving on
-- ---------------------------------------------------------------------

-- Expect: Type = enum('SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED')
SHOW COLUMNS FROM `trip` LIKE 'status';

-- Expect: zero rows. Any row here is a trip whose consecutive stops sit in the
-- same place, which is what made vehicles stall.
SELECT `trip_id`, ROUND(`latitude`, 5) AS lat, ROUND(`longitude`, 5) AS lng, COUNT(*) AS repeats
FROM `trip_stop`
GROUP BY `trip_id`, lat, lng
HAVING COUNT(*) > 1;

-- Expect: every trip's stop_order runs 1..n with no gaps, so gap_count = 0.
SELECT COUNT(*) AS gap_count FROM (
    SELECT `trip_id`, MIN(`stop_order`) AS lo, MAX(`stop_order`) AS hi, COUNT(*) AS n
    FROM `trip_stop`
    GROUP BY `trip_id`
    HAVING lo <> 1 OR hi <> n
) gaps;

-- Trips that are ready to be tracked: they have somewhere to start and somewhere
-- to finish. Everything listed here can be driven by POST /api/trips/{id}/start.
SELECT t.`trip_id`, t.`trip_type`, t.`status`,
       t.`departure_stop`, t.`destination_stop`,
       COUNT(ts.`id`) AS stops,
       COUNT(ts.`id`) + 1 AS expected_legs
FROM `trip` t
LEFT JOIN `trip_stop` ts ON ts.`trip_id` = t.`trip_id`
WHERE t.`departure_lat` IS NOT NULL AND t.`departure_lng` IS NOT NULL
  AND t.`destination_lat` IS NOT NULL AND t.`destination_lng` IS NOT NULL
  AND t.`status` IN ('SCHEDULED','CONFIRMED','IN_PROGRESS')
GROUP BY t.`trip_id`
ORDER BY t.`trip_id`;
