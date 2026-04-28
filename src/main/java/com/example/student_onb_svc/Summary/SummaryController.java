package com.example.student_onb_svc.Summary;

import com.example.student_onb_svc.Security.StudentPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/onboarding")
public class SummaryController {

    @Autowired
    SummaryService summaryService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam String token) {
        return ResponseEntity.ok(summaryService.getSummary(token));
    }

    @PostMapping("/submit")
    public ResponseEntity<UUID> submit(
            @RequestParam String token) {
        UUID studentId = summaryService.submit(token);
        return ResponseEntity.ok().body(studentId);
    }

}
