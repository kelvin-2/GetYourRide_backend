USE shuttle_db;

-- Dummy data for testing route search + shuttle assignment
-- Run AFTER the route table and trip.route_id column have been created.
-- Uses existing shuttle_stop, driver, vehicle, and shuttle_time_slot rows already in shuttle_db.

-- ============================================================
-- 1. Seed routes (using real existing shuttle_stop rows)
-- ============================================================

INSERT INTO route (origin_stop_id, destination_stop_id, route_name)
SELECT origin.stop_id, dest.stop_id, CONCAT(origin.stop_name, ' → ', dest.stop_name)
FROM shuttle_stop origin, shuttle_stop dest
WHERE origin.stop_name = 'North Campus' AND dest.stop_name = 'South Campus';

INSERT INTO route (origin_stop_id, destination_stop_id, route_name)
SELECT origin.stop_id, dest.stop_id, CONCAT(origin.stop_name, ' → ', dest.stop_name)
FROM shuttle_stop origin, shuttle_stop dest
WHERE origin.stop_name = 'Korsten' AND dest.stop_name = 'North Campus';

INSERT INTO route (origin_stop_id, destination_stop_id, route_name)
SELECT origin.stop_id, dest.stop_id, CONCAT(origin.stop_name, ' → ', dest.stop_name)
FROM shuttle_stop origin, shuttle_stop dest
WHERE origin.stop_name = 'Summerstrand' AND dest.stop_name = 'Summerstrand Campus';

INSERT INTO route (origin_stop_id, destination_stop_id, route_name)
SELECT origin.stop_id, dest.stop_id, CONCAT(origin.stop_name, ' → ', dest.stop_name)
FROM shuttle_stop origin, shuttle_stop dest
WHERE origin.stop_name = 'Central' AND dest.stop_name = 'North Campus';

-- ============================================================
-- 2. Seed trips covering every test scenario you'll want:
--    - a route+slot with an open shuttle (normal booking path)
--    - a route+slot where one shuttle is full but another has room
--      (tests "first trip with room" assignment logic)
--    - a route+slot where every shuttle is full
--      (tests the "no shuttles available" message)
--    - a route+slot with zero trips scheduled at all
--      (tests the same message from the opposite cause — nothing was ever scheduled)
-- ============================================================

-- Scenario A: North Campus → South Campus, Afternoon 14:30 (slot_id 6)
-- Two shuttles: one with room, one full. Booking should pick the one with room.
INSERT INTO trip
  (driver_id, registration_number, trip_type, slot_id, route_id,
   departure_stop, destination_stop, departure_time, available_seats, price, status)
SELECT
  1, 'ABC 123 EC', 'SHUTTLE', 6, r.route_id,
  'North Campus', 'South Campus', '2026-08-03 14:30:00', 5, 0.00, 'SCHEDULED'
FROM route r
JOIN shuttle_stop o ON r.origin_stop_id = o.stop_id
JOIN shuttle_stop d ON r.destination_stop_id = d.stop_id
WHERE o.stop_name = 'North Campus' AND d.stop_name = 'South Campus';

INSERT INTO trip
  (driver_id, registration_number, trip_type, slot_id, route_id,
   departure_stop, destination_stop, departure_time, available_seats, price, status)
SELECT
  2, 'XYZ 789 EC', 'SHUTTLE', 6, r.route_id,
  'North Campus', 'South Campus', '2026-08-03 14:30:00', 0, 0.00, 'SCHEDULED'
FROM route r
JOIN shuttle_stop o ON r.origin_stop_id = o.stop_id
JOIN shuttle_stop d ON r.destination_stop_id = d.stop_id
WHERE o.stop_name = 'North Campus' AND d.stop_name = 'South Campus';

-- Scenario B: Korsten → North Campus, Morning 06:45 (slot_id 1)
-- One shuttle, plenty of room. Simple happy-path booking.
INSERT INTO trip
  (driver_id, registration_number, trip_type, slot_id, route_id,
   departure_stop, destination_stop, departure_time, available_seats, price, status)
SELECT
  1, 'NMU001EC', 'SHUTTLE', 1, r.route_id,
  'Korsten', 'North Campus', '2026-08-03 06:45:00', 12, 0.00, 'SCHEDULED'
FROM route r
JOIN shuttle_stop o ON r.origin_stop_id = o.stop_id
JOIN shuttle_stop d ON r.destination_stop_id = d.stop_id
WHERE o.stop_name = 'Korsten' AND d.stop_name = 'North Campus';

-- Scenario C: Central → North Campus, Afternoon 14:30 (slot_id 6)
-- Only shuttle is completely full. Booking attempt should return
-- "no shuttles currently have available seats" even though a trip exists.
INSERT INTO trip
  (driver_id, registration_number, trip_type, slot_id, route_id,
   departure_stop, destination_stop, departure_time, available_seats, price, status)
SELECT
  2, 'NMU002EC', 'SHUTTLE', 6, r.route_id,
  'Central', 'North Campus', '2026-08-03 14:30:00', 0, 0.00, 'SCHEDULED'
FROM route r
JOIN shuttle_stop o ON r.origin_stop_id = o.stop_id
JOIN shuttle_stop d ON r.destination_stop_id = d.stop_id
WHERE o.stop_name = 'Central' AND d.stop_name = 'North Campus';

-- Scenario D: Summerstrand → Summerstrand Campus, Afternoon 12:30 (slot_id 5)
-- Deliberately NO trip rows inserted here at all — this route+slot exists
-- but nothing is scheduled. Same "no shuttles available" message should
-- appear, but for a different underlying reason (nothing scheduled vs. all full).

-- ============================================================
-- 3. Verify the seeded data
-- ============================================================

SELECT
  r.route_id,
  origin.stop_name AS origin_stop,
  dest.stop_name AS destination_stop,
  t.trip_id,
  t.slot_id,
  t.departure_time,
  t.available_seats,
  t.status
FROM route r
JOIN shuttle_stop origin ON r.origin_stop_id = origin.stop_id
JOIN shuttle_stop dest ON r.destination_stop_id = dest.stop_id
LEFT JOIN trip t ON t.route_id = r.route_id
ORDER BY r.route_id, t.slot_id;
