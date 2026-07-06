package com.example1.getyourride.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripStopResponse {
    private Long id;
    private String stopName;
    private Double latitude;
    private Double longitude;
    private Integer stopOrder;
    private Long studentId;
    private String studentName;
}
