package com.example1.getyourride.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example1.getyourride.dto.request.TripStopRequest;
import com.example1.getyourride.dto.response.TripStopResponse;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.entity.Trip;
import com.example1.getyourride.entity.TripStop;
import com.example1.getyourride.exception.ResourceNotFoundException;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.repository.TripRepository;
import com.example1.getyourride.repository.TripStopRepository;
import com.example1.getyourride.service.TripStopService;

@Service
public class TripStopServiceImpl implements TripStopService {

    /**
     * Diagnostic logging for the coordinate-loss investigation. DEBUG-level and therefore
     * off by default, because stop coordinates are effectively student home addresses and
     * do not belong in production logs. Enable with
     * {@code logging.level.com.example1.getyourride.service.impl=DEBUG} when reproducing a
     * client-side coordinate problem.
     */
    private static final Logger log = LoggerFactory.getLogger(TripStopServiceImpl.class);

    private final TripStopRepository tripStopRepository;
    private final TripRepository tripRepository;
    private final StudentRepository studentRepository;

    public TripStopServiceImpl(TripStopRepository tripStopRepository,
                               TripRepository tripRepository,
                               StudentRepository studentRepository) {
        this.tripStopRepository = tripStopRepository;
        this.tripRepository = tripRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional
    public TripStopResponse addStopToTrip(Long tripId, TripStopRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        logReceivedStop("POST /api/trips/{tripId}/stops", tripId, request);

        TripStop stop = new TripStop();
        stop.setTrip(trip);
        stop.setStopName(request.getStopName());
        stop.setLatitude(request.getLatitude());
        stop.setLongitude(request.getLongitude());
        
        // If stopOrder is not provided, add to the end
        if (request.getStopOrder() == null) {
            stop.setStopOrder(trip.getStops().size() + 1);
        } else {
            stop.setStopOrder(request.getStopOrder());
        }

        TripStop savedStop = tripStopRepository.save(stop);
        return mapToResponse(savedStop);
    }

    @Override
    @Transactional
    public TripStopResponse addStudentStopToTrip(Long tripId, TripStopRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with email: " + email));

        logReceivedStop("POST /api/trips/{tripId}/stops/student", tripId, request);

        TripStop stop = new TripStop();
        stop.setTrip(trip);
        stop.setStudent(student);
        stop.setStopName(request.getStopName());
        stop.setLatitude(request.getLatitude());
        stop.setLongitude(request.getLongitude());
        
        // Student stops are typically pick-ups, maybe we add them at a specific order
        // For now, just add to the end
        stop.setStopOrder(trip.getStops().size() + 1);

        TripStop savedStop = tripStopRepository.save(stop);
        return mapToResponse(savedStop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripStopResponse> getStopsByTrip(Long tripId) {
        return tripStopRepository.findByTripTripIdOrderByStopOrderAsc(tripId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeStop(Long stopId) {
        if (!tripStopRepository.existsById(stopId)) {
            throw new ResourceNotFoundException("Trip stop not found with id: " + stopId);
        }
        tripStopRepository.deleteById(stopId);
    }

    /**
     * Logs a stop's coordinates exactly as received, before persistence.
     *
     * <p>Lets a developer distinguish a client that never sent coordinates from a backend
     * that discarded them. Guarded by {@code isDebugEnabled} so it is free when disabled.
     */
    private void logReceivedStop(String endpoint, Long tripId, TripStopRequest request) {
        if (!log.isDebugEnabled()) {
            return;
        }
        String payload = request == null ? "null" : String.format("{name=%s, lat=%s, lng=%s, order=%s}",
                request.getStopName(), request.getLatitude(), request.getLongitude(), request.getStopOrder());
        log.debug("{} (trip {}) received {}", endpoint, tripId, payload);
    }

    private TripStopResponse mapToResponse(TripStop stop) {
        return TripStopResponse.builder()
                .id(stop.getId())
                .stopName(stop.getStopName())
                .latitude(stop.getLatitude())
                .longitude(stop.getLongitude())
                .stopOrder(stop.getStopOrder())
                .status(stop.getStatus())
                .studentId(stop.getStudent() != null ? stop.getStudent().getStudentId() : null)
                .studentName(stop.getStudent() != null ? stop.getStudent().getFirstName() + " " + stop.getStudent().getLastName() : null)
                .build();
    }
}
