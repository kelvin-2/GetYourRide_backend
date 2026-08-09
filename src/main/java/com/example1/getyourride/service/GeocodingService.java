package com.example1.getyourride.service;

import com.example1.getyourride.dto.response.AddressSuggestion;
import com.example1.getyourride.dto.response.GeocodeResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Set google.maps.api.key=YOUR_DEMO_KEY in application.properties
    @Value("${google.maps.api.key}")
    private String googleApiKey;

    // Set geoapify.api.key=YOUR_KEY in application.properties (existing fallback)
    @Value("${geoapify.api.key}")
    private String geoapifyApiKey;

    // --- Google endpoints ---
    private static final String GOOGLE_AUTOCOMPLETE_URL = "https://places.googleapis.com/v1/places:autocomplete";
    private static final String GOOGLE_PLACE_DETAILS_URL = "https://places.googleapis.com/v1/places/";
    private static final String GOOGLE_GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    // --- Geoapify endpoints (fallback) ---
    private static final String GEOAPIFY_AUTOCOMPLETE_URL = "https://api.geoapify.com/v1/geocode/autocomplete";
    private static final String GEOAPIFY_SEARCH_URL = "https://api.geoapify.com/v1/geocode/search";
    private static final String GEOAPIFY_REVERSE_URL = "https://api.geoapify.com/v1/geocode/reverse";

    // Nelson Mandela Bay center for bias/proximity so nearby results rank first
    private static final double NMB_LAT = -33.9608;
    private static final double NMB_LON = 25.6022;
    private static final double NMB_BIAS_RADIUS_METERS = 50000.0;
    private static final String NMB_PROXIMITY_GEOAPIFY = "25.6022,-33.9608"; // lon,lat

    private static final Map<String, String> CAMPUS_ALIASES = new LinkedHashMap<>();
    static {
        CAMPUS_ALIASES.put("south campus", "Nelson Mandela University South Campus, Summerstrand, Gqeberha");
        CAMPUS_ALIASES.put("north campus", "Nelson Mandela University North Campus, Summerstrand, Gqeberha");
        CAMPUS_ALIASES.put("second avenue campus", "Nelson Mandela University Second Avenue Campus, Gqeberha");
        CAMPUS_ALIASES.put("bird street", "Bird Street, Gqeberha");
        CAMPUS_ALIASES.put("missionvale", "Missionvale Campus, Nelson Mandela University, Gqeberha");
    }

    // Separate throttles per provider so one doesn't block the other's fallback attempt.
    private final Object googleRateLimitLock = new Object();
    private volatile long lastGoogleRequestMillis = 0;
    private static final long GOOGLE_MIN_INTERVAL_MILLIS = 150;

    private final Object geoapifyRateLimitLock = new Object();
    private volatile long lastGeoapifyRequestMillis = 0;
    private static final long GEOAPIFY_MIN_INTERVAL_MILLIS = 150;

    private void throttleGoogle() {
        long sleepMillis;
        synchronized (googleRateLimitLock) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastGoogleRequestMillis;
            sleepMillis = elapsed < GOOGLE_MIN_INTERVAL_MILLIS ? GOOGLE_MIN_INTERVAL_MILLIS - elapsed : 0;
            lastGoogleRequestMillis = now + sleepMillis;
        }
        sleep(sleepMillis);
    }

    private void throttleGeoapify() {
        long sleepMillis;
        synchronized (geoapifyRateLimitLock) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastGeoapifyRequestMillis;
            sleepMillis = elapsed < GEOAPIFY_MIN_INTERVAL_MILLIS ? GEOAPIFY_MIN_INTERVAL_MILLIS - elapsed : 0;
            lastGeoapifyRequestMillis = now + sleepMillis;
        }
        sleep(sleepMillis);
    }

    private void sleep(long millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =========================================================================
    // PUBLIC API — unchanged signatures, so nothing else needs to be touched.
    // Each tries Google first, falls back to Geoapify automatically on failure
    // or empty result. This is the safety net for demo day.
    // =========================================================================

    public GeocodeResponse geocode(String address) {
        GeocodeResponse googleResult = geocodeWithFallbackLadder(address, this::performGeocodeGoogle);
        if (googleResult.isFound()) return googleResult;

        System.out.println("Google geocode found nothing/failed — falling back to Geoapify for: " + address);
        return geocodeWithFallbackLadder(address, this::performGeocodeGeoapify);
    }

    public List<AddressSuggestion> suggest(String partialQuery) {
        if (partialQuery == null || partialQuery.trim().length() < 3) {
            return new ArrayList<>();
        }
        String trimmed = partialQuery.trim();

        List<AddressSuggestion> googleResults = suggestWithProvider(trimmed, this::performAutocompleteGoogle);
        if (!googleResults.isEmpty()) return googleResults;

        System.out.println("Google suggestions empty/failed — falling back to Geoapify for: " + trimmed);
        return suggestWithProvider(trimmed, this::performAutocompleteGeoapify);
    }

    public AddressSuggestion reverseGeocode(double lat, double lon) {
        AddressSuggestion googleResult = reverseGeocodeGoogle(lat, lon);
        if (googleResult != null) return googleResult;

        System.out.println("Google reverse geocode failed — falling back to Geoapify for: " + lat + "," + lon);
        return reverseGeocodeGeoapify(lat, lon);
    }

    // =========================================================================
    // Shared ladder logic (same fallback address variants as before, now
    // reused for whichever provider function is passed in)
    // =========================================================================

    private GeocodeResponse geocodeWithFallbackLadder(String address, Function<String, GeocodeResponse> performGeocode) {
        GeocodeResponse response = performGeocode.apply(address);
        if (response.isFound()) return response;

        if (address.contains("Gqeberha")) {
            response = performGeocode.apply(address.replace("Gqeberha", "Port Elizabeth"));
            if (response.isFound()) return response;
        }

        if (address.toLowerCase().contains("bird street")) {
            response = performGeocode.apply("Bird Street, Port Elizabeth");
            if (response.isFound()) return response;
        }

        if (address.toLowerCase().contains("summerstrand")) {
            response = performGeocode.apply("Summerstrand, Port Elizabeth");
            if (response.isFound()) return response;
        }

        String simpleAddress = address.split(",")[0];
        if (!simpleAddress.equals(address)) {
            response = performGeocode.apply(simpleAddress + ", South Africa");
            if (response.isFound()) return response;
        }

        return GeocodeResponse.notFound();
    }

    private List<AddressSuggestion> suggestWithProvider(String trimmed, AutocompleteFn autocompleteFn) {
        String lower = trimmed.toLowerCase();

        // 0. Known alias short-circuit
        for (Map.Entry<String, String> alias : CAMPUS_ALIASES.entrySet()) {
            if (alias.getKey().startsWith(lower) || lower.startsWith(alias.getKey())) {
                List<AddressSuggestion> aliasResults = autocompleteFn.apply(alias.getValue(), true, true);
                if (!aliasResults.isEmpty()) return aliasResults;
            }
        }

        // 1. Primary: biased toward NMB, country-limited
        List<AddressSuggestion> results = autocompleteFn.apply(trimmed, true, true);
        if (!results.isEmpty()) return results;

        // 2. Loosen: drop the country filter (handles an out-of-town home address)
        results = autocompleteFn.apply(trimmed, true, false);
        if (!results.isEmpty()) return results;

        // 3. Last resort: no bias, no country filter
        return autocompleteFn.apply(trimmed, false, false);
    }

    @FunctionalInterface
    private interface AutocompleteFn {
        List<AddressSuggestion> apply(String query, boolean useBias, boolean useCountryFilter);
    }

    // =========================================================================
    // GOOGLE — Places API (New) + classic Geocoding API
    // =========================================================================

    private GeocodeResponse performGeocodeGoogle(String address) {
        String url = UriComponentsBuilder
                .fromHttpUrl(GOOGLE_GEOCODE_URL)
                .queryParam("address", address)
                .queryParam("region", "za")
                .queryParam("components", "country:ZA")
                .queryParam("key", googleApiKey)
                .toUriString();

        throttleGoogle();
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), JsonNode.class
            );
            JsonNode body = response.getBody();
            if (body == null) return GeocodeResponse.notFound();

            String status = body.path("status").asText();
            JsonNode results = body.path("results");
            System.out.println("Google Geocoding status=" + status + " results=" + results.size() + " address=" + address);

            if (!"OK".equals(status) || !results.isArray() || results.isEmpty()) {
                return GeocodeResponse.notFound();
            }

            JsonNode first = results.get(0);
            JsonNode location = first.path("geometry").path("location");
            double lat = location.path("lat").asDouble();
            double lon = location.path("lng").asDouble();
            String matched = first.path("formatted_address").asText();
            return GeocodeResponse.of(lat, lon, matched);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Google Geocoding call FAILED for address=" + address
                    + " — HTTP " + e.getStatusCode() + " — body=" + e.getResponseBodyAsString());
            return GeocodeResponse.notFound();
        } catch (Exception e) {
            System.out.println("Google Geocoding call FAILED for address=" + address + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return GeocodeResponse.notFound();
        }
    }

    /**
     * Google's Autocomplete (New) only returns a placeId + text, not coordinates.
     * To keep the AddressSuggestion contract identical to before (lat/lon populated
     * immediately), each prediction is resolved via Place Details right here,
     * server-side, before returning. Costs a few extra calls per keystroke but
     * keeps the frontend untouched.
     */
    private List<AddressSuggestion> performAutocompleteGoogle(String query, boolean useBias, boolean useCountryFilter) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("input", query);

        if (useBias) {
            ObjectNode locationBias = body.putObject("locationBias");
            ObjectNode circle = locationBias.putObject("circle");
            ObjectNode center = circle.putObject("center");
            center.put("latitude", NMB_LAT);
            center.put("longitude", NMB_LON);
            circle.put("radius", NMB_BIAS_RADIUS_METERS);
        }
        if (useCountryFilter) {
            body.putArray("includedRegionCodes").add("za");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleApiKey);
        headers.set("X-Goog-FieldMask", "suggestions.placePrediction.placeId,suggestions.placePrediction.text");

        System.out.println("Google Places Autocomplete REQUEST body=" + body);

        throttleGoogle();
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    GOOGLE_AUTOCOMPLETE_URL, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class
            );
            JsonNode responseBody = response.getBody();
            JsonNode suggestionsNode = responseBody != null ? responseBody.path("suggestions") : null;
            if (suggestionsNode == null || !suggestionsNode.isArray() || suggestionsNode.isEmpty()) {
                System.out.println("Google Places Autocomplete: no suggestions — query=" + query);
                return new ArrayList<>();
            }

            List<AddressSuggestion> suggestions = new ArrayList<>();
            for (JsonNode s : suggestionsNode) {
                JsonNode prediction = s.path("placePrediction");
                if (!prediction.has("placeId") || !prediction.has("text")) continue;

                String placeId = prediction.get("placeId").asText();
                String text = prediction.path("text").path("text").asText();

                GeocodeResponse resolved = resolvePlaceGoogle(placeId);
                if (!resolved.isFound()) continue; // skip predictions we can't resolve coords for

                suggestions.add(new AddressSuggestion(text, resolved.getLatitude(), resolved.getLongitude()));
            }

            System.out.println("Google Places Autocomplete resolved=" + suggestions.size() + " query=" + query);
            return suggestions;
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            System.out.println("Google Places rate-limited us (429) — query=" + query + " — body=" + e.getResponseBodyAsString());
            return new ArrayList<>();
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            System.out.println("Google Places rejected the API key (401) — check google.maps.api.key — query=" + query + " — body=" + e.getResponseBodyAsString());
            return new ArrayList<>();
        } catch (org.springframework.web.client.HttpClientErrorException.Forbidden e) {
            // Demo keys commonly fail here: API not enabled, referrer-restricted, or not valid for server-side use.
            System.out.println("Google Places FORBIDDEN (403) — query=" + query + " — body=" + e.getResponseBodyAsString());
            return new ArrayList<>();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Google Places FAILED — HTTP " + e.getStatusCode() + " — query=" + query + " — body=" + e.getResponseBodyAsString());
            return new ArrayList<>();
        } catch (Exception e) {
            System.out.println("Google Places Autocomplete FAILED — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private GeocodeResponse resolvePlaceGoogle(String placeId) {
        String url = GOOGLE_PLACE_DETAILS_URL + placeId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Goog-Api-Key", googleApiKey);
        headers.set("X-Goog-FieldMask", "location,formattedAddress");

        throttleGoogle();
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class
            );
            JsonNode body = response.getBody();
            if (body == null || !body.has("location")) return GeocodeResponse.notFound();

            double lat = body.path("location").path("latitude").asDouble();
            double lon = body.path("location").path("longitude").asDouble();
            String formatted = body.path("formattedAddress").asText();
            return GeocodeResponse.of(lat, lon, formatted);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Google Place Details FAILED for placeId=" + placeId
                    + " — HTTP " + e.getStatusCode() + " — body=" + e.getResponseBodyAsString());
            return GeocodeResponse.notFound();
        } catch (Exception e) {
            System.out.println("Google Place Details FAILED for placeId=" + placeId + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return GeocodeResponse.notFound();
        }
    }

    private AddressSuggestion reverseGeocodeGoogle(double lat, double lon) {
        String url = UriComponentsBuilder
                .fromHttpUrl(GOOGLE_GEOCODE_URL)
                .queryParam("latlng", lat + "," + lon)
                .queryParam("key", googleApiKey)
                .toUriString();

        throttleGoogle();
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), JsonNode.class
            );
            JsonNode body = response.getBody();
            JsonNode results = body != null ? body.path("results") : null;
            if (results == null || !results.isArray() || results.isEmpty()) {
                return null;
            }
            String formatted = results.get(0).path("formatted_address").asText();
            if (formatted == null || formatted.isBlank()) {
                return null;
            }
            // Use the caller's original lat/lon rather than re-parsing the
            // API's echoed values, since they refer to the exact same point.
            return new AddressSuggestion(formatted, lat, lon);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Google reverse geocode FAILED — HTTP " + e.getStatusCode() + " — body=" + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            System.out.println("Google reverse geocode FAILED — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // GEOAPIFY — original implementation, kept intact as the fallback path
    // =========================================================================

    private GeocodeResponse performGeocodeGeoapify(String address) {
        String url = UriComponentsBuilder
                .fromHttpUrl(GEOAPIFY_SEARCH_URL)
                .queryParam("text", address)
                .queryParam("filter", "countrycode:za")
                .queryParam("limit", 1)
                .queryParam("apiKey", geoapifyApiKey)
                .toUriString();

        try {
            JsonNode[] features = fetchGeoapifyFeatures(url);
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

    private List<AddressSuggestion> performAutocompleteGeoapify(String query, boolean useBias, boolean useCountryFilter) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(GEOAPIFY_AUTOCOMPLETE_URL)
                .queryParam("text", query)
                .queryParam("limit", 5)
                .queryParam("apiKey", geoapifyApiKey);

        if (useBias) {
            builder.queryParam("bias", "proximity:" + NMB_PROXIMITY_GEOAPIFY);
        }
        if (useCountryFilter) {
            builder.queryParam("filter", "countrycode:za");
        }

        JsonNode[] features = fetchGeoapifyFeatures(builder.toUriString());
        return mapGeoapifyToSuggestions(features);
    }

    private List<AddressSuggestion> mapGeoapifyToSuggestions(JsonNode[] features) {
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

    private AddressSuggestion reverseGeocodeGeoapify(double lat, double lon) {
        String url = UriComponentsBuilder
                .fromHttpUrl(GEOAPIFY_REVERSE_URL)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("limit", 1)
                .queryParam("apiKey", geoapifyApiKey)
                .toUriString();

        try {
            JsonNode[] features = fetchGeoapifyFeatures(url);
            if (features.length == 0) {
                return new AddressSuggestion("Current Location", lat, lon);
            }
            JsonNode props = features[0].path("properties");
            if (!props.has("formatted")) {
                return new AddressSuggestion("Current Location", lat, lon);
            }
            return new AddressSuggestion(props.get("formatted").asText(), lat, lon);
        } catch (Exception e) {
            return new AddressSuggestion("Current Location", lat, lon);
        }
    }

    /** Pulls the "features" array out of a Geoapify GeoJSON FeatureCollection response. */
    private JsonNode[] fetchGeoapifyFeatures(String url) {
        throttleGeoapify();
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