-- =====================================================================
-- 05_carpool_tracking_seed.sql
-- Seeds carpool trips that can be tracked end-to-end, so the simulation can be
-- watched without waiting for a real driver to post a ride.
--
-- These are dummy trips in the sense that nobody is really driving them, but they
-- are ordinary rows in shuttle_db read through the ordinary repositories -- there
-- is no separate dummy code path. Switching to "live" data means starting a trip
-- that a real driver posted instead of one of these; nothing in the application
-- changes.
--
-- SCOPE: carpool only. Shuttle trips are excluded on purpose -- all 19
-- shuttle_stop rows have NULL latitude and longitude, and route only records an
-- origin and a destination stop with no intermediate waypoints, so a shuttle trip
-- currently has no coordinates to route through. Backfilling those is separate work.
--
-- WHAT GETS CREATED
--   Trip A  Newton Park -> South Campus        3 stops -> 4 legs
--   Trip B  South Campus -> North Campus       2 stops -> 3 legs
--   Trip C  Newton Park -> Missionvale Campus   0 stops -> 1 leg
--
-- Trip C exists to cover the common case rather than the interesting one: 342 of
-- the 353 trips in this database have no trip_stop rows at all, so a route built
-- only from stop pairs would leave almost every trip untrackable.
--
-- Every coordinate below is copied from a row already present in shuttle_db, so
-- none of them are invented -- they are locations the app has already geocoded.
--
-- Owner: driver 3 (Sipho Mabaso, STUDENT_DRIVER, verified) driving CA123456.
-- Booked by: student 1 (test@mandela.ac.za), the account whose bookings the
-- Android Track tab resolves through GET /api/trips/my-bookings?status=CONFIRMED.
--
-- Run order: after 04_repair_tracking_data.sql.
-- Idempotent: re-running replaces the previously seeded trips rather than adding more.
-- =====================================================================

USE `shuttle_db`;

-- ---------------------------------------------------------------------
-- 0. Parameters
--
-- Departure times are set earlier than the student's existing CONFIRMED bookings
-- (trip 552 on 2026-08-08, trip 555 on 2026-08-09) because the Android client
-- picks the CONFIRMED booking with the soonest departureTime. Without this, the
-- Track tab would keep resolving to trip 552 and these seeded trips would never
-- appear. That selection rule is itself questionable -- an IN_PROGRESS trip should
-- win regardless of departure time -- and is raised in doc/FRONTEND_TRACKING_TODO.md.
-- ---------------------------------------------------------------------
SET @driver_id   := 3;
SET @vehicle_reg := 'CA123456';
SET @student_id  := 1;

SET @depart_a := '2026-08-02 07:15:00';
SET @depart_b := '2026-08-03 07:30:00';
SET @depart_c := '2026-08-04 07:00:00';

-- Preflight. All three must report 1; a 0 means the inserts below would fail on a
-- foreign key, so check this output before reading any further errors.
SELECT
    (SELECT COUNT(*) FROM `driver`  WHERE `driver_id` = @driver_id)             AS driver_found,
    (SELECT COUNT(*) FROM `vehicle` WHERE `registration_number` = @vehicle_reg) AS vehicle_found,
    (SELECT COUNT(*) FROM `student` WHERE `student_id` = @student_id)           AS student_found;

-- ---------------------------------------------------------------------
-- 1. Remove any previous run of this script
--
-- Identified by owner plus the exact seeded departure timestamps, so a real trip
-- posted by the same driver is never caught by this. trip_stop, trip_booking,
-- trip_leg_route and trip_location_history all cascade on trip delete.
-- ---------------------------------------------------------------------
DELETE FROM `trip`
WHERE `driver_id` = @driver_id
  AND `departure_time` IN (@depart_a, @depart_b, @depart_c);

-- ---------------------------------------------------------------------
-- 2. Trip A -- Newton Park -> South Campus, three pickups along the way
--
-- A genuine west-to-east run across Gqeberha: Cape Road, then Walmer, then
-- Humewood, then Summerstrand, ending at NMU South Campus. Every leg is a few
-- kilometres, so OpenRouteService returns a real duration for each and the
-- simulator paces the legs proportionally instead of falling back to a flat step.
-- ---------------------------------------------------------------------
INSERT INTO `trip`
  (`driver_id`, `registration_number`, `trip_type`,
   `departure_stop`, `departure_lat`, `departure_lng`,
   `destination_stop`, `destination_lat`, `destination_lng`,
   `departure_time`, `available_seats`, `price`, `status`,
   `current_leg_index`, `current_point_index`)
VALUES
  (@driver_id, @vehicle_reg, 'Carpool',
   'Newton Park, Cape Road Spar', -33.9457, 25.5661,
   'South Campus', -34.00809, 25.67319,
   @depart_a, 3, 30.00, 'SCHEDULED', 0, 0);

SET @trip_a := LAST_INSERT_ID();

INSERT INTO `trip_stop`
  (`trip_id`, `stop_name`, `latitude`, `longitude`, `stop_order`, `status`, `student_id`)
VALUES
  (@trip_a, 'Walmer, 6th Avenue',                          -33.9758,    25.5858,    1, 'PENDING', @student_id),
  (@trip_a, '15 Killarney Road, Humewood',                 -33.9755999, 25.6405641, 2, 'PENDING', @student_id),
  (@trip_a, 'Summerstrand, Summerbreeze Spar',             -34.0021,    25.6601,    3, 'PENDING', @student_id);

INSERT INTO `trip_booking` (`trip_id`, `student_id`, `booking_status`)
VALUES (@trip_a, @student_id, 'CONFIRMED');

-- ---------------------------------------------------------------------
-- 3. Trip B -- South Campus -> North Campus, the short campus hop
--
-- Deliberately short legs (roughly 1 km each). Useful for watching a whole trip
-- complete quickly, and for checking that the dwell pause at each stop is visible
-- rather than being swamped by a long drive.
-- ---------------------------------------------------------------------
INSERT INTO `trip`
  (`driver_id`, `registration_number`, `trip_type`,
   `departure_stop`, `departure_lat`, `departure_lng`,
   `destination_stop`, `destination_lat`, `destination_lng`,
   `departure_time`, `available_seats`, `price`, `status`,
   `current_leg_index`, `current_point_index`)
VALUES
  (@driver_id, @vehicle_reg, 'Carpool',
   'South Campus', -34.00809, 25.67319,
   'North Campus', -33.9912, 25.6698,
   @depart_b, 3, 25.00, 'SCHEDULED', 0, 0);

SET @trip_b := LAST_INSERT_ID();

INSERT INTO `trip_stop`
  (`trip_id`, `stop_name`, `latitude`, `longitude`, `stop_order`, `status`, `student_id`)
VALUES
  (@trip_b, 'Summerstrand, Summerbreeze Spar', -34.0021, 25.6601, 1, 'PENDING', @student_id),
  (@trip_b, '2nd Avenue Campus',               -33.9914, 25.6569, 2, 'PENDING', @student_id);

INSERT INTO `trip_booking` (`trip_id`, `student_id`, `booking_status`)
VALUES (@trip_b, @student_id, 'CONFIRMED');

-- ---------------------------------------------------------------------
-- 4. Trip C -- Newton Park -> Missionvale Campus, no intermediate stops
--
-- The shape almost every real trip in this database has. Produces a single leg
-- from departure straight to destination. This is the case that the original
-- stop-pairs-only rule rejected outright.
-- ---------------------------------------------------------------------
INSERT INTO `trip`
  (`driver_id`, `registration_number`, `trip_type`,
   `departure_stop`, `departure_lat`, `departure_lng`,
   `destination_stop`, `destination_lat`, `destination_lng`,
   `departure_time`, `available_seats`, `price`, `status`,
   `current_leg_index`, `current_point_index`)
VALUES
  (@driver_id, @vehicle_reg, 'Carpool',
   'Newton Park, Cape Road Spar', -33.9457, 25.5661,
   'Missionvale Campus', -33.87253, 25.55223,
   @depart_c, 3, 35.00, 'SCHEDULED', 0, 0);

SET @trip_c := LAST_INSERT_ID();

INSERT INTO `trip_booking` (`trip_id`, `student_id`, `booking_status`)
VALUES (@trip_c, @student_id, 'CONFIRMED');

-- ---------------------------------------------------------------------
-- 5. What was created -- note these trip ids, they are the ones to start
-- ---------------------------------------------------------------------
SELECT 'Seeded trips' AS info;

SELECT t.`trip_id`, t.`trip_type`, t.`status`,
       t.`departure_stop`, t.`destination_stop`, t.`departure_time`,
       COUNT(ts.`id`)     AS stops,
       COUNT(ts.`id`) + 1 AS expected_legs
FROM `trip` t
LEFT JOIN `trip_stop` ts ON ts.`trip_id` = t.`trip_id`
WHERE t.`trip_id` IN (@trip_a, @trip_b, @trip_c)
GROUP BY t.`trip_id`
ORDER BY t.`trip_id`;

SELECT b.`booking_id`, b.`trip_id`, b.`student_id`, b.`booking_status`, s.`email`
FROM `trip_booking` b
JOIN `student` s ON s.`student_id` = b.`student_id`
WHERE b.`trip_id` IN (@trip_a, @trip_b, @trip_c)
ORDER BY b.`trip_id`;

-- Which trip the Android Track tab will resolve to for this student: the CONFIRMED
-- booking with the soonest departure whose trip is still trackable.
SELECT 'Track tab will follow this trip' AS info, t.`trip_id`, t.`departure_stop`,
       t.`destination_stop`, t.`departure_time`, t.`status`
FROM `trip_booking` b
JOIN `trip` t ON t.`trip_id` = b.`trip_id`
WHERE b.`student_id` = @student_id
  AND b.`booking_status` = 'CONFIRMED'
  AND t.`status` IN ('SCHEDULED','CONFIRMED','IN_PROGRESS')
ORDER BY t.`departure_time`
LIMIT 1;

-- ---------------------------------------------------------------------
-- 6. How to drive one of these
--
--   POST /api/trips/{tripId}/start
--       Precomputes the leg routes if they are missing, sets the trip
--       IN_PROGRESS and parks the vehicle at the departure point.
--       Add ?recomputeRoute=true after changing the trip's stops.
--
--   The scheduler then advances it every 4 seconds, publishing to
--   /topic/trip/{tripId} over STOMP at ws://<host>:8080/ws. Requires
--   getyourride.tracking.simulation.enabled=true.
--
--   Watch it without a client:
--       SELECT trip_id, status, current_lat, current_lng,
--              current_leg_index, current_point_index, dwell_until
--       FROM trip WHERE trip_id = <tripId>;
--
--       SELECT COUNT(*), MAX(recorded_at) FROM trip_location_history
--       WHERE trip_id = <tripId>;
--
--   Reset it to run again:
--       PATCH /api/trips/{tripId}/schedule   then   POST /api/trips/{tripId}/start
--
-- ---------------------------------------------------------------------
-- Rollback -- removes only what this script created
-- ---------------------------------------------------------------------
-- DELETE FROM `trip`
-- WHERE `driver_id` = 3
--   AND `departure_time` IN ('2026-08-02 07:15:00','2026-08-03 07:30:00','2026-08-04 07:00:00');
