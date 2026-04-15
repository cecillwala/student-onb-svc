package com.example.student_onb_svc.Common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ApiError {

    private String error;
    private String message;
    private Instant timestamp;

    public static ApiError of(String error, String message) {
        return new ApiError(error, message, Instant.now());
    }
}