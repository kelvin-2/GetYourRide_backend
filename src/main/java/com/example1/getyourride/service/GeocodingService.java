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

    // Set geoapify.api.key=YOUR_KEY in application.properties
    @Value("${geoapify.api.key}")
    private String apiKey;

    private static final String AUTOCOMPLETE_URL = "https://api.geoapify.com/v1/geocode/autocomplete";
    private static final String SEARCH_URL = "https://api.geoapify.com/v1/geocode/search";
    private static final String REVERSE_URL = "https://api.geoapify.com/v1/geocode/reverse";

    // Nelson Mandela Bay bounding box, Geoapify rect format: lon1,lat1,lon2,lat2
    private static final String NMB_RECT = "25.30,-33.75,25.95,-34.15";
    // Rough center of Gqeberha, used for bias=proximity so nearby results rank first
    // without excluding anything further away.
    private static final String NMB_PROXIMITY = "25.6022,-33.9608";

    private static final Map<String, String> CAMPUS_ALIASES = new LinkedHashMap<>();
    static {
        CAMPUS_ALIASES.put("south campus", "Nelson Mandela University South Campus, Summerstrand, Gqeberha");
        CAMPUS_ALIASES.put("north campus", "Nelson Mandela University North Campus, Summerstrand, Gqeberha");
        CAMPUS_ALIASES.put("second avenue campus", "Nelson Mandela University Second Avenue Campus, Gqeberha");
        CAMPUS_ALIASES.put("bird street", "Bird Street, Gqeberha");
        CAMPUS_ALIASES.put("missionvale", "Missionvale Campus, Nelson Mandela University, Gqeberha");
    }

    // Geoapify's free/paid tiers allow far more than Nominatim's 1 req/sec,
    // but a light throttle is still cheap insurance against 429s under burst typing.
    private final Object rateLimitLock = new Object();
    private volatile long lastRequestTimeMillis = 0;
    private static final long MIN_INTERVAL_MILLIS = 150;

    private void throttle() {
        long sleepMillis;
        synchronized (rateLimitLock) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestTimeMillis;
            sleepMillis = elapsed < MIN_INTERVAL_MILLIS ? MIN_INTERVAL_MILLIS - elapsed : 0;
            lastRequestTimeMillis = now + sleepMillis;
        }
        if (sleepMillis > 0) {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Precise single-match geocode — same fallback ladder as before, just
     * hitting Geoapify's /search endpoint instead of Nominatim's.
     */
    public GeocodeResponse geocode(String address) {
        GeocodeResponse response = performGeocode(address);
        if (response.isFound()) return response;

        if (address.contains("Gqeberha")) {
            response = performGeocode(address.replace("Gqeberha", "Port Elizabeth"));
            if (response.isFound()) return response;
        }

        if (address.toLowerCase().contains("bird street")) {
            response = performGeocode("Bird Street, Port Elizabeth");
            if (response.isFound()) return response;
        }

        if (address.toLowerCase().contains("summerstrand")) {
            response = performGeocode("Summerstrand, Port Elizabeth");
            if (response.isFound()) return response;
        }

        String simpleAddress = address.split(",")[0];
        if (!simpleAddress.equals(address)) {
            response = performGeocode(simpleAddress + ", South Africa");
            if (response.isFound()) return response;
        }

        return GeocodeResponse.notFound();
    }

    private GeocodeResponse performGeocode(String address) {
        String url = UriComponentsBuilder
                .fromHttpUrl(SEARCH_URL)
                .queryParam("text", address)
                .queryParam("filter", "countrycode:za")
                .queryParam("limit", 1)
                .queryParam("apiKey", apiKey)
                .toUriString();

        try {
            JsonNode[] features = fetchFeatures(url);
            if (features.length == 0) return GeocodeResponse.notFound();

            JsonNode props = features[0].path("properties");
            double lat = props.get("lat").asDouble();
            double lon = props.get("lon").asDouble();
            String matched = props.get("formatted").asText();
            return GeocodeResponse.of(lat, lon, matched);
        } catch (Exception e) {
            return GeocodeResponse.notFound();
        }
    }

    /**
     * Autocomplete-style suggestions as the student types.
     * Uses bias (soft ranking) rather than a hard rect filter as the primary
     * pass, so addresses outside the exact NMB box still surface — that hard
     * filter was almost certainly why suggestions were missing before.
     */
    public List<AddressSuggestion> suggest(String partialQuery) {
        if (partialQuery == null || partialQuery.trim().length() < 3) {
            return new ArrayList<>();
        }

        String trimmed = partialQuery.trim();
        String lower = trimmed.toLowerCase();

        // 0. Known alias short-circuit
        for (Map.Entry<String, String> alias : CAMPUS_ALIASES.entrySet()) {
            if (alias.getKey().startsWith(lower) || lower.startsWith(alias.getKey())) {
                List<AddressSuggestion> aliasResults = performAutocomplete(alias.getValue(), true, true);
                if (!aliasResults.isEmpty()) return aliasResults;
            }
        }

        // 1. Primary: biased toward NMB, country-limited, no hard box
        List<AddressSuggestion> results = performAutocomplete(trimmed, true, true);
        if (!results.isEmpty()) return results;

        // 2. Loosen: drop the country filter too (handles students typing
        //    an out-of-town home address, e.g. for holiday trip planning)
        results = performAutocomplete(trimmed, true, false);
        if (!results.isEmpty()) return results;

        // 3. Last resort: plain /search endpoint, no bias/filter at all
        return performSearchFallback(trimmed);
    }

    private List<AddressSuggestion> performAutocomplete(String query, boolean useBias, boolean useCountryFilter) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(AUTOCOMPLETE_URL)
                .queryParam("text", query)
                .queryParam("limit", 5)
                .queryParam("apiKey", apiKey);

        if (useBias) {
            builder.queryParam("bias", "proximity:" + NMB_PROXIMITY);
        }
        if (useCountryFilter) {
            builder.queryParam("filter", "countrycode:za");
        }

        JsonNode[] features = fetchFeatures(builder.toUriString());
        return mapToSuggestions(features);
    }

    private List<AddressSuggestion> performSearchFallback(String query) {
        String url = UriComponentsBuilder
                .fromHttpUrl(SEARCH_URL)
                .queryParam("text", query)
                .queryParam("limit", 5)
                .queryParam("apiKey", apiKey)
                .toUriString();

        JsonNode[] features = fetchFeatures(url);
        return mapToSuggestions(features);
    }

    private List<AddressSuggestion> mapToSuggestions(JsonNode[] features) {
        List<AddressSuggestion> suggestions = new ArrayList<>();
        for (JsonNode feature : features) {
            JsonNode props = feature.path("properties");
            if (!props.has("formatted") || !props.has("lat") || !props.has("lon")) continue;
            suggestions.add(new AddressSuggestion(
                    props.get("formatted").asText(),
                    props.get("lat").asDouble(),
                    props.get("lon").asDouble()
            ));
        }
        return suggestions;
    }

    /**
     * Reverse geocode — same shape/behavior as before: falls back to a
     * generic label rather than failing outright.
     */
    public AddressSuggestion reverseGeocode(double lat, double lon) {
        String url = UriComponentsBuilder
                .fromHttpUrl(REVERSE_URL)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("limit", 1)
                .queryParam("apiKey", apiKey)
                .toUriString();

        try {
            JsonNode[] features = fetchFeatures(url);
            if (features.length == 0) {
                return new AddressSuggestion("Current Location", lat, lon);
            }
            JsonNode props = features[0].path("properties");
            if (!props.has("formatted")) {
                return new AddressSuggestion("Current Location", lat, lon);
            }
            // Use the caller's original lat/lon rather than re-parsing the
            // API's echoed values, since they refer to the exact same point.
            return new AddressSuggestion(props.get("formatted").asText(), lat, lon);
        } catch (Exception e) {
            return new AddressSuggestion("Current Location", lat, lon);
        }
    }

    /** Pulls the "features" array out of a Geoapify GeoJSON FeatureCollection response. */
    private JsonNode[] fetchFeatures(String url) {
        throttle();
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), JsonNode.class
            );
            JsonNode body = response.getBody();
            JsonNode featuresNode = body != null ? body.path("features") : null;
            if (featuresNode == null || !featuresNode.isArray()) {
                System.out.println("Geoapify: no features array — url=" + url);
                return new JsonNode[0];
            }
            JsonNode[] features = new JsonNode[featuresNode.size()];
            for (int i = 0; i < featuresNode.size(); i++) {
                features[i] = featuresNode.get(i);
            }
            System.out.println("Geoapify status=" + response.getStatusCode() + " results=" + features.length + " url=" + url);
            return features;
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            System.out.println("Geoapify rate-limited us (429) — url=" + url);
            return new JsonNode[0];
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            System.out.println("Geoapify rejected the API key (401) — check geoapify.api.key — url=" + url);
            return new JsonNode[0];
        } catch (Exception e) {
            System.out.println("Geoapify call FAILED for url=" + url + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return new JsonNode[0];
        }
    }
}