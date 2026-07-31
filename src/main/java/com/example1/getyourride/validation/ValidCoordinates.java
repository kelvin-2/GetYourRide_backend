package com.example1.getyourride.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint asserting that a {@link HasCoordinates} DTO carries a
 * plausible geographic point.
 *
 * <p><b>Why this exists:</b> {@code trip_stop.latitude} and {@code trip_stop.longitude}
 * are {@code NOT NULL DOUBLE} in the schema, so the database happily accepts
 * {@code 0.0, 0.0}. Before this constraint, a client that lost the picked address
 * suggestion's coordinates would silently persist a stop at Null Island (0,0 - in the
 * Atlantic off West Africa), which then poisoned every downstream route calculation.
 * Rejecting it at the request boundary is the only place the fix belongs: the DB
 * cannot express "not zero" portably, and catching it per-service would mean
 * duplicating the same rule across four write paths.
 *
 * <p>This is deliberately a <em>class-level</em> constraint rather than field-level
 * annotations, because "is this pair of numbers a real place" is a property of the
 * latitude/longitude combination, not of either field alone.
 *
 * <p>Null latitude/longitude are intentionally <em>not</em> reported here - that is
 * {@code @NotNull}'s job on the individual fields. See {@link CoordinatesValidator}.
 */
@Documented
@Constraint(validatedBy = CoordinatesValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCoordinates {

    String message() default "Coordinates are not a valid location";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
