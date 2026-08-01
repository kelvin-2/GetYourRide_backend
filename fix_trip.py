import os
def write_trip_service():
    content = """package com.example1.getyourride.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example1.getyourride.dto.request.BookCarpoolRequest;
import com.example1.getyourride.dto.request.CreateTripRequest;
import com.example1.getyourride.dto.request.OfferRideRequest;
import com.example1.getyourride.dto.request.TripStopRequest;
import com.example1.getyourride.dto.response.GeocodeResponse;
import com.example1.getyourride.dto.response.OfferRideResponse;
import com.example1.getyourride.dto.response.TripResponse;
import com.example1.getyourride.dto.response.TripStopResponse;
import com.example1.getyourride.entity.Booking;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.entity.TripStop;
import com.example1.getyourride.entity.Vehicle;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.repository.BookingRepository;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.repository.VehicleRepository;
import com.example1.getyourride.service.GeocodingService;
import com.example1.getyourride.service.TripService;
import com.example1.getyourride.service.TripSimulationService;

@Service
public class TripServiceImpl implements TripService {

    private static final Logger log = LoggerFactory.getLogger(TripServiceImpl.class);

    private final TripRepository tripRepository;
    private final DriverRepository driverRepository;
    private final StudentRepository studentRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final GeocodingService geocodingService;
    private final TripSimulationService tripSimulationService;

    public TripServiceImpl(TripRepository tripRepository,
                           DriverRepository driverRepository,
                           StudentRepository studentRepository,
                           VehicleRepository vehicleRepository,
                           BookingRepository bookingRepository,
                           GeocodingService geocodingService,
                           TripSimulationService tripSimulationService) {
        this.tripRepository = tripRepository;
        this.driverRepository = driverRepository;
        this.studentRepository = studentRepository;
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.geocodingService = geocodingService;
        this.tripSimulationService = tripSimulationService;
    }

    @Override
    @Transactional
    public OfferRideResponse offerRide(String email, OfferRideRequest request) {
        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with email: " + email));

        if (!Boolean.TRUE.equals(driver.getIsVerified())) {
            throw new BadRequestException("Your account is not yet verified. Please wait for administrator approval.");
        }

        List<Vehicle> vehicles = vehicleRepository.findByDriverDriverId(driver.getDriverId());
        if (vehicles.isEmpty()) {
            throw new BadRequestException("No vehicle linked to driver profile.");
        }
        Vehicle vehicle = vehicles.get(0);

        LocalDateTime departureTime;
        try {
            LocalDate date = LocalDate.parse(request.getRideDate());
            LocalTime time = LocalTime.parse(request.getRideTime());
            departureTime = LocalDateTime.of(date, time);
        } catch (Exception e) {
            throw new BadRequestException("Invalid date or time format provided.");
        }

        if (departureTime.isBefore(LocalDateTime.now().plusMinutes(15))) {
            throw new BadRequestException("Departure time must be scheduled at least 15 minutes in advance.");
        }

        Trip trip = new Trip();
        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        trip.setTripType("Carpool");
        trip.setDepartureStop(request.getPickupLocation());
        trip.setDestinationStop(request.getDestination());
        trip.setDepartureTime(departureTime);
        trip.setAvailableSeats(request.getAvailableSeats());
        trip.setPrice(BigDecimal.valueOf(request.getFarePerSeat()));
        trip.setStatus("SCHEDULED");
        trip.setDepartureLat(request.getPickupLat());
        trip.setDepartureLng(request.getPickupLng());
        trip.setDestinationLat(request.getDestinationLat());
        trip.setDestinationLng(request.getDestinationLng());

        Trip savedTrip = tripRepository.save(trip);

        return new OfferRideResponse(savedTrip.getTripId(), "Ride posted successfully!");
    }

    @Override
    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with email: " + email));

        List<Vehicle> vehicles = vehicleRepository.findByDriverDriverId(driver.getDriverId());
        Vehicle vehicle = vehicles.isEmpty() ? null : vehicles.get(0);

        Trip trip = new Trip();
        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        trip.setTripType(request.getTripType());
        trip.setDepartureStop(request.getDepartureStop());
        trip.setDestinationStop(request.getDestinationStop());

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
        trip.setStatus("CONFIRMED");

        if (request.getStops() != null && !request.getStops().isEmpty()) {
            logReceivedStops("POST /api/trips", request.getStops());
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
    @Transactional
    public TripResponse bookCarpool(Long tripId, BookCarpoolRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        if (trip.getAvailableSeats() <= 0) {
            throw new BadRequestException("No available seats for this trip");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with email: " + email));

        if (log.isDebugEnabled()) {
            log.debug("POST /api/trips/{}/book received pickup={} dropOff={}",
                    tripId, describe(request.getPickupStop()), describe(request.getDropOffStop()));
        }

        TripStop pickupStop = new TripStop();
        pickupStop.setTrip(trip);
        pickupStop.setStudent(student);
        pickupStop.setStopName(request.getPickupStop().getStopName());
        pickupStop.setLatitude(request.getPickupStop().getLatitude());
        pickupStop.setLongitude(request.getPickupStop().getLongitude());
        pickupStop.setStopOrder(trip.getStops().size() + 1);
        trip.addStop(pickupStop);

        if (request.getDropOffStop() != null) {
            TripStop dropOffStop = new TripStop();
            dropOffStop.setTrip(trip);
            dropOffStop.setStudent(student);
            dropOffStop.setStopName(request.getDropOffStop().getStopName());
            dropOffStop.setLatitude(request.getDropOffStop().getLatitude());
            dropOffStop.setLongitude(request.getDropOffStop().getLongitude());
            dropOffStop.setStopOrder(trip.getStops().size() + 1);
            trip.addStop(dropOffStop);
        }

        trip.setAvailableSeats(trip.getAvailableSeats() - 1);
        trip.setStatus("SCHEDULED");

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

        trip.setStatus(status.toUpperCase());
        if ("COMPLETED".equalsIgnoreCase(status)) {
            trip.setArrivalTime(java.time.LocalDateTime.now());
        }

        Trip updatedTrip = tripRepository.save(trip);

        if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            tripSimulationService.startTracking(tripId);
        }

        return mapToResponse(updatedTrip);
    }

    @Override
    @Transactional
    public TripResponse cancelTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        trip.setStatus("CANCELLED");
        return mapToResponse(tripRepository.save(trip));
    }

    @Override
    @Transactional
    public TripResponse completeTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        trip.setStatus("COMPLETED");
        trip.setArrivalTime(java.time.LocalDateTime.now());
        return mapToResponse(tripRepository.save(trip));
    }

    @Override
    @Transactional
    public TripResponse scheduleTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        trip.setStatus("SCHEDULED");
        return mapToResponse(tripRepository.save(trip));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> searchTrips(String departure, String destination, String studentEmail) {
        GeocodeResponse depGeocode = geocodingService.geocode(departure);
        GeocodeResponse destGeocode = geocodingService.geocode(destination);

        boolean includeShuttle = isStudentFunded(studentEmail);

        if (depGeocode.isFound() && destGeocode.isFound()) {
            return searchTripsByCoordinates(
                    depGeocode.getLatitude(), depGeocode.getLongitude(),
                    destGeocode.getLatitude(), destGeocode.getLongitude(),
                    2.0,
                    studentEmail
            );
        }

        return tripRepository.findByDepartureAndDestination(departure, destination, includeShuttle)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> searchTripsByCoordinates(Double depLat, Double depLng, Double destLat, Double destLng, Double radiusInKm, String studentEmail) {
        double latRange = radiusInKm / 111.0;
        double lngRange = radiusInKm / 92.0;

        boolean includeShuttle = isStudentFunded(studentEmail);

        return tripRepository.findNearbyTrips(
                        depLat - latRange, depLat + latRange,
                        depLng - lngRange, depLng + lngRange,
                        destLat - latRange, destLat + latRange,
                        destLng - lngRange, destLng + lngRange,
                        "SCHEDULED",
                        includeShuttle
                ).stream()
                .map(trip -> {
                    TripResponse response = mapToResponse(trip);
                    response.setPickupDistance(calculateHaversineDistance(depLat, depLng, trip.getDepartureLat(), trip.getDepartureLng()));
                    response.setDropOffDistance(calculateHaversineDistance(destLat, destLng, trip.getDestinationLat(), trip.getDestinationLng()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    private boolean isStudentFunded(String email) {
        if (email == null) return false;
        return studentRepository.findByEmail(email)
                .map(Student::getIsFunded)
                .orElse(false);
    }

    private double calculateHaversineDistance(double lat1, double lng1, double lat2, double lng2) {
        if (lat1 == 0 && lng1 == 0 || lat2 == 0 && lng2 == 0) return 0.0;
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void logReceivedStops(String endpoint, List<TripStopRequest> stops) {
        if (!log.isDebugEnabled()) {
            return;
        }
        for (int i = 0; i < stops.size(); i++) {
            log.debug("{} received stops[{}]={}", endpoint, i, describe(stops.get(i)));
        }
    }

    private String describe(TripStopRequest stop) {
        if (stop == null) {
            return "null";
        }
        return String.format("{name=%s, lat=%s, lng=%s, order=%s}",
                stop.getStopName(), stop.getLatitude(), stop.getLongitude(), stop.getStopOrder());
    }

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
                                .status(stop.getStatus())
                                .studentId(stop.getStudent() != null ? stop.getStudent().getStudentId() : null)
                                .studentName(stop.getStudent() != null ? stop.getStudent().getFirstName() + " " + stop.getStudent().getLastName() : null)
                                .build())
                        .collect(Collectors.toList()) : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getMyTrips(String email) {
        List<Trip> tripsAsDriver = new ArrayList<>();
        List<Trip> tripsAsStudent = new ArrayList<>();

        Optional<Driver> driverOpt = driverRepository.findByEmail(email);
        if (driverOpt.isPresent()) {
            tripsAsDriver = tripRepository.findByDriverDriverIdOrderByDepartureTimeDesc(driverOpt.get().getDriverId());
        }

        Optional<Student> studentOpt = studentRepository.findByEmail(email);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            tripsAsStudent.addAll(bookingRepository.findByStudent(student).stream()
                    .map(Booking::getTrip)
                    .collect(Collectors.toList()));
            
            tripsAsStudent.addAll(tripRepository.findTripsByStudentInStops(student.getStudentId()));
        }

        if (!driverOpt.isPresent() && !studentOpt.isPresent()) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        List<Trip> allTrips = new ArrayList<>();
        allTrips.addAll(tripsAsDriver);
        allTrips.addAll(tripsAsStudent);

        return allTrips.stream()
                .distinct()
                .map(this::mapToResponse)
                .sorted(Comparator.comparing(TripResponse::getDepartureTime).reversed())
                .collect(Collectors.toList());
    }
}
"""
    with open('src/main/java/com/example1/getyourride/service/impl/TripServiceImpl.java', 'w', encoding='utf-8') as f:
        f.write(content)

write_trip_service()
