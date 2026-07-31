-- =====================================================================
-- 03_restore_trip_status_enum.sql
-- Repairs trip.status after Hibernate's ddl-auto=update downgraded it.
--
-- WHAT HAPPENED
-- doc/Task Phase 0 set trip.status to
--   ENUM('SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL
-- and ticked "trip.status is an ENUM" as an acceptance criterion.
--
-- The Trip entity maps that column as a plain Java String. With
-- spring.jpa.hibernate.ddl-auto=update, Hibernate compares the entity to the
-- schema on every application start, decides a String should be a varchar, and
-- issues:
--   alter table trip modify column status varchar(255) not null
--
-- That statement succeeds, so the ENUM is silently replaced by varchar(255) and
-- the database stops rejecting invalid status values. This is not a one-off --
-- it recurs on every start until either the mapping or ddl-auto changes.
--
-- RUN ORDER
-- Fix the cause first (see below), then run this. Restoring the ENUM while
-- ddl-auto=update is still active only buys you until the next restart.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. Confirm the problem before changing anything
--    Expect: Type = varchar(255) if the downgrade has happened,
--            Type = enum(...)    if it has not.
-- ---------------------------------------------------------------------
SHOW COLUMNS FROM `trip` LIKE 'status';

-- ---------------------------------------------------------------------
-- 1. Check for values the ENUM would reject
--
--    While the column was varchar, any string could be written. Rows holding a
--    value outside the five valid states must be corrected first, or the ALTER
--    below will fail (or silently truncate them to '' under a lax sql_mode).
--    Expect: zero rows.
-- ---------------------------------------------------------------------
SELECT `trip_id`, `status`
FROM `trip`
WHERE `status` NOT IN ('SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED');

-- Normalise casing if the query above returned lowercase or mixed-case rows.
-- The service layer calls status.toUpperCase() on update, but rows written by
-- other paths may not be uppercase.
UPDATE `trip`
SET `status` = UPPER(`status`)
WHERE `status` <> UPPER(`status`);

-- ---------------------------------------------------------------------
-- 2. Restore the ENUM
-- ---------------------------------------------------------------------
ALTER TABLE `trip`
    MODIFY COLUMN `status`
    ENUM('SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED')
    NOT NULL DEFAULT 'SCHEDULED';

-- ---------------------------------------------------------------------
-- 3. Verify
--    Expect: Type = enum('SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED')
-- ---------------------------------------------------------------------
SHOW COLUMNS FROM `trip` LIKE 'status';

-- ---------------------------------------------------------------------
-- FIXING THE CAUSE -- pick one, then re-run this file
--
-- Option A (recommended, and what doc/GetYourRide_Database_Setup.md already
-- specifies): stop Hibernate managing the schema.
--
--     spring.jpa.hibernate.ddl-auto=none
--
--   The schema is maintained by these numbered SQL files anyway, so Hibernate
--   has nothing useful to contribute and every opportunity to do damage. This
--   also silences the two ALTERs it currently retries and fails on every start
--   (trip.registration_number and vehicle.registration_number, both blocked by
--   the fk_trip_vehicle foreign key).
--
-- Option B: map the column as a Java enum so Hibernate stops "correcting" it.
--
--     @Enumerated(EnumType.STRING)
--     @Column(name = "status", nullable = false,
--             columnDefinition = "enum('SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED')")
--     private TripStatus status;
--
--   Type-safe and self-documenting, but it changes the signature of
--   TripService.updateTripStatus and every caller that passes a String, so it
--   is a wider change than it first appears. doc/project-rules.md says existing
--   String status fields should be left alone unless a phase calls for the
--   migration.
--
-- Option A is a one-line config change and addresses the whole class of
-- problem, not just this column.
-- ---------------------------------------------------------------------
