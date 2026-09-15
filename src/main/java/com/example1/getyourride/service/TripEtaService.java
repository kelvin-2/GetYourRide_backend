package com.example1.getyourride.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example1.getyourride.dto.response.EtaResponse;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.repository.TripRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Live ETA for an in-progress trip, using Google's Compute Routes API.
 *
 * <p>Deliberately separate from {@link RouteService} (OpenRouteService, which drives the
 * simulated vehicle's path — see {@code TripSimulationServiceImpl}): this calls Google fresh,
 * on demand, from wherever the simulation cursor currently is to the trip's destination. It
 * does not touch the existing leg-precomputation flow at all, so the two routing engines run
 * side by side without either duplicating the other's job — ORS drives the vehicle, Google
 * answers "how long from here".
 *
 * <p>Reuses {@code google.maps.api.key} — the same key already configured for
 * {@link GeocodingService} — so no new credential is introduced.
 */
@Service
public class TripEtaService {

    private static final Logger log = LoggerFactory.getLogger(TripEtaService.class);

    private static final String COMPUTE_ROUTES_URL =
            "https://routes.googleapis.com/directions/v2:computeRoutes";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TripRepository tripRepository;

    // Reused from GeocodingService's key — set once in application.properties.
    @Value("${google.maps.api.key}")
    private String googleApiKey;

    public TripEtaService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    public EtaResponse getEta(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip " + tripId + " not found"));

        // Prefer the live simulated position; fall back to the departure point for a trip that
        // hasn't started tracking yet, so the screen can show an initial ETA before the first tick.
        Double originLat = trip.getCurrentLat() != null ? trip.getCurrentLat() : trip.getDepartureLat();
        Double originLng = trip.getCurrentLng() != null ? trip.getCurrentLng() : trip.getDepartureLng();
        Double destLat = trip.getDestinationLat();
        Double destLng = trip.getDestinationLng();

        if (originLat == null || originLng == null || destLat == null || destLng == null) {
            throw new BadRequestException(
                    "Trip " + tripId + " is missing coordinates needed to calculate an ETA");
        }

        JsonNode response = callComputeRoutes(originLat, originLng, destLat, destLng);

        if (response == null || !response.has("routes") || response.get("routes").isEmpty()) {
            log.warn("Google Compute Routes returned no route for trip {} ETA. Payload: {}", tripId, response);
            throw new BadRequestException("No route found for ETA calculation.");
        }

        JsonNode route = response.get("routes").get(0);
        // Duration comes back as a string like "184s" — strip the trailing "s".
        String durationRaw = route.path("duration").asText("0s");
        double etaSeconds = Double.parseDouble(durationRaw.replace("s", ""));
        Double distanceMeters = route.has("distanceMeters") ? route.get("distanceMeters").asDouble() : null;
        int etaMinutes = (int) Math.ceil(etaSeconds / 60.0);

        return new EtaResponse(tripId, etaSeconds, etaMinutes, distanceMeters);
    }

    private JsonNode callComputeRoutes(double originLat, double originLng, double destLat, double destLng) {
        ObjectNode body = objectMapper.createObjectNode();
        body.putObject("origin").putObject("location").putObject("latLng")
                .put("latitude", originLat).put("longitude", originLng);
        body.putObject("destination").putObject("location").putObject("latLng")
                .put("latitude", destLat).put("longitude", destLng);
        body.put("travelMode", "DRIVE");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleApiKey);
        // Compute Routes returns nothing unless the fields it should send back are named
        // explicitly here.
        headers.set("X-Goog-FieldMask", "routes.duration,routes.distanceMeters");

        HttpEntity<String> request;
        try {
            request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        } catch (Exception ex) {
            throw new BadRequestException("Could not build ETA request: " + ex.getMessage());
        }

        try {
            return restTemplate.postForObject(COMPUTE_ROUTES_URL, request, JsonNode.class);
        } catch (RestClientException ex) {
            log.warn("Google Compute Routes ETA request failed for {},{} -> {},{}: {}",
                    originLat, originLng, destLat, destLng, ex.getMessage());
            throw new BadRequestException("Could not calculate ETA: " + ex.getMessage());
        }
    }
}