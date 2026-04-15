package com.example.student_onb_svc.AcademicDetails;

import lombok.Data;

import java.util.List;

@Data
public class AcademicDetailsRequest {
    private List<String> extraCurricularActivities;
    private String modeOfStudy;
    private String specialSupportNeeds;
}
