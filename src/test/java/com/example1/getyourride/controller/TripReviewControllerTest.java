package com.example1.getyourride.controller;

import com.example1.getyourride.config.SecurityConfig;
import com.example1.getyourride.dto.request.TripRatingRequest;
import com.example1.getyourride.entity.TripReview;
import com.example1.getyourride.exception.GlobalExceptionHandler;
import com.example1.getyourride.security.JwtAuthFilter;
import com.example1.getyourride.security.JwtUtil;
import com.example1.getyourride.service.TripReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TripReviewController.class, GlobalExceptionHandler.class})
@Import(SecurityConfig.class)
class TripReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TripReviewService tripReviewService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(username = "student@test.com")
    void rateTrip_Success() throws Exception {
        TripRatingRequest request = new TripRatingRequest(1L, 5, "Excellent!", new ArrayList<>());
        TripReview review = new TripReview();
        review.setReviewId(1L);
        review.setRating(5);
        review.setReview("Excellent!");
        review.setTags(new ArrayList<>());

        when(tripReviewService.rateTrip(any(TripRatingRequest.class), eq("student@test.com")))
                .thenReturn(review);

        mockMvc.perform(post("/api/ratings/rate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(1L))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.review").value("Excellent!"));
    }

    @Test
    @WithMockUser(username = "student@test.com")
    void rateTrip_WithTags_Success() throws Exception {
        String json = "{\"bookingId\":1, \"rating\":5, \"review\":\"Excellent!\", \"tags\":[\"On time\", \"Friendly driver\"]}";
        
        TripReview review = new TripReview();
        review.setReviewId(1L);
        review.setRating(5);
        review.setReview("Excellent!");
        review.setTags(java.util.Arrays.asList("On time", "Friendly driver"));

        when(tripReviewService.rateTrip(any(TripRatingRequest.class), eq("student@test.com")))
                .thenReturn(review);

        mockMvc.perform(post("/api/ratings/rate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").exists())
                .andExpect(jsonPath("$.tags[0]").value("On time"))
                .andExpect(jsonPath("$.tags[1]").value("Friendly driver"));
    }

    @Test
    @WithMockUser(username = "student@test.com")
    void rateTrip_InvalidRequest() throws Exception {
        TripRatingRequest request = new TripRatingRequest(null, 6, "", new ArrayList<>()); // Invalid: null ID, rating > 5, empty review

        mockMvc.perform(post("/api/ratings/rate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
