package com.example.student_onb_svc.ChatBot;

import com.example.student_onb_svc.Security.StudentPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/onboarding/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Getter @Setter
    static class ChatRequest {
        @NotBlank(message = "Message is required")
        private String message;
        private String sessionId; // for unauthenticated users
    }

    /**
     * POST /api/v1/chat — send a message to the chatbot.
     * If authenticated (session token), uses student context.
     * If unauthenticated, uses sessionId for conversation continuity.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(
            @AuthenticationPrincipal StudentPrincipal principal,
            @Valid @RequestBody ChatRequest request) {

        UUID studentId = principal != null ? principal.getStudentId() : null;
        String sessionId = request.getSessionId();

        // Unauthenticated users must provide a sessionId
        if (studentId == null && (sessionId == null || sessionId.isBlank())) {
            sessionId = UUID.randomUUID().toString();
        }

        Map<String, Object> response = chatService.chat(
                request.getMessage(), studentId, sessionId);

        return ResponseEntity.ok(response);
    }
}