package com.example.student_onb_svc.StepTracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final JdbcTemplate jdbc;

    /**
     * Get onboarding tracking data using student ID.
     * Public endpoint — no auth required.
     */
    public Map<String, Object> getTrackingByStudentId(UUID studentId) {
        List<Map<String, Object>> results = jdbc.query("""
            SELECT s.first_name, s.last_name, s.reg_no, s.programme, s.status,
                   o.current_stage,
                   o.admin_review_status, o.admin_review_at,
                   o.dept_verification_status, o.dept_verification_at, o.dept_flag_reason,
                   o.medical_status, o.medical_cleared_at, o.medical_flag_reason, o.medical_referral_note,
                   o.completed_at
            FROM students s
            LEFT JOIN onboarding_statuses o ON s.id = o.student_id
            WHERE s.id = ?
            """,
                (rs, rowNum) -> buildTrackingResponse(rs), studentId
        );

        if (results.isEmpty()) {
            throw new IllegalArgumentException("Student not found.");
        }

        return results.getFirst();
    }

    /**
     * Get onboarding tracking data using magic link token.
     */
    public Map<String, Object> getTrackingByToken(String rawToken) {
        String hashedToken = sha256(rawToken);

        List<Map<String, Object>> results = jdbc.query("""
            SELECT s.first_name, s.last_name, s.reg_no, s.programme, s.status,
                   o.current_stage,
                   o.admin_review_status, o.admin_review_at,
                   o.dept_verification_status, o.dept_verification_at, o.dept_flag_reason,
                   o.medical_status, o.medical_cleared_at, o.medical_flag_reason, o.medical_referral_note,
                   o.completed_at
            FROM students s
            LEFT JOIN onboarding_statuses o ON s.id = o.student_id
            WHERE s.magic_link_token = ?
            """,
                (rs, rowNum) -> buildTrackingResponse(rs), hashedToken
        );

        if (results.isEmpty()) {
            throw new IllegalArgumentException("Invalid tracking link.");
        }

        return results.getFirst();
    }

    private Map<String, Object> buildTrackingResponse(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("name", rs.getString("first_name") + " " + rs.getString("last_name"));
        data.put("regNo", rs.getString("reg_no"));
        data.put("programme", rs.getString("programme"));
        data.put("status", rs.getString("status"));
        data.put("currentStage", rs.getString("current_stage"));

        List<Map<String, Object>> stages = new ArrayList<>();

        stages.add(buildStage(
                "submitted", "Application Submitted", "send",
                "Your onboarding application has been received and is under review.",
                resolveStatus("SUBMITTED", rs.getString("current_stage")),
                null, null
        ));

        String deptStatus = rs.getString("dept_verification_status");
        stages.add(buildStage(
                "dept_verification", "Department Verification", "school",
                "Your academic details and documents are being verified by your department.",
                resolveStageStatus(deptStatus, "DEPT_VERIFICATION", rs.getString("current_stage")),
                rs.getTimestamp("dept_verification_at") != null ? rs.getTimestamp("dept_verification_at").toString() : null,
                rs.getString("dept_flag_reason")
        ));

        String medStatus = rs.getString("medical_status");
        stages.add(buildStage(
                "medical_review", "Medical Review", "local_hospital",
                "Your health details and medical report are being reviewed.",
                resolveStageStatus(medStatus, "MEDICAL_REVIEW", rs.getString("current_stage")),
                rs.getTimestamp("medical_cleared_at") != null ? rs.getTimestamp("medical_cleared_at").toString() : null,
                rs.getString("medical_flag_reason") != null ? rs.getString("medical_flag_reason") :
                        rs.getString("medical_referral_note") != null ? "Referred: " + rs.getString("medical_referral_note") : null
        ));

        stages.add(buildStage(
                "accommodation", "Accommodation Review", "home",
                "Your accommodation preferences are being reviewed.",
                resolveStatus("ACCOMMODATION", rs.getString("current_stage")),
                null, null
        ));

        stages.add(buildStage(
                "completed", "Onboarding Complete", "verified",
                "All verification stages are complete. Welcome to Egerton University!",
                rs.getTimestamp("completed_at") != null ? "completed" : "pending",
                rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toString() : null,
                null
        ));

        data.put("stages", stages);
        return data;
    }

    private Map<String, Object> buildStage(String id, String title, String icon,
                                           String description, String status,
                                           String date, String note) {
        Map<String, Object> stage = new LinkedHashMap<>();
        stage.put("id", id);
        stage.put("title", title);
        stage.put("icon", icon);
        stage.put("description", description);
        stage.put("status", status); // completed, in-progress, pending, flagged
        stage.put("date", date);
        stage.put("note", note);
        return stage;
    }

    /**
     * Determine stage status based on the current_stage progression.
     * Stages before current = completed, current = in-progress, after = pending.
     */
    private String resolveStatus(String thisStage, String currentStage) {
        if (currentStage == null) return "pending";

        String[] order = {"INVITED", "SUBMITTED", "DEPT_VERIFICATION", "MEDICAL_REVIEW", "ACCOMMODATION", "COMPLETED"};
        int thisIndex = indexOf(order, thisStage);
        int currentIndex = indexOf(order, currentStage);

        if (currentStage.equals("COMPLETED") && thisStage.equals("COMPLETED")) return "completed";
        if (thisIndex < currentIndex) return "completed";
        if (thisIndex == currentIndex) return "in-progress";
        return "pending";
    }

    /**
     * Resolve status for stages that have their own status field (dept, medical).
     */
    private String resolveStageStatus(String stageStatus, String thisStage, String currentStage) {
        if ("APPROVED".equals(stageStatus) || "CLEARED".equals(stageStatus)) return "completed";
        if ("FLAGGED".equals(stageStatus)) return "flagged";
        if ("REFERRED".equals(stageStatus)) return "flagged";

        // Fall back to position-based
        return resolveStatus(thisStage, currentStage);
    }

    private int indexOf(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(value)) return i;
        }
        return -1;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}