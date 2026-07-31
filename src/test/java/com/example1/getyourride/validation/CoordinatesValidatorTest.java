package com.example1.getyourride.validation;

import com.example1.getyourride.dto.request.TripStopRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link ValidCoordinates} constraint as applied to
 * {@link TripStopRequest}.
 *
 * <p>Deliberately a plain JUnit test driving the Bean Validation API directly rather than
 * a {@code @SpringBootTest}: the constraint has no Spring dependencies, and the existing
 * {@code @SpringBootTest} classes in this project require a reachable MySQL instance, so
 * they cannot run in environments without one. Keeping this test context-free means the
 * 0,0 regression is guarded on every build.
 */
class CoordinatesValidatorTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    private TripStopRequest stop(Double latitude, Double longitude) {
        return new TripStopRequest("Walmer, 6th Avenue", latitude, longitude, 1);
    }

    private Set<String> violatedPaths(TripStopRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("A real Gqeberha coordinate pair passes")
    void acceptsRealCoordinates() {
        assertTrue(validator.validate(stop(-33.9758, 25.5858)).isEmpty(),
                "A valid coordinate pair must not produce violations");
    }

    @Test
    @DisplayName("Exactly 0,0 is rejected on both coordinate fields")
    void rejectsNullIsland() {
        Set<String> paths = violatedPaths(stop(0.0, 0.0));

        assertTrue(paths.contains("latitude"), "Expected a violation attached to latitude");
        assertTrue(paths.contains("longitude"), "Expected a violation attached to longitude");
    }

    @Test
    @DisplayName("The 0,0 rejection message explains the cause to the caller")
    void nullIslandMessageIsActionable() {
        String message = validator.validate(stop(0.0, 0.0)).iterator().next().getMessage();

        assertTrue(message.contains("0,0"), "Message should name the offending value: " + message);
    }

    @Test
    @DisplayName("Negative zero and near-zero noise are treated as absent coordinates")
    void rejectsNegativeAndNearZero() {
        // A client serialising an uninitialised double can produce any of these; none is a
        // real street address, so all must be rejected the same way as exact 0,0.
        assertTrue(violatedPaths(stop(-0.0, 0.0)).contains("latitude"));
        assertTrue(violatedPaths(stop(0.0000001, -0.0000001)).contains("latitude"));
    }

    @Test
    @DisplayName("A zero latitude with a real longitude is allowed")
    void allowsSingleZeroComponent() {
        // The equator is a real place. Only the 0,0 pair is the bug signature, so a lone
        // zero component must not be rejected.
        assertTrue(validator.validate(stop(0.0, 25.5858)).isEmpty(),
                "Zero latitude with a valid longitude is a legitimate location");
    }

    @Test
    @DisplayName("Out-of-range latitude and longitude are rejected")
    void rejectsOutOfRange() {
        assertTrue(violatedPaths(stop(91.0, 25.5858)).contains("latitude"));
        assertTrue(violatedPaths(stop(-33.9758, 181.0)).contains("longitude"));
    }

    @Test
    @DisplayName("Swapped lat/lng that exceeds latitude range is caught")
    void rejectsSwappedPair() {
        // Gqeberha is roughly (-33.97, 25.58). Swapping gives a latitude of 25.58 which is
        // in range, so this specific swap is not detectable - but a swap involving a
        // longitude beyond 90 is, and that is worth locking in.
        assertTrue(violatedPaths(stop(150.0, -33.9758)).contains("latitude"));
    }

    @Test
    @DisplayName("Missing coordinates are reported once, by @NotNull only")
    void nullCoordinatesReportedOnlyByNotNull() {
        Set<String> paths = violatedPaths(stop(null, null));

        assertTrue(paths.contains("latitude"));
        assertTrue(paths.contains("longitude"));
        // The class-level validator returns early on nulls so the caller gets one clear
        // "required" message per field instead of two competing messages.
        assertEquals(2, validator.validate(stop(null, null)).size(),
                "Null coordinates should yield exactly one violation per field");
    }
}
