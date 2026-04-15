package com.example.student_onb_svc.HealthDetails;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HealthDetailsRequest {

    @NotBlank(message = "Blood group is required")
    private String bloodGroup;

    private String preferredHospital;
    private List<String> medicalConditions;  // multi-select → stored as comma-separated
    private List<String> allergies;          // multi-select → stored as comma-separated

    private String insuranceProvider;
    private String policyNumber;

    @NotBlank(message = "Emergency contact first name is required")
    private String emergencyFirstName;

    @NotBlank(message = "Emergency contact last name is required")
    private String emergencyLastName;

    @NotBlank(message = "Emergency contact relationship is required")
    private String emergencyRelationship;

    @NotBlank(message = "Emergency contact phone is required")
    private String emergencyPhone;

    private String emergencyEmail;
}
