package com.example1.getyourride.service.impl;

import com.example1.getyourride.dto.request.VehicleRequest;
import com.example1.getyourride.dto.response.VehicleResponse;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.Vehicle;
import com.example1.getyourride.exception.BadRequestException;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.VehicleRepository;
import com.example1.getyourride.service.VehicleService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleServiceImpl(VehicleRepository vehicleRepository, DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    @Transactional
    public VehicleResponse registerVehicle(VehicleRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with email: " + email));

        if (vehicleRepository.findByRegistrationNumber(request.getRegistrationNumber()).isPresent()) {
            throw new BadRequestException("Vehicle with registration number " + request.getRegistrationNumber() + " already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setModel(request.getModel());
        vehicle.setVehicleYear(request.getVehicleYear());
        vehicle.setColour(request.getColour());
        vehicle.setCapacity(request.getCapacity());
        vehicle.setDriver(driver);

        Vehicle saved = vehicleRepository.save(vehicle);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with email: " + email));

        return vehicleRepository.findByDriverDriverId(driver.getDriverId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .registrationNumber(vehicle.getRegistrationNumber())
                .model(vehicle.getModel())
                .vehicleYear(vehicle.getVehicleYear())
                .colour(vehicle.getColour())
                .capacity(vehicle.getCapacity())
                .driverId(vehicle.getDriver().getDriverId())
                .build();
    }
}
