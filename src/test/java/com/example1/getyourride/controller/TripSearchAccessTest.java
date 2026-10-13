package com.example1.getyourride.controller;

import com.example1.getyourride.config.SecurityConfig;
import com.example1.getyourride.security.JwtAuthFilter;
import com.example1.getyourride.security.JwtUtil;
import com.example1.getyourride.service.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
@Import(SecurityConfig.class)
public class TripSearchAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(roles = "STUDENT")
    public void studentCanSearchTrips() throws Exception {
        when(tripService.searchTrips(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/trips/search")
                        .param("departure", "Main Campus")
                        .param("destination", "North Campus"))
                .andExpect(status().isOk());
    }

    @Test
    public void unauthenticatedCannotSearchTrips() throws Exception {
        mockMvc.perform(get("/api/trips/search")
                        .param("departure", "Main Campus")
                        .param("destination", "North Campus"))
                .andExpect(status().isUnauthorized());
    }
}
