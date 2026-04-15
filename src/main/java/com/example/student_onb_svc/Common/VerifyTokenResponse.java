package com.example.student_onb_svc.Common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class VerifyTokenResponse {
    private String fullName;
    private String indexNumber;
    private String regNo;
    private String programme;
    private String department;
    private String faculty;
    private String maskedNationalId;
}