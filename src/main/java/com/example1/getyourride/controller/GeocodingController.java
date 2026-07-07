package com.example1.getyourride.controller;

import com.example1.getyourride.dto.request.GeocodeRequest;
import com.example1.getyourride.dto.response.AddressSuggestion;
import com.example1.getyourride.dto.response.GeocodeResponse;
import com.example1.getyourride.service.GeocodingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geocode")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    // Precise: used once, right before saving a confirmed address's coordinates
    @PostMapping
    public ResponseEntity<GeocodeResponse> geocode(@RequestBody GeocodeRequest request) {
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(geocodingService.geocode(request.getAddress()));
    }

    // Autocomplete: called repeatedly as the student types
    @GetMapping("/suggestions")
    public ResponseEntity<List<AddressSuggestion>> suggestions(@RequestParam("query") String query) {
        return ResponseEntity.ok(geocodingService.suggest(query));
    }

    // Reverse: turns a GPS fix (Current Location) into a readable address,
    // same AddressSuggestion shape as suggestions() so it saves as a stop
    // exactly like any searched address does.
    @GetMapping("/reverse")
    public ResponseEntity<AddressSuggestion> reverse(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon) {
        return ResponseEntity.ok(geocodingService.reverseGeocode(lat, lon));
    }
}