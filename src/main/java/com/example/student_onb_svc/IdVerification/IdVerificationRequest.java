package com.example.student_onb_svc.IdVerification;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdVerificationRequest {

    @NotBlank(message = "National ID image is required")
    private String nationalId;

    @NotBlank(message = "Selfie image is required")
    private String selfie;
}

