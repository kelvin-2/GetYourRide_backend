package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.RouteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteService {

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

        JsonNode response = restTemplate.getForObject(url, JsonNode.class);

        JsonNode feature = response.get("features").get(0);
        JsonNode summary = feature.get("properties").get("summary");
        JsonNode geometryCoords = feature.get("geometry").get("coordinates");

        double distanceMeters = summary.get("distance").asDouble();
        double durationSeconds = summary.get("duration").asDouble();

        List<double[]> path = new ArrayList<>();
        for (JsonNode coordPair : geometryCoords) {
            double lng = coordPair.get(0).asDouble();
            double lat = coordPair.get(1).asDouble();
            path.add(new double[]{lat, lng}); // flip back to lat,lng for the Android side
        }

        return new RouteResponse(path, distanceMeters, durationSeconds);
    }
}