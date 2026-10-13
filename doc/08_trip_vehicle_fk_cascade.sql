-- =====================================================================
-- 08_trip_vehicle_fk_cascade.sql
-- ---------------------------------------------------------------------
-- PROBLEM
--   Driver profile updates fail with MySQL error 1451 / SQLSTATE 23000:
--     "Cannot delete or update a parent row: a foreign key constraint
--      fails `shuttle_db`.`trip`, CONSTRAINT `fk_trip_vehicle`
--      FOREIGN KEY (`registration_number`)
--      REFERENCES `vehicle` (`registration_number`)"
--
--   The trip table references the vehicle by its (mutable) number plate,
--   not by vehicle_id. When a driver edits the registration number,
--   Hibernate runs:
--       UPDATE vehicle SET registration_number=? WHERE vehicle_id=?
--   which MySQL rejects because existing trip rows still hold the old
--   plate and the constraint defaults to ON UPDATE RESTRICT.
--
-- FIX
--   Recreate fk_trip_vehicle with ON UPDATE CASCADE so a plate change on
--   the vehicle automatically rewrites trip.registration_number for every
--   referencing trip. ON DELETE is left as RESTRICT (the default): a
--   vehicle that still has trips must not be silently deleted.
--
-- SAFETY
--   - trip is the only table that references vehicle.registration_number,
--     so this is the only constraint that needs changing.
--   - Run against the application schema (e.g. shuttle_db) while the
--     backend is stopped, or during a quiet window. The ALTERs are quick
--     metadata changes but take brief table locks.
--   - Idempotent-ish: the DROP will error if the constraint is already
--     gone; re-run only the ADD in that case.
-- =====================================================================

ALTER TABLE `trip` DROP FOREIGN KEY `fk_trip_vehicle`;

ALTER TABLE `trip`
  ADD CONSTRAINT `fk_trip_vehicle`
  FOREIGN KEY (`registration_number`)
  REFERENCES `vehicle` (`registration_number`)
  ON UPDATE CASCADE
  ON DELETE RESTRICT;
