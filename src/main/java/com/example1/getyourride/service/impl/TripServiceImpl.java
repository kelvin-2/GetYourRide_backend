package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.request.CreateTripRequest;
import com.example1.getyourride.dto.response.TripResponse;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.entity.Vehicle;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.repository.VehicleRepository;
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

    public TripServiceImpl(TripRepository tripRepository, 
                           DriverRepository driverRepository, 
                           VehicleRepository vehicleRepository) {
        this.tripRepository = tripRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        // Get authenticated driver email from SecurityContext
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Find driver by email
        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with email: " + email));

        // Find vehicle belonging to this driver
        List<Vehicle> vehicles = vehicleRepository.findByDriverDriverId(driver.getDriverId());
        if (vehicles.isEmpty()) {
            throw new ResourceNotFoundException("No vehicle found for the authenticated driver. Please register a vehicle first.");
        }
        // For simplicity, we use the first vehicle found for the driver
        Vehicle vehicle = vehicles.get(0);

        // Create new Trip entity
        Trip trip = new Trip();
        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        trip.setTripType(request.getTripType());
        trip.setDepartureStop(request.getDepartureStop());
        trip.setDestinationStop(request.getDestinationStop());
        trip.setDepartureTime(request.getDepartureTime());
        trip.setAvailableSeats(request.getAvailableSeats());
        trip.setPrice(request.getPrice());
        trip.setStatus("CONFIRMED"); // Default status as requested

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
        
        trip.setStatus(status);
        if ("COMPLETED".equalsIgnoreCase(status)) {
            trip.setArrivalTime(java.time.LocalDateTime.now());
        }
        
        Trip updatedTrip = tripRepository.save(trip);
        return mapToResponse(updatedTrip);
    }

    /**
     * Helper method to map Trip entity to TripResponse DTO.
     */
    private TripResponse mapToResponse(Trip trip) {
        return TripResponse.builder()
                .tripId(trip.getTripId())
                .driverId(trip.getDriver().getDriverId())
                .driverName(trip.getDriver().getFirstName() + " " + trip.getDriver().getLastName())
                .registrationNumber(trip.getVehicle().getRegistrationNumber())
                .tripType(trip.getTripType())
                .departureStop(trip.getDepartureStop())
                .destinationStop(trip.getDestinationStop())
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .availableSeats(trip.getAvailableSeats())
                .price(trip.getPrice())
                .status(trip.getStatus())
                .build();
    }
}
