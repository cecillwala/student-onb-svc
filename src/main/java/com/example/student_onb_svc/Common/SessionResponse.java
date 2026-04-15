package com.example.student_onb_svc.Common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class SessionResponse {
    private String sessionToken;
    private UUID studentId;
    private String firstName;
    private String lastName;
    private String regNo;
    private String programme;
    private int currentStep;
    private String status;
}