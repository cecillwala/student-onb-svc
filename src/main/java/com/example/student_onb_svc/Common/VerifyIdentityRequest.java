package com.example.student_onb_svc.Common;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyIdentityRequest {
    @NotBlank(message = "Token is required")
    private String token;
}