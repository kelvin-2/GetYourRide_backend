package com.example1.getyourride.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarkAsBoardedResponse {
    private boolean success;
    private String message;
    private String boardedAt;
}
