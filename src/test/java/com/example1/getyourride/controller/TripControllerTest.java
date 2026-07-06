package com.example1.getyourride.controller;

import com.example1.getyourride.dto.request.CreateTripRequest;
import com.example1.getyourride.dto.response.TripResponse;
import com.example1.getyourride.service.TripService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TripService tripService;

    @Test
    @WithMockUser
    public void testGetAllTrips() throws Exception {
        TripResponse trip1 = TripResponse.builder()
                .tripId(1L)
                .driverId(1L)
                .driverName("Sam Driver")
                .registrationNumber("CAA 12345")
                .vehicleModel("Toyota Corolla")
                .vehicleColour("White")
                .vehicleCapacity(4)
                .tripType("STUDENT_DRIVER")
                .status("CONFIRMED")
                .price(new BigDecimal("20.00"))
                .departureStop("Main Campus")
                .destinationStop("North Campus")
                .departureTime(LocalDateTime.now())
                .availableSeats(3)
                .build();

        List<TripResponse> trips = Arrays.asList(trip1);

        Mockito.when(tripService.getAllTrips()).thenReturn(trips);

        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].tripId").value(1L))
                .andExpect(jsonPath("$[0].driverName").value("Sam Driver"))
                .andExpect(jsonPath("$[0].registrationNumber").value("CAA 12345"))
                .andExpect(jsonPath("$[0].vehicleModel").value("Toyota Corolla"))
                .andExpect(jsonPath("$[0].vehicleColour").value("White"))
                .andExpect(jsonPath("$[0].vehicleCapacity").value(4))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser
    public void testCreateTrip() throws Exception {
        CreateTripRequest request = new CreateTripRequest();
        request.setTripType("STUDENT_DRIVER");
        request.setDepartureStop("Main Campus");
        request.setDestinationStop("North Campus");
        request.setDepartureTime(LocalDateTime.now().plusHours(1));
        request.setAvailableSeats(3);
        request.setPrice(new BigDecimal("20.00"));

        TripResponse response = TripResponse.builder()
                .tripId(1L)
                .driverName("Sam Driver")
                .status("CONFIRMED")
                .build();

        Mockito.when(tripService.createTrip(Mockito.any(CreateTripRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(1L))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser
    public void testGetTripById() throws Exception {
        TripResponse trip = TripResponse.builder()
                .tripId(1L)
                .driverName("Sam Driver")
                .tripType("STUDENT_DRIVER")
                .status("CONFIRMED")
                .price(new BigDecimal("20.00"))
                .departureStop("Main Campus")
                .destinationStop("North Campus")
                .departureTime(LocalDateTime.now())
                .availableSeats(3)
                .build();

        Mockito.when(tripService.getTripById(1L)).thenReturn(trip);

        mockMvc.perform(get("/api/trips/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(1L))
                .andExpect(jsonPath("$.driverName").value("Sam Driver"));
    }

    @Test
    @WithMockUser
    public void testGetTripsByStatus() throws Exception {
        TripResponse trip = TripResponse.builder()
                .tripId(1L)
                .driverName("Sam Driver")
                .status("CONFIRMED")
                .build();

        Mockito.when(tripService.getTripsByStatus("CONFIRMED")).thenReturn(Arrays.asList(trip));

        mockMvc.perform(get("/api/trips/status/CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void testGetAllTripsAsStudent() throws Exception {
        TripResponse trip1 = TripResponse.builder()
                .tripId(1L)
                .driverName("Sam Driver")
                .tripType("STUDENT_DRIVER")
                .status("CONFIRMED")
                .build();

        Mockito.when(tripService.getAllTrips()).thenReturn(Arrays.asList(trip1));

        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(1L));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void testGetTripsByStatusAsStudent() throws Exception {
        TripResponse trip = TripResponse.builder()
                .tripId(1L)
                .driverName("Sam Driver")
                .status("COMPLETED")
                .build();

        Mockito.when(tripService.getTripsByStatus("COMPLETED")).thenReturn(Arrays.asList(trip));

        mockMvc.perform(get("/api/trips/status/COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    public void testUpdateTripStatus() throws Exception {
        TripResponse updatedTrip = TripResponse.builder()
                .tripId(1L)
                .status("COMPLETED")
                .arrivalTime(LocalDateTime.now())
                .build();

        Mockito.when(tripService.updateTripStatus(1L, "COMPLETED")).thenReturn(updatedTrip);

        mockMvc.perform(patch("/api/trips/1/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.arrivalTime").exists());
    }

    @Test
    @WithMockUser
    public void testCancelTrip() throws Exception {
        TripResponse cancelledTrip = TripResponse.builder()
                .tripId(1L)
                .status("CANCELLED")
                .build();

        Mockito.when(tripService.cancelTrip(1L)).thenReturn(cancelledTrip);

        mockMvc.perform(patch("/api/trips/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser
    public void testCompleteTrip() throws Exception {
        TripResponse completedTrip = TripResponse.builder()
                .tripId(1L)
                .status("COMPLETED")
                .arrivalTime(LocalDateTime.now())
                .build();

        Mockito.when(tripService.completeTrip(1L)).thenReturn(completedTrip);

        mockMvc.perform(patch("/api/trips/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.arrivalTime").exists());
    }

    @Test
    @WithMockUser
    public void testScheduleTrip() throws Exception {
        TripResponse scheduledTrip = TripResponse.builder()
                .tripId(1L)
                .status("SCHEDULED")
                .build();

        Mockito.when(tripService.scheduleTrip(1L)).thenReturn(scheduledTrip);

        mockMvc.perform(patch("/api/trips/1/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @WithMockUser
    public void testSearchTripsByCoordinates() throws Exception {
        TripResponse tripResponse = TripResponse.builder()
                .tripId(1L)
                .status("SCHEDULED")
                .build();

        Mockito.when(tripService.searchTripsByCoordinates(
                Mockito.anyDouble(), Mockito.anyDouble(), 
                Mockito.anyDouble(), Mockito.anyDouble(), 
                Mockito.anyDouble())).thenReturn(List.of(tripResponse));

        mockMvc.perform(get("/api/trips/search")
                        .param("depLat", "-33.9")
                        .param("depLng", "25.6")
                        .param("destLat", "-34.0")
                        .param("destLng", "25.7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(1L));
    }
}
