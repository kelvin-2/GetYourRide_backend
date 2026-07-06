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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Nominatim asks that you identify your app — required, they will block generic/browser user agents.
    @Value("${app.name:GetYourRide}")
    private String appName;

    // Rough bounding box around Gqeberha/Nelson Mandela Bay — adjust to your actual service area
    private static final String VIEWBOX = "25.30,-33.75,25.95,-34.15";

    // Informal/colloquial names students actually type, mapped to something Nominatim can resolve.
    // Add to this as you discover more terms that return empty results.
    private static final Map<String, String> CAMPUS_ALIASES = new LinkedHashMap<>();
    static {
        CAMPUS_ALIASES.put("south campus", "Nelson Mandela University South Campus, Summerstrand, Gqeberha");
        CAMPUS_ALIASES.put("north campus", "Nelson Mandela University North Campus, Summerstrand, Gqeberha");
        CAMPUS_ALIASES.put("second avenue campus", "Nelson Mandela University Second Avenue Campus, Gqeberha");
        CAMPUS_ALIASES.put("bird street", "Bird Street, Gqeberha");
        CAMPUS_ALIASES.put("missionvale", "Missionvale Campus, Nelson Mandela University, Gqeberha");
    }

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
     * Tries a tightly-bounded search first (fast, precise, avoids "Kilkenny in Ireland"
     * type mismatches), then progressively loosens if that comes back empty —
     * because informal names like "south campus" often aren't tagged with OSM
     * geometry that falls neatly inside our box.
     */
    public List<AddressSuggestion> suggest(String partialQuery) {
        if (partialQuery == null || partialQuery.trim().length() < 3) {
            return new ArrayList<>(); // avoid firing Nominatim on 1-2 char input
        }

        String trimmed = partialQuery.trim();
        String lower = trimmed.toLowerCase();

        // 0. Known alias short-circuit — if the student's query matches (or starts to match)
        //    a known informal campus name, search the resolved version directly.
        for (Map.Entry<String, String> alias : CAMPUS_ALIASES.entrySet()) {
            if (alias.getKey().startsWith(lower) || lower.startsWith(alias.getKey())) {
                List<AddressSuggestion> aliasResults = performSuggest(alias.getValue(), false);
                if (!aliasResults.isEmpty()) {
                    return aliasResults;
                }
            }
        }

        // 1. Fast path: bounded search, exact query
        List<AddressSuggestion> results = performSuggest(trimmed, true);
        if (!results.isEmpty()) {
            return results;
        }

        // 2. Loosen: drop "bounded", keep viewbox as a soft bias instead of a hard filter
        results = performSuggest(trimmed, false);
        if (!results.isEmpty()) {
            return results;
        }

        // 3. Add city context for informal/local names that need disambiguation
        return performSuggest(trimmed + ", Gqeberha", false);
    }

    private List<AddressSuggestion> performSuggest(String query, boolean bounded) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("limit", 5)
                .queryParam("viewbox", VIEWBOX);

        if (bounded) {
            builder.queryParam("bounded", 1);
        }

        JsonNode[] results = fetchResults(builder.toUriString());

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

        try {
            ResponseEntity<JsonNode[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode[].class
            );
            JsonNode[] body = response.getBody();
            return body != null ? body : new JsonNode[0];
        } catch (Exception e) {
            return new JsonNode[0];
        }
    }
}