package com.example.student_onb_svc.HealthDetails;

import com.example.student_onb_svc.Security.StudentPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding")
public class HealthDetailsController {

    @Autowired
    HealthDetailsService healthDetailsService;

    @GetMapping("/health")
    public ResponseEntity<HealthDetailsRequest> getHealthDetails(
            @RequestParam String token) {
        return ResponseEntity.ok(healthDetailsService.get(token));
    }

    @PostMapping("/health")
    public ResponseEntity<Void> saveHealthDetails(
            @RequestParam String token,
            @Valid @RequestBody HealthDetailsRequest request) {
        healthDetailsService.save(token, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/health")
    public ResponseEntity<Void> updateHealthDetails(
            @RequestParam String token,
            @Valid @RequestBody HealthDetailsRequest request) {
        healthDetailsService.save(token, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/health/medical-report")
    public ResponseEntity<Map<String, String>> uploadMedicalReport(
            @RequestParam String token,
            @RequestParam("file") MultipartFile file) {
        String path = healthDetailsService.uploadMedicalReport(token, file);
        return ResponseEntity.ok(Map.of("path", path));
    }
}
