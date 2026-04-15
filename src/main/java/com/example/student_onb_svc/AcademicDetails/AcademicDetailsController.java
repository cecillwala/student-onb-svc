package com.example.student_onb_svc.AcademicDetails;

import com.example.student_onb_svc.Security.StudentPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
public class AcademicDetailsController {

    @Autowired
    AcademicDetailsService academicDetailsService;

    @PostMapping("/academic-details")
    public ResponseEntity<String> saveAcademicDetails(
            @RequestParam String token,
            @AuthenticationPrincipal StudentPrincipal principal,
            @RequestBody AcademicDetailsRequest req){

        try {
            academicDetailsService.saveAcademicDetails(token, req);
            return ResponseEntity.ok("Academic Details saved successfully");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
