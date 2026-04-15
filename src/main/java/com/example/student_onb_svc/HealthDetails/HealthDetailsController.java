package com.example.student_onb_svc.HealthDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
public class HealthDetailsController {

    @Autowired
    HealthDetailsService healthDetailsService;

    @PostMapping("/health-details")
    public ResponseEntity<?> saveHealthDetails(@RequestBody HealthDetailsRequest req, @RequestParam String token){

        return ResponseEntity.ok(true);
    }
}
