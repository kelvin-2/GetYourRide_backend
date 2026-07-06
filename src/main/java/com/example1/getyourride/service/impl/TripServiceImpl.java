package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.request.CreateTripRequest;
import com.example1.getyourride.dto.response.TripResponse;
import com.example1.getyourride.dto.response.GeocodeResponse;
import com.example1.getyourride.dto.response.TripStopResponse;
import com.example1.getyourride.entity.*;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.repository.VehicleRepository;
import com.example1.getyourride.service.GeocodingService;
import com.example1.getyourride.service.TripService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of TripService.
 */
@Service
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final GeocodingService geocodingService;

    public TripServiceImpl(TripRepository tripRepository, 
                           DriverRepository driverRepository, 
                           VehicleRepository vehicleRepository,
                           GeocodingService geocodingService) {
        this.tripRepository = tripRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.geocodingService = geocodingService;
    }

    @Override
    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        // Get authenticated driver email from SecurityContext
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Find driver by email
        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with email: " + email));

        // Get vehicles for the driver
        List<Vehicle> vehicles = vehicleRepository.findByDriverDriverId(driver.getDriverId());
        
        // For simplicity, we use the first vehicle found for the driver if one exists.
        // We no longer strictly validate here as requested by user.
        Vehicle vehicle = vehicles.isEmpty() ? null : vehicles.get(0);

        // Create new Trip entity
        Trip trip = new Trip();
        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        trip.setTripType(request.getTripType());
        trip.setDepartureStop(request.getDepartureStop());
        trip.setDestinationStop(request.getDestinationStop());

        // Handle coordinates - either from request or by geocoding the addresses
        if (request.getDepartureLat() != null && request.getDepartureLng() != null) {
            trip.setDepartureLat(request.getDepartureLat());
            trip.setDepartureLng(request.getDepartureLng());
        } else {
            GeocodeResponse departureGeocode = geocodingService.geocode(request.getDepartureStop());
            if (departureGeocode != null && departureGeocode.isFound()) {
                trip.setDepartureLat(departureGeocode.getLatitude());
                trip.setDepartureLng(departureGeocode.getLongitude());
            }
        }

        if (request.getDestinationLat() != null && request.getDestinationLng() != null) {
            trip.setDestinationLat(request.getDestinationLat());
            trip.setDestinationLng(request.getDestinationLng());
        } else {
            GeocodeResponse destinationGeocode = geocodingService.geocode(request.getDestinationStop());
            if (destinationGeocode != null && destinationGeocode.isFound()) {
                trip.setDestinationLat(destinationGeocode.getLatitude());
                trip.setDestinationLng(destinationGeocode.getLongitude());
            }
        }

        trip.setDepartureTime(request.getDepartureTime());
        trip.setAvailableSeats(request.getAvailableSeats());
        trip.setPrice(request.getPrice());
        trip.setStatus("CONFIRMED"); // Default status as requested

        // Handle stops if provided
        if (request.getStops() != null && !request.getStops().isEmpty()) {
            request.getStops().forEach(stopRequest -> {
                TripStop stop = new TripStop();
                stop.setStopName(stopRequest.getStopName());
                stop.setLatitude(stopRequest.getLatitude());
                stop.setLongitude(stopRequest.getLongitude());
                stop.setStopOrder(stopRequest.getStopOrder());
                trip.addStop(stop);
            });
        }

        Trip savedTrip = tripRepository.save(trip);
        return mapToResponse(savedTrip);
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse getTripById(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));
        return mapToResponse(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getAllTrips() {
        return tripRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByStatus(String status) {
        return tripRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TripResponse updateTripStatus(Long tripId, String status) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));
        
        trip.setStatus(status.toUpperCase()); // Normalize status to uppercase
        if ("COMPLETED".equalsIgnoreCase(status)) {
            trip.setArrivalTime(java.time.LocalDateTime.now());
        }
        
        Trip updatedTrip = tripRepository.save(trip);
        return mapToResponse(updatedTrip);
    }

    /**
     * Cancels a trip.
     * Sets the status to CANCELLED.
     */
    @Override
    @Transactional
    public TripResponse cancelTrip(Long tripId) {
        // Find the trip or throw exception
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));
        
        // Update status to CANCELLED
        trip.setStatus("CANCELLED");
        
        // Save and return the updated trip
        return mapToResponse(tripRepository.save(trip));
    }

    /**
     * Completes a trip.
     * Sets the status to COMPLETED and records the current time as arrival time.
     */
    @Override
    @Transactional
    public TripResponse completeTrip(Long tripId) {
        // Find the trip or throw exception
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));
        
        // Update status to COMPLETED
        trip.setStatus("COMPLETED");
        // Record arrival time
        trip.setArrivalTime(java.time.LocalDateTime.now());
        
        // Save and return the updated trip
        return mapToResponse(tripRepository.save(trip));
    }

    /**
     * Schedules a trip.
     * Sets the status to SCHEDULED.
     */
    @Override
    @Transactional
    public TripResponse scheduleTrip(Long tripId) {
        // Find the trip or throw exception
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));
        
        // Update status to SCHEDULED
        trip.setStatus("SCHEDULED");
        
        // Save and return the updated trip
        return mapToResponse(tripRepository.save(trip));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> searchTrips(String departure, String destination) {
        // First try to geocode the addresses to perform a coordinate-based search
        GeocodeResponse depGeocode = geocodingService.geocode(departure);
        GeocodeResponse destGeocode = geocodingService.geocode(destination);

        if (depGeocode.isFound() && destGeocode.isFound()) {
            return searchTripsByCoordinates(
                    depGeocode.getLatitude(), depGeocode.getLongitude(),
                    destGeocode.getLatitude(), destGeocode.getLongitude(),
                    2.0 // Default radius
            );
        }

        // Fallback to text-based search if geocoding fails
        return tripRepository.findByDepartureStopContainingIgnoreCaseAndDestinationStopContainingIgnoreCase(departure, destination)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> searchTripsByCoordinates(Double depLat, Double depLng, Double destLat, Double destLng, Double radiusInKm) {
        // Degree to Km conversion (approximate)
        // 1 degree of latitude is roughly 111km
        // 1 degree of longitude at equator is 111km, but varies by latitude. 
        // For Gqeberha (approx -34 lat), 1 degree lon is roughly 111 * cos(-34) = 92km.
        double latRange = radiusInKm / 111.0;
        double lngRange = radiusInKm / 92.0;

        return tripRepository.findNearbyTrips(
                depLat - latRange, depLat + latRange,
                depLng - lngRange, depLng + lngRange,
                destLat - latRange, destLat + latRange,
                destLng - lngRange, destLng + lngRange,
                "SCHEDULED"
        ).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to map Trip entity to TripResponse DTO.
     */
    private TripResponse mapToResponse(Trip trip) {
        TripResponse.TripResponseBuilder builder = TripResponse.builder()
                .tripId(trip.getTripId())
                .driverId(trip.getDriver().getDriverId())
                .driverName(trip.getDriver().getFirstName() + " " + trip.getDriver().getLastName());

        if (trip.getVehicle() != null) {
            builder.registrationNumber(trip.getVehicle().getRegistrationNumber())
                    .vehicleModel(trip.getVehicle().getModel())
                    .vehicleColour(trip.getVehicle().getColour())
                    .vehicleCapacity(trip.getVehicle().getCapacity());
        }

        return builder
                .tripType(trip.getTripType())
                .departureStop(trip.getDepartureStop())
                .departureLat(trip.getDepartureLat())
                .departureLng(trip.getDepartureLng())
                .destinationStop(trip.getDestinationStop())
                .destinationLat(trip.getDestinationLat())
                .destinationLng(trip.getDestinationLng())
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .availableSeats(trip.getAvailableSeats())
                .price(trip.getPrice())
                .status(trip.getStatus())
                .stops(trip.getStops() != null ? trip.getStops().stream()
                        .map(stop -> TripStopResponse.builder()
                                .id(stop.getId())
                                .stopName(stop.getStopName())
                                .latitude(stop.getLatitude())
                                .longitude(stop.getLongitude())
                                .stopOrder(stop.getStopOrder())
                                .studentId(stop.getStudent() != null ? stop.getStudent().getStudentId() : null)
                                .studentName(stop.getStudent() != null ? stop.getStudent().getFirstName() + " " + stop.getStudent().getLastName() : null)
                                .build())
                        .collect(Collectors.toList()) : null)
                .build();
    }
}
