package com.example1.getyourride.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the booking write path.
 *
 * <p>{@code TripServiceImpl.bookCarpool} persists both stops on this request as
 * {@code trip_stop} rows, so it was a second route to the same silent 0,0 save as
 * {@code POST /api/trips}. These tests assert the {@code @Valid} cascade on both fields.
 */
class BookCarpoolRequestValidationTest {

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

    private Set<String> violatedPaths(BookCarpoolRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("A booking with a valid pickup stop passes")
    void acceptsValidPickup() {
        BookCarpoolRequest request = new BookCarpoolRequest(
                new TripStopRequest("Walmer, 6th Avenue", -33.9758, 25.5858, 1), null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("A missing pickup stop is rejected")
    void rejectsMissingPickup() {
        assertTrue(violatedPaths(new BookCarpoolRequest(null, null)).contains("pickupStop"));
    }

    @Test
    @DisplayName("A 0,0 pickup stop is rejected")
    void rejectsNullIslandPickup() {
        BookCarpoolRequest request = new BookCarpoolRequest(
                new TripStopRequest("Somewhere", 0.0, 0.0, 1), null);

        Set<String> paths = violatedPaths(request);

        assertTrue(paths.contains("pickupStop.latitude"), "Actual: " + paths);
        assertTrue(paths.contains("pickupStop.longitude"), "Actual: " + paths);
    }

    @Test
    @DisplayName("A 0,0 drop-off stop is rejected when one is supplied")
    void rejectsNullIslandDropOff() {
        BookCarpoolRequest request = new BookCarpoolRequest(
                new TripStopRequest("Walmer, 6th Avenue", -33.9758, 25.5858, 1),
                new TripStopRequest("Somewhere", 0.0, 0.0, 2));

        Set<String> paths = violatedPaths(request);

        assertTrue(paths.contains("dropOffStop.latitude"), "Actual: " + paths);
    }

    @Test
    @DisplayName("An omitted drop-off stop is still allowed")
    void allowsAbsentDropOff() {
        // Students may ride all the way to the driver's stated destination, so dropOffStop
        // stays optional. @Valid must not turn it into a required field.
        BookCarpoolRequest request = new BookCarpoolRequest(
                new TripStopRequest("Walmer, 6th Avenue", -33.9758, 25.5858, 1), null);

        assertTrue(validator.validate(request).isEmpty());
    }
}
