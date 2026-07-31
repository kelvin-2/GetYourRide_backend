package com.example1.getyourride.validation;

import com.example1.getyourride.dto.request.CreateTripRequest;
import com.example1.getyourride.dto.request.TripStopRequest;
import com.example1.getyourride.exception.GlobalExceptionHandler;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the full path from a constraint violation to the field names a client sees.
 *
 * <p><b>Why this test exists separately:</b> raw Bean Validation property paths are not
 * what the API returns. Spring's {@link SpringValidatorAdapter} converts violations into
 * {@link FieldError}s, and {@link GlobalExceptionHandler} builds its 400 body from those.
 * Two things could silently break that chain:
 * <ul>
 *   <li>A class-level constraint such as {@link ValidCoordinates} that forgot to call
 *       {@code addPropertyNode} produces a global error, not a field error, and would have
 *       returned a 400 with an empty body.</li>
 *   <li>Container-element paths such as {@code stops[0].<list element>} need to collapse to
 *       a usable field name.</li>
 * </ul>
 * Exercising {@code SpringValidatorAdapter} directly reproduces exactly what Spring MVC
 * does for an {@code @Valid @RequestBody} argument, with no application context or
 * database, so this runs on every build.
 */
class ValidationErrorMappingTest {

    private static ValidatorFactory validatorFactory;
    private static SpringValidatorAdapter springValidator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        springValidator = new SpringValidatorAdapter(validator);
    }

    @AfterAll
    static void tearDownValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

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

    /** Mirrors how Spring MVC validates an {@code @Valid @RequestBody} argument. */
    private BeanPropertyBindingResult validate(CreateTripRequest request) {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(request, "createTripRequest");
        springValidator.validate(request, bindingResult);
        return bindingResult;
    }

    /** The map GlobalExceptionHandler would place in the 400 response body. */
    private Map<String, String> responseBody(BeanPropertyBindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                        (first, second) -> first));
    }

    @Test
    @DisplayName("A 0,0 nested stop surfaces as latitude and longitude field errors")
    void nullIslandBecomesFieldErrors() {
        BeanPropertyBindingResult result = validate(requestWithStops(
                Collections.singletonList(new TripStopRequest("Somewhere", 0.0, 0.0, 1))));

        Map<String, String> body = responseBody(result);

        assertTrue(body.containsKey("stops[0].latitude"), "Actual body: " + body);
        assertTrue(body.containsKey("stops[0].longitude"), "Actual body: " + body);
        assertTrue(body.get("stops[0].latitude").contains("0,0"),
                "The message must explain the cause. Actual: " + body.get("stops[0].latitude"));
    }

    @Test
    @DisplayName("A null stop element surfaces as a field error named stops[0]")
    void nullElementBecomesIndexedFieldError() {
        BeanPropertyBindingResult result = validate(
                requestWithStops(Collections.singletonList(null)));

        Map<String, String> body = responseBody(result);

        // Spring drops synthetic container-element node names (those wrapped in angle
        // brackets), collapsing "stops[0].<list element>" to "stops[0]".
        assertTrue(body.containsKey("stops[0]"), "Actual body: " + body);
    }

    @Test
    @DisplayName("No violation is reported as a global error, so the 400 body is never empty")
    void classLevelConstraintProducesNoGlobalErrors() {
        // This is the regression guard for ValidCoordinates using addPropertyNode. If a
        // future change drops that call, the violation becomes a global error and the
        // response body loses the field name.
        BeanPropertyBindingResult result = validate(requestWithStops(
                Collections.singletonList(new TripStopRequest("Somewhere", 0.0, 0.0, 1))));

        assertTrue(result.getGlobalErrors().isEmpty(),
                "Coordinate violations must be field-scoped, not global. Actual: "
                        + result.getGlobalErrors());
        assertFalse(responseBody(result).isEmpty(), "The 400 body must never be empty");
    }

    @Test
    @DisplayName("Every field error carries a non-blank message")
    void allFieldErrorsHaveMessages() {
        BeanPropertyBindingResult result = validate(requestWithStops(Arrays.asList(
                new TripStopRequest("Somewhere", 0.0, 0.0, 1),
                null,
                new TripStopRequest("  ", null, null, 3))));

        assertFalse(result.getFieldErrors().isEmpty());
        assertTrue(result.getFieldErrors().stream()
                        .allMatch(e -> e.getDefaultMessage() != null && !e.getDefaultMessage().isBlank()),
                "Every field error needs a message for the client to act on");
    }

    @Test
    @DisplayName("A fully valid request produces no errors")
    void validRequestProducesNoErrors() {
        BeanPropertyBindingResult result = validate(requestWithStops(Collections.singletonList(
                new TripStopRequest("Walmer, 6th Avenue", -33.9758, 25.5858, 1))));

        assertFalse(result.hasErrors(), "Actual errors: " + result.getAllErrors());
    }
}
