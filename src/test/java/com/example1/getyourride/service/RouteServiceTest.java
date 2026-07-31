package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.RouteResponse;
import com.example1.getyourride.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for the OpenRouteService client.
 *
 * <p>Uses {@link MockRestServiceServer} bound to the service's own {@link RestTemplate}, so no
 * real ORS request is made and no API quota is consumed. The template is reached with
 * {@link ReflectionTestUtils} because it is a private field rather than a constructor
 * parameter; that keeps the test from forcing a change to how the bean is wired.
 *
 * <p>Two behaviours are worth locking down. The coordinate order is the first: ORS takes
 * {@code lng,lat} while the rest of the application uses {@code lat,lng}, and getting that
 * backwards silently produces routes in the wrong hemisphere. The second is error handling —
 * before Phase 2 this method only ever saw two hardcoded points, so malformed responses were
 * impossible; now that real coordinates reach it they are not.
 */
class RouteServiceTest {

    private RouteService routeService;
    private MockRestServiceServer mockServer;

    /** A minimal but structurally accurate ORS GeoJSON directions response. */
    private static final String ORS_RESPONSE = """
            {
              "features": [{
                "properties": { "summary": { "distance": 4231.5, "duration": 412.3 } },
                "geometry": {
                  "coordinates": [[25.5858, -33.9758], [25.6000, -33.9800], [25.6750, -33.9984]]
                }
              }]
            }
            """;

    @BeforeEach
    void setUp() {
        routeService = new RouteService();
        ReflectionTestUtils.setField(routeService, "orsApiKey", "test-key");

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(routeService, "restTemplate");
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    @DisplayName("Coordinates are sent to ORS as lng,lat")
    void sendsLngLatToOrs() {
        // The order is inverted relative to everything else in the codebase. Asserting on the
        // outgoing URL is the only way to catch a flip here.
        mockServer.expect(requestTo(containsString("start=25.5858,-33.9758")))
                .andRespond(withSuccess(ORS_RESPONSE, MediaType.APPLICATION_JSON));

        routeService.getRoute(-33.9758, 25.5858, -33.9984, 25.6750);

        mockServer.verify();
    }

    @Test
    @DisplayName("The API key is sent as a query parameter")
    void sendsApiKey() {
        mockServer.expect(requestTo(containsString("api_key=test-key")))
                .andRespond(withSuccess(ORS_RESPONSE, MediaType.APPLICATION_JSON));

        routeService.getRoute(-33.9758, 25.5858, -33.9984, 25.6750);

        mockServer.verify();
    }

    @Test
    @DisplayName("Returned geometry is flipped back to lat,lng with distance and duration")
    void flipsGeometryBackToLatLng() {
        mockServer.expect(requestTo(containsString("directions/driving-car")))
                .andRespond(withSuccess(ORS_RESPONSE, MediaType.APPLICATION_JSON));

        RouteResponse response = routeService.getRoute(-33.9758, 25.5858, -33.9984, 25.6750);

        assertEquals(3, response.getCoordinates().size());
        assertEquals(-33.9758, response.getCoordinates().get(0)[0], 1e-9, "First element must be latitude");
        assertEquals(25.5858, response.getCoordinates().get(0)[1], 1e-9, "Second element must be longitude");
        assertEquals(4231.5, response.getDistanceMeters());
        assertEquals(412.3, response.getDurationSeconds());
    }

    @Test
    @DisplayName("An ORS error response becomes a clear BadRequestException, not a 500")
    void orsErrorBecomesBadRequest() {
        // ORS answers 404 when a point is not near a routable road, which is the most likely
        // real-world failure now that user-supplied coordinates reach this call.
        mockServer.expect(requestTo(containsString("directions/driving-car")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> routeService.getRoute(-33.9758, 25.5858, -33.9984, 25.6750));

        assertTrue(ex.getMessage().toLowerCase().contains("route"), "Actual: " + ex.getMessage());
    }

    @Test
    @DisplayName("A response with no features is reported clearly instead of throwing an NPE")
    void emptyFeaturesReportedClearly() {
        mockServer.expect(requestTo(containsString("directions/driving-car")))
                .andRespond(withSuccess("{\"features\": []}", MediaType.APPLICATION_JSON));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> routeService.getRoute(-33.9758, 25.5858, -33.9984, 25.6750));

        assertTrue(ex.getMessage().contains("No route found"), "Actual: " + ex.getMessage());
    }

    @Test
    @DisplayName("A feature with empty geometry is reported clearly")
    void emptyGeometryReportedClearly() {
        String noGeometry = """
                {"features": [{"properties": {"summary": {"distance": 0, "duration": 0}},
                 "geometry": {"coordinates": []}}]}
                """;
        mockServer.expect(requestTo(containsString("directions/driving-car")))
                .andRespond(withSuccess(noGeometry, MediaType.APPLICATION_JSON));

        assertThrows(BadRequestException.class,
                () -> routeService.getRoute(-33.9758, 25.5858, -33.9984, 25.6750));
    }

    @Test
    @DisplayName("A missing summary yields a zero-length route rather than failing")
    void missingSummaryDefaultsToZero() {
        // ORS omits the summary when start and end resolve to the same road segment. That is a
        // valid zero-length route, so it must not be treated as an error.
        String noSummary = """
                {"features": [{"properties": {},
                 "geometry": {"coordinates": [[25.5858, -33.9758], [25.5859, -33.9759]]}}]}
                """;
        mockServer.expect(requestTo(containsString("directions/driving-car")))
                .andRespond(withSuccess(noSummary, MediaType.APPLICATION_JSON));

        RouteResponse response = routeService.getRoute(-33.9758, 25.5858, -33.9759, 25.5859);

        assertEquals(0.0, response.getDistanceMeters());
        assertEquals(2, response.getCoordinates().size());
    }
}
