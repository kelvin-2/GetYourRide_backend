package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.AddressSuggestion;
import com.example1.getyourride.dto.response.GeocodeResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Nominatim asks that you identify your app — required, they will block generic/browser user agents.
    @Value("${app.name:GetYourRide}")
    private String appName;

    /**
     * Precise single-match geocode — used when confirming a final address
     * (e.g. right before saving pickup/destination coordinates to a ride).
     */
    public GeocodeResponse geocode(String address) {
        // Try original address first
        GeocodeResponse response = performGeocode(address);
        if (response.isFound()) {
            return response;
        }

        // If not found and contains "Gqeberha", try with "Port Elizabeth"
        if (address.contains("Gqeberha")) {
            String fallbackAddress = address.replace("Gqeberha", "Port Elizabeth");
            response = performGeocode(fallbackAddress);
            if (response.isFound()) {
                return response;
            }
        }
        
        // Specific fallbacks for NMU campuses
        if (address.toLowerCase().contains("bird street")) {
             response = performGeocode("Bird Street, Port Elizabeth");
             if (response.isFound()) return response;
        }
        
        if (address.toLowerCase().contains("summerstrand")) {
            response = performGeocode("Summerstrand, Port Elizabeth");
            if (response.isFound()) return response;
        }

        // Broad search if still not found
        String simpleAddress = address.split(",")[0];
        if (!simpleAddress.equals(address)) {
            response = performGeocode(simpleAddress + ", South Africa");
            if (response.isFound()) return response;
        }

        return GeocodeResponse.notFound();
    }

    private GeocodeResponse performGeocode(String address) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("q", address)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .toUriString();

        try {
            JsonNode[] results = fetchResults(url);
            if (results.length == 0) {
                return GeocodeResponse.notFound();
            }

            JsonNode first = results[0];
            double lat = first.get("lat").asDouble();
            double lon = first.get("lon").asDouble();
            String matched = first.get("display_name").asText();
            return GeocodeResponse.of(lat, lon, matched);
        } catch (Exception e) {
            return GeocodeResponse.notFound();
        }
    }

    /**
     * Autocomplete-style suggestions as the student types.
     * Biased to the Nelson Mandela Bay / Gqeberha area via viewbox+bounded,
     * so "Kilkenny" doesn't return a Kilkenny in Ireland ahead of the local one.
     */
    public List<AddressSuggestion> suggest(String partialQuery) {
        if (partialQuery == null || partialQuery.trim().length() < 3) {
            return new ArrayList<>(); // avoid firing Nominatim on 1-2 char input
        }

        String url = UriComponentsBuilder
                .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("q", partialQuery)
                .queryParam("format", "json")
                .queryParam("limit", 5)
                // Rough bounding box around Gqeberha/Nelson Mandela Bay — adjust to your actual service area
                .queryParam("viewbox", "25.30,-33.75,25.95,-34.15")
                .queryParam("bounded", 1)
                .toUriString();

        JsonNode[] results = fetchResults(url);

        List<AddressSuggestion> suggestions = new ArrayList<>();
        for (JsonNode node : results) {
            suggestions.add(new AddressSuggestion(
                    node.get("display_name").asText(),
                    node.get("lat").asDouble(),
                    node.get("lon").asDouble()
            ));
        }
        return suggestions;
    }

    private JsonNode[] fetchResults(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", appName + " (student capstone project)");

        ResponseEntity<JsonNode[]> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode[].class
        );

        JsonNode[] body = response.getBody();
        return body != null ? body : new JsonNode[0];
    }
}