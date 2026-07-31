package com.example1.getyourride.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates the {@link ValidCoordinates} constraint.
 *
 * <p>Two rules are enforced:
 * <ol>
 *   <li><b>Earth range.</b> Latitude must be within +/-90 and longitude within +/-180.
 *       Anything outside that is not a coordinate at all, usually a swapped lat/lng pair
 *       or an uninitialised value.</li>
 *   <li><b>Not Null Island.</b> Exactly {@code 0.0, 0.0} is rejected. This is the
 *       signature of a client that failed to copy the selected address suggestion's
 *       coordinates into the outgoing request - a primitive {@code double} defaulting to
 *       zero, or a JSON field serialised as 0 instead of being omitted.</li>
 * </ol>
 *
 * <p><b>Why no regional bounding box:</b> it is tempting to reject anything outside
 * Gqeberha, but {@code GeocodingService} deliberately supports addresses outside the
 * city (students travelling home for holidays), so a regional box would reject
 * legitimate trips. The 0,0 check catches the actual bug without that false-positive
 * risk.
 *
 * <p><b>Why zero is compared with a tolerance:</b> the values arrive as JSON doubles.
 * A client sending {@code 0} and a client sending {@code -0.0} or {@code 1e-15} are all
 * expressing "I have no coordinate", and none of those are within ~100m of any real
 * street address. An exact {@code == 0.0} comparison would let {@code 1e-15} through.
 */
public class CoordinatesValidator implements ConstraintValidator<ValidCoordinates, HasCoordinates> {

    private static final double MAX_ABS_LATITUDE = 90.0;
    private static final double MAX_ABS_LONGITUDE = 180.0;

    /**
     * Anything this close to zero is treated as "absent" rather than a real location.
     * 1e-6 degrees is roughly 0.1m, far below the precision of any geocoded address.
     */
    private static final double ZERO_TOLERANCE = 1e-6;

    private static final String NULL_ISLAND_MESSAGE =
            "Coordinates of 0,0 are not a real location - select an address suggestion "
                    + "so its latitude and longitude are sent with the request";

    @Override
    public boolean isValid(HasCoordinates value, ConstraintValidatorContext context) {
        // A null DTO is @NotNull's concern (see BookCarpoolRequest.pickupStop and the
        // element-level @NotNull on CreateTripRequest.stops). Reporting it here too
        // would stack two messages on the same field.
        if (value == null) {
            return true;
        }

        Double latitude = value.getLatitude();
        Double longitude = value.getLongitude();

        // Likewise, missing individual coordinates are already covered by @NotNull on
        // the latitude/longitude fields themselves.
        if (latitude == null || longitude == null) {
            return true;
        }

        // Collected rather than reported immediately so the default violation is only
        // disabled when there is actually something to replace it with, and so the
        // client receives every problem at once instead of one per round trip.
        Map<String, String> violationsByProperty = new LinkedHashMap<>();

        if (Math.abs(latitude) > MAX_ABS_LATITUDE) {
            violationsByProperty.put("latitude", "Latitude must be between -90 and 90 degrees");
        }
        if (Math.abs(longitude) > MAX_ABS_LONGITUDE) {
            violationsByProperty.put("longitude", "Longitude must be between -180 and 180 degrees");
        }

        // Only worth checking once both values are in range; an out-of-range pair has a
        // more specific message already and is not the 0,0 bug.
        if (violationsByProperty.isEmpty() && isNullIsland(latitude, longitude)) {
            violationsByProperty.put("latitude", NULL_ISLAND_MESSAGE);
            violationsByProperty.put("longitude", NULL_ISLAND_MESSAGE);
        }

        if (violationsByProperty.isEmpty()) {
            return true;
        }

        // Violations are attached to the latitude/longitude property nodes rather than
        // left on the class node. Spring only converts violations that resolve to a
        // property into FieldErrors, and GlobalExceptionHandler builds its 400 body from
        // those - so without addPropertyNode the client would get an empty error body.
        context.disableDefaultConstraintViolation();
        violationsByProperty.forEach((property, message) ->
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(property)
                        .addConstraintViolation());

        return false;
    }

    private boolean isNullIsland(double latitude, double longitude) {
        return Math.abs(latitude) < ZERO_TOLERANCE && Math.abs(longitude) < ZERO_TOLERANCE;
    }
}
