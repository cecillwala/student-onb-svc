package com.example.student_onb_svc.PersonalDetails;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalDetailsRequest {

    @NotBlank(message = "Gender is required")
    private String gender;

    private String religion;

    @NotBlank(message = "Nationality is required")
    private String nationality;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "County is required")
    private String county;

    @NotBlank(message = "Constituency is required")
    private String constituency;

    @NotBlank(message = "Ward is required")
    private String ward;

    private String postalCode;

    @NotBlank(message = "Guardian first name is required")
    private String guardianFirstName;

    @NotBlank(message = "Guardian last name is required")
    private String guardianLastName;

    @NotBlank(message = "Guardian phone is required")
    private String guardianPhone;

    @Email(message = "Invalid guardian email format")
    private String guardianEmail;

    @NotBlank(message = "Guardian occupation is required")
    private String guardianOccupation;

    @NotBlank(message = "Guardian relationship is required")
    private String guardianRelationship;
}