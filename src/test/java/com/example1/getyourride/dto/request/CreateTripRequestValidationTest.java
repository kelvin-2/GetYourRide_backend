package com.example1.getyourride.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the nested-stop validation gap on {@code POST /api/trips}.
 *
 * <p>Before Phase 1, {@code CreateTripRequest.stops} had no {@code @Valid}, so none of the
 * constraints declared on {@link TripStopRequest} ran for this endpoint and stops persisted
 * with 0,0 coordinates. These tests assert the cascade is in place and stays in place -
 * removing the {@code @Valid} or the element-level {@code @NotNull} will fail them.
 *
 * <p>Plain Bean Validation rather than {@code @SpringBootTest} so the suite runs without a
 * database. Spring performs exactly this validation for an {@code @Valid @RequestBody}
 * argument, so a violation here corresponds to a 400 from the controller.
 */
class CreateTripRequestValidationTest {

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

    /** Builds a request that is valid apart from the stops list under test. */
    private CreateTripRequest requestWithStops(List<TripStopRequest> stops) {
        CreateTripRequest request = new CreateTripRequest();
        request.setTripType("Carpool");
        request.setDepartureStop("Walmer, 6th Avenue");
        request.setDestinationStop("South Campus");
        request.setDepartureTime(LocalDateTime.now().plusHours(2));
        request.setAvailableSeats(3);
        request.setPrice(new BigDecimal("25.00"));
        request.setStops(stops);
        return request;
    }

    private Set<String> violatedPaths(CreateTripRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("A trip with valid stops passes validation")
    void acceptsValidStops() {
        CreateTripRequest request = requestWithStops(Collections.singletonList(
                new TripStopRequest("Walmer, 6th Avenue", -33.9758, 25.5858, 1)));

        assertTrue(validator.validate(request).isEmpty(),
                "A fully populated request must not produce violations");
    }

    @Test
    @DisplayName("A trip with no stops passes: stops are optional")
    void acceptsAbsentStops() {
        assertTrue(validator.validate(requestWithStops(null)).isEmpty());
        assertTrue(validator.validate(requestWithStops(Collections.emptyList())).isEmpty());
    }

    @Test
    @DisplayName("A 0,0 stop nested in the list is rejected (the original bug)")
    void rejectsNullIslandStopInList() {
        CreateTripRequest request = requestWithStops(Collections.singletonList(
                new TripStopRequest("Somewhere", 0.0, 0.0, 1)));

        Set<String> paths = violatedPaths(request);

        assertTrue(paths.contains("stops[0].latitude"),
                "Cascade must reach the nested stop's latitude. Actual: " + paths);
        assertTrue(paths.contains("stops[0].longitude"),
                "Cascade must reach the nested stop's longitude. Actual: " + paths);
    }

    @Test
    @DisplayName("Missing coordinates on a nested stop are rejected")
    void rejectsNullCoordinatesInList() {
        CreateTripRequest request = requestWithStops(Collections.singletonList(
                new TripStopRequest("Somewhere", null, null, 1)));

        Set<String> paths = violatedPaths(request);

        assertTrue(paths.contains("stops[0].latitude"), "Actual: " + paths);
        assertTrue(paths.contains("stops[0].longitude"), "Actual: " + paths);
    }

    @Test
    @DisplayName("A null element inside the stops list is rejected")
    void rejectsNullElementInList() {
        // The case called out in the Phase 1 acceptance criteria. @Valid cascades into list
        // elements but skips nulls, so without the element-level @NotNull this payload
        // passed validation and then threw an NPE inside TripServiceImpl.createTrip.
        CreateTripRequest request = requestWithStops(Collections.singletonList(null));

        Set<String> paths = violatedPaths(request);

        // Hibernate Validator reports container-element constraints as
        // "stops[0].<list element>". Asserting on the "stops[0]" prefix keeps this test
        // tied to the behaviour that matters (the element at index 0 is rejected) rather
        // than to the validator's internal node naming. ValidationErrorMappingTest covers
        // the field name the HTTP client actually receives.
        assertTrue(paths.stream().anyMatch(path -> path.startsWith("stops[0]")),
                "A null stop entry must be rejected at the element level. Actual: " + paths);
    }

    @Test
    @DisplayName("A null element mixed with valid stops is still rejected")
    void rejectsNullElementAmongValidStops() {
        CreateTripRequest request = requestWithStops(Arrays.asList(
                new TripStopRequest("Walmer, 6th Avenue", -33.9758, 25.5858, 1),
                null,
                new TripStopRequest("South Campus", -33.9984, 25.6750, 3)));

        Set<String> paths = violatedPaths(request);

        assertTrue(paths.stream().anyMatch(path -> path.startsWith("stops[1]")),
                "The null entry must be identified by index. Actual: " + paths);
        assertTrue(paths.stream().noneMatch(path -> path.startsWith("stops[0]")),
                "The valid stops must not be flagged. Actual: " + paths);
    }

    @Test
    @DisplayName("Every bad stop in a list is reported, not just the first")
    void reportsAllBadStops() {
        CreateTripRequest request = requestWithStops(Arrays.asList(
                new TripStopRequest("Bad one", 0.0, 0.0, 1),
                new TripStopRequest("Bad two", 0.0, 0.0, 2)));

        Set<String> paths = violatedPaths(request);

        assertTrue(paths.contains("stops[0].latitude"), "Actual: " + paths);
        assertTrue(paths.contains("stops[1].latitude"), "Actual: " + paths);
    }

    @Test
    @DisplayName("A blank stop name on a nested stop is rejected")
    void rejectsBlankStopNameInList() {
        // Confirms the cascade activates every TripStopRequest constraint, not only the
        // coordinate ones.
        CreateTripRequest request = requestWithStops(Collections.singletonList(
                new TripStopRequest("  ", -33.9758, 25.5858, 1)));

        assertTrue(violatedPaths(request).contains("stops[0].stopName"));
    }

    @Test
    @DisplayName("Nested violations carry a non-empty message for the 400 response body")
    void nestedViolationsCarryMessages() {
        // GlobalExceptionHandler builds its response body from violation messages, so an
        // empty message would produce a 400 that tells the client nothing.
        CreateTripRequest request = requestWithStops(Collections.singletonList(
                new TripStopRequest("Somewhere", 0.0, 0.0, 1)));

        Set<ConstraintViolation<CreateTripRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().allMatch(v -> v.getMessage() != null && !v.getMessage().isBlank()),
                "Every violation must carry a human-readable message");
    }
}
