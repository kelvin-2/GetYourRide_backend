package com.example1.getyourride.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardedStudentResponse {
    private Long bookingId;
    private Long studentId;
    private String firstName;
    private String lastName;
    private String studentNumber;
    private String bookingStatus;
    private String boardedAt;  // null if not boarded yet
}
