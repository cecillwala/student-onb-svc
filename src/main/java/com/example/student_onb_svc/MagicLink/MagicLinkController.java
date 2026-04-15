package com.example.student_onb_svc.MagicLink;

import com.example.student_onb_svc.Common.VerifyIdentityRequest;
import com.example.student_onb_svc.Common.SessionResponse;
import com.example.student_onb_svc.Common.VerifyTokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class MagicLinkController {

    private final MagicLinkService magicLinkService;

    /**
     * GET /api/v1/onboarding/verify?token=xxx
     * Validates the magic link token. Returns student info for the identity confirmation screen.
     * No auth required.
     */
    @GetMapping("/verify")
    public ResponseEntity<VerifyTokenResponse> verifyToken(@RequestParam String token) {
        return ResponseEntity.ok(magicLinkService.verifyToken(token));
    }

    /**
     * POST /api/v1/onboarding/verify-identity
     * Confirms identity by matching national ID. Returns session token.
     * No auth required.
     */
    @PostMapping("/verify-identity")
    public ResponseEntity<SessionResponse> verifyIdentity(
            @Valid @RequestBody VerifyIdentityRequest request) {
        SessionResponse response = magicLinkService.verifyIdentity(
                request.getToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current-step")
    public ResponseEntity<Integer> getCurrentStep(@RequestParam String token){
        return ResponseEntity.ok(magicLinkService.getCurrentStep(token));
    }
}