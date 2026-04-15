package com.example.student_onb_svc.IdVerification;

import com.example.student_onb_svc.Security.StudentPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding")
public class IdVerificationController {

    @Autowired
    IdVerificationService idVerificationService;

    @GetMapping("/id-verification")
    public ResponseEntity<Map<String, Object>> getIdVerification(
            @AuthenticationPrincipal StudentPrincipal principal) {
        return ResponseEntity.ok(idVerificationService.get(principal.getStudentId()));
    }

    @PostMapping("/id-verification")
    public ResponseEntity<Map<String, String>> saveIdVerification(
            @RequestParam String token,
            @AuthenticationPrincipal StudentPrincipal principal,
            @Valid @RequestBody IdVerificationRequest request) {
        Map<String, String> paths = idVerificationService.save(
                token, request.getNationalId(), request.getSelfie());
        return ResponseEntity.ok(paths);
    }
}
