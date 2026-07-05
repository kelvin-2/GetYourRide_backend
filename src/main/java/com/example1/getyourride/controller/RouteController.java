package com.example1.getyourride.controller;

import com.example1.getyourride.dto.response.RouteResponse;
import com.example1.getyourride.service.RouteService;
// import com.example1.getyourride.repository.TripRepository; // wire in once shared
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rides")
public class RouteController {

    private final RouteService routeService;
    // private final TripRepository tripRepository;

    public RouteController(RouteService routeService /*, TripRepository tripRepository */) {
        this.routeService = routeService;
        // this.tripRepository = tripRepository;
    }

    @GetMapping("/{rideId}/route")
    public ResponseEntity<RouteResponse> getRoute(@PathVariable Long rideId) {
        // TODO: replace with a real lookup once pickup/destination coordinate
        // columns exist on your Trip entity, e.g.:
        // Trip trip = tripRepository.findById(rideId).orElseThrow();
        // double startLat = trip.getPickupLat(); ... etc.
        double startLat = -33.9581, startLng = 25.6014;   // placeholder pickup
        double endLat = -33.9615, endLng = 25.6089;       // placeholder destination

        RouteResponse route = routeService.getRoute(startLat, startLng, endLat, endLng);
        return ResponseEntity.ok(route);
    }
}