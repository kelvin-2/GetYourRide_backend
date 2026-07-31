package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.RouteResponse;
import com.example1.getyourride.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin client over the OpenRouteService directions API.
 *
 * <p>This is the single ORS integration point in the application. Anything that needs a
 * road-following route goes through here rather than building its own HTTP call, so the key
 * handling, coordinate-order flip and error translation live in one place.
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    // Store in application.properties / an environment variable — never commit it to git.
    // ors.api.key=YOUR_KEY_HERE
    @Value("${ors.api.key}")
    private String orsApiKey;

    public RouteResponse getRoute(double startLat, double startLng, double endLat, double endLng) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.openrouteservice.org/v2/directions/driving-car")
                .queryParam("api_key", orsApiKey)
                // ORS expects lng,lat order (opposite of what you'd expect) — easy bug to hit
                .queryParam("start", startLng + "," + startLat)
                .queryParam("end", endLng + "," + endLat)
                .toUriString();

        JsonNode response;
        try {
            response = restTemplate.getForObject(url, JsonNode.class);
        } catch (RestClientException ex) {
            // Until Phase 2 this method only ever saw two hardcoded placeholder coordinates,
            // so failures were effectively impossible. Now that real trip and stop
            // coordinates reach it, the common failure is ORS refusing a request: a point in
            // the sea or outside the routable network returns 404, an exhausted quota returns
            // 403/429. Those are all reported as a 400 with the ORS reason attached, because
            // the actionable cause is nearly always the coordinates that were supplied.
            // Letting the original exception escape would surface as an opaque 500 instead.
            log.warn("ORS directions request failed for {},{} -> {},{}: {}",
                    startLat, startLng, endLat, endLng, ex.getMessage());
            throw new BadRequestException(
                    "Could not calculate a route between the supplied coordinates: " + ex.getMessage());
        }

        // Guard the response shape before walking it. The previous implementation chained
        // get("features").get(0) straight through, which threw a bare NullPointerException on
        // any unexpected payload and gave no clue what ORS had actually returned.
        if (response == null || !response.has("features") || !response.get("features").isArray()
                || response.get("features").isEmpty()) {
            log.warn("ORS returned no route for {},{} -> {},{}. Payload: {}",
                    startLat, startLng, endLat, endLng, response);
            throw new BadRequestException(
                    "No route found between the supplied coordinates. Check that both points are "
                            + "on land and reachable by road.");
        }

        JsonNode feature = response.get("features").get(0);
        JsonNode summary = feature.path("properties").path("summary");
        JsonNode geometryCoords = feature.path("geometry").path("coordinates");

        if (!geometryCoords.isArray() || geometryCoords.isEmpty()) {
            throw new BadRequestException("ORS returned a route with no geometry for the supplied coordinates.");
        }

        // path() rather than get() so a missing summary yields 0 instead of an NPE. ORS omits
        // the summary when start and end resolve to the same road segment, which is a valid
        // zero-length route rather than an error.
        double distanceMeters = summary.path("distance").asDouble();
        double durationSeconds = summary.path("duration").asDouble();

        List<double[]> path = new ArrayList<>();
        for (JsonNode coordPair : geometryCoords) {
            double lng = coordPair.get(0).asDouble();
            double lat = coordPair.get(1).asDouble();
            path.add(new double[]{lat, lng}); // flip back to lat,lng for the Android side
        }

        return new RouteResponse(path, distanceMeters, durationSeconds);
    }
}
