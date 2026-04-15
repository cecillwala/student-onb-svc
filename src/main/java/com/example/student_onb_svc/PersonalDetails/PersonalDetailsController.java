package com.example.student_onb_svc.PersonalDetails;

import com.example.student_onb_svc.Security.StudentPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
public class PersonalDetailsController {

    @Autowired
    PersonalDetailsService personalDetailsService;
    // ── Step 1: Personal Details (auth required) ──

    @GetMapping("/personal-details")
    public ResponseEntity<PersonalDetailsRequest> getPersonalDetails(
            @AuthenticationPrincipal StudentPrincipal principal) {
        return ResponseEntity.ok(personalDetailsService.get(principal.getStudentId()));
    }

    @PostMapping("/personal-details")
    public ResponseEntity<Void> savePersonalDetails(
            @AuthenticationPrincipal StudentPrincipal principal,
            @Valid @RequestBody PersonalDetailsRequest request) {
        personalDetailsService.save(principal.getStudentId(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/personal-details")
    public ResponseEntity<Void> updatePersonalDetails(
            @AuthenticationPrincipal StudentPrincipal principal,
            @Valid @RequestBody PersonalDetailsRequest request) {
        personalDetailsService.save(principal.getStudentId(), request);
        return ResponseEntity.ok().build();
    }
}
