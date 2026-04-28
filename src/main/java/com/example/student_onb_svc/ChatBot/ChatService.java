package com.example.student_onb_svc.ChatBot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Value("${chatbot.api-key:}")
    private String apiKey;

    @Value("${chatbot.model:gemini-3.1-flash-lite-preview}")
    private String model;

    @Value("${chatbot.max-tokens:1024}")
    private int maxTokens;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    /**
     * Send a message and get a response.
     * If studentId is provided, includes student context and stores conversation history.
     * If sessionId is provided (unauthenticated), uses session-based conversation.
     */
    public Map<String, Object> chat(String userMessage, UUID studentId, String sessionId) {

        UUID conversationId = getOrCreateConversation(studentId, sessionId);
        List<Map<String, String>> history = loadHistory(conversationId);
        String systemPrompt = buildSystemPrompt(studentId);

        history.add(Map.of("role", "user", "content", userMessage));

        String response = callGemini(systemPrompt, history);

        storeMessage(conversationId, "user", userMessage);
        storeMessage(conversationId, "assistant", response);
        jdbc.update("UPDATE chat_conversations SET updated_at = NOW() WHERE id = ?", conversationId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conversationId.toString());
        result.put("response", response);
        return result;
    }

    private UUID getOrCreateConversation(UUID studentId, String sessionId) {
        String lookupSql;
        Object lookupParam;

        if (studentId != null) {
            lookupSql = "SELECT id FROM chat_conversations WHERE student_id = ? ORDER BY updated_at DESC LIMIT 1";
            lookupParam = studentId;
        } else {
            lookupSql = "SELECT id FROM chat_conversations WHERE session_id = ? ORDER BY updated_at DESC LIMIT 1";
            lookupParam = sessionId;
        }

        List<UUID> existing = jdbc.query(lookupSql,
                (rs, rowNum) -> rs.getObject("id", UUID.class), lookupParam);

        if (!existing.isEmpty()) {
            return existing.getFirst();
        }

        UUID newId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO chat_conversations (id, student_id, session_id)
            VALUES (?, ?, ?)
            """, newId, studentId, sessionId);

        return newId;
    }

    private List<Map<String, String>> loadHistory(UUID conversationId) {
        return new ArrayList<>(jdbc.query("""
            SELECT role, content FROM chat_messages
            WHERE conversation_id = ?
            ORDER BY created_at ASC
            LIMIT 20
            """,
                (rs, rowNum) -> Map.of(
                        "role", rs.getString("role"),
                        "content", rs.getString("content")
                ), conversationId
        ));
    }

    private void storeMessage(UUID conversationId, String role, String content) {
        jdbc.update("""
            INSERT INTO chat_messages (id, conversation_id, role, content)
            VALUES (gen_random_uuid(), ?, ?, ?)
            """, conversationId, role, content);
    }

    private String buildSystemPrompt(UUID studentId) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
            You are the Egerton University Onboarding Assistant. You help first-year students complete their online enrolment process.

            ABOUT THE ONBOARDING PROCESS:
            The onboarding has 7 steps that students must complete:
            1. Personal Details — gender, religion, nationality, email, phone, address, guardian information
            2. ID Verification — upload national ID photo and take a selfie
            3. Academic Details — mode of study, academic interests, support needs, extracurricular activities
            4. Accommodation — choose between resident (on-campus hostel) or non-resident (off-campus). Residents select hostel, room type, floor, and room. Non-residents provide landlord and location details.
            5. Health Details — blood group, medical conditions, allergies, insurance, emergency contact, and medical report upload
            6. Document Upload — KCSE Certificate, KCSE Result Slip, Birth Certificate, Chief's Details Form, Leaving Certificate, Letter of Acceptance
            7. Summary & Submit — review all information and submit

            AFTER SUBMISSION:
            The application goes through verification stages:
            1. Department Verification — department head reviews academic details and documents
            2. Medical Review — medical staff reviews health details
            3. Accommodation Review — hostel manager reviews accommodation preferences
            4. Completion — all stages approved, student is fully onboarded

            Students can track their application status using the tracking link sent to their email.

            DOCUMENTS REQUIRED:
            - KCSE Certificate (required)
            - KCSE Result Slip (required)
            - Birth Certificate (required)
            - Chief's Details Form (required) — signed form from the area chief
            - Leaving Certificate (required) — school leaving certificate
            - Letter of Acceptance (optional)

            FILE REQUIREMENTS:
            - Accepted formats: PDF, JPEG, PNG
            - Maximum file size: 5MB per file

            ACCOMMODATION:
            - Resident students select a hostel, room type, floor, and specific room
            - Non-resident students provide: reason for living off-campus, building name, location, landlord details
            - Popular off-campus locations include: Njokerio, Gate, Ng'ondu

            IMPORTANT RULES:
            - Be friendly, helpful, and concise
            - Answer only questions related to Egerton University onboarding
            - If asked about something unrelated, politely redirect to onboarding topics
            - Use simple language — many students are first-time university applicants
            - If you don't know the answer, suggest contacting the registrar's office at support@egerton.ac.ke or calling 0712-345-678
            """);

        if (studentId != null) {
            try {
                jdbc.query("""
                    SELECT s.first_name, s.last_name, s.programme, s.current_step, s.status,
                           d.name AS department, d.faculty
                    FROM students s
                    LEFT JOIN departments d ON s.department_id = d.id
                    WHERE s.id = ?
                    """, rs -> {
                    sb.append("\n\nCURRENT STUDENT CONTEXT:\n");
                    sb.append("Name: ").append(rs.getString("first_name")).append(" ").append(rs.getString("last_name")).append("\n");
                    sb.append("Programme: ").append(rs.getString("programme")).append("\n");
                    sb.append("Department: ").append(rs.getString("department")).append("\n");
                    sb.append("Faculty: ").append(rs.getString("faculty")).append("\n");
                    sb.append("Current Step: ").append(rs.getInt("current_step")).append(" of 7\n");
                    sb.append("Status: ").append(rs.getString("status")).append("\n");
                    sb.append("\nUse this information to give personalised guidance. Address the student by their first name.");
                }, studentId);

                jdbc.query("""
                    SELECT current_stage, dept_verification_status, dept_flag_reason,
                           medical_status, medical_flag_reason, medical_referral_note
                    FROM onboarding_statuses WHERE student_id = ?
                    """, rs -> {
                    sb.append("\nVERIFICATION STATUS:\n");
                    sb.append("Current Stage: ").append(rs.getString("current_stage")).append("\n");
                    sb.append("Dept Verification: ").append(rs.getString("dept_verification_status")).append("\n");
                    if (rs.getString("dept_flag_reason") != null) {
                        sb.append("Dept Flag Reason: ").append(rs.getString("dept_flag_reason")).append("\n");
                    }
                    sb.append("Medical Status: ").append(rs.getString("medical_status")).append("\n");
                    if (rs.getString("medical_flag_reason") != null) {
                        sb.append("Medical Flag Reason: ").append(rs.getString("medical_flag_reason")).append("\n");
                    }
                }, studentId);
            } catch (Exception e) {
                log.warn("Could not load student context: {}", e.getMessage());
            }
        }

        return sb.toString();
    }

    /**
     * Call Google Gemini API.
     * Gemini uses a different message format:
     * - System instruction is separate
     * - Messages use "user" and "model" roles (not "assistant")
     * - Content is nested in "parts"
     */
    private String callGemini(String systemPrompt, List<Map<String, String>> messages) {
        try {
            RestTemplate rest = new RestTemplate();

            String url = String.format(GEMINI_URL, model, apiKey);

            ObjectNode body = objectMapper.createObjectNode();

            // System instruction
            ObjectNode systemInstruction = body.putObject("system_instruction");
            ObjectNode sysPart = systemInstruction.putArray("parts").addObject();
            sysPart.put("text", systemPrompt);

            // Conversation contents
            ArrayNode contents = body.putArray("contents");
            for (Map<String, String> msg : messages) {
                ObjectNode turn = contents.addObject();
                // Gemini uses "model" instead of "assistant"
                String role = "assistant".equals(msg.get("role")) ? "model" : msg.get("role");
                turn.put("role", role);
                ObjectNode part = turn.putArray("parts").addObject();
                part.put("text", msg.get("content"));
            }

            // Generation config
            ObjectNode genConfig = body.putObject("generationConfig");
            genConfig.put("maxOutputTokens", maxTokens);
            genConfig.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = rest.exchange(url, HttpMethod.POST, request, String.class);

            JsonNode responseBody = objectMapper.readTree(response.getBody());

            // Extract text from Gemini response
            JsonNode candidates = responseBody.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        return parts.get(0).get("text").asText();
                    }
                }
            }

            return "I'm sorry, I couldn't process your question. Please try again.";

        } catch (Exception e) {
            log.error("Gemini API error: {}", e.getMessage(), e);
            return "I'm having trouble connecting right now. Please try again in a moment, or contact support@egerton.ac.ke for help.";
        }
    }
}