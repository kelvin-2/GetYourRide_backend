package com.example1.getyourride.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response returned after a ride is successfully posted.
 */
@Getter
@AllArgsConstructor
public class OfferRideResponse {
    private Long tripId;
    private String message;
}