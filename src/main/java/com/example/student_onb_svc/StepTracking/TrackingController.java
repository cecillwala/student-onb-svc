package com.example.student_onb_svc.StepTracking;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class TrackingController {

    @Autowired
    TrackingService trackingService;

    @GetMapping("/tracking")
    public ResponseEntity<Map<String, Object>> getTracking(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) UUID studentId) {
        if (studentId != null) {
            return ResponseEntity.ok(trackingService.getTrackingByStudentId(studentId));
        }
        if (token != null) {
            return ResponseEntity.ok(trackingService.getTrackingByToken(token));
        }
        throw new IllegalArgumentException("Either token or studentId is required.");
    }


}
