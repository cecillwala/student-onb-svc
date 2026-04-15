package com.example.student_onb_svc.MagicLink;

import com.example.student_onb_svc.Common.SessionResponse;
import com.example.student_onb_svc.Common.VerifyTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MagicLinkService {

    private final JdbcTemplate jdbc;

    /**
     * Step 1: Validate the magic link token.
     * Returns student info + masked national ID for the identity confirmation screen.
     */
    public VerifyTokenResponse verifyToken(String rawToken) {
        String hashedToken = sha256(rawToken);

        List<VerifyTokenResponse> results = jdbc.query("""
            SELECT s.first_name, s.last_name, s.index_number, s.reg_no,
                   s.programme, s.national_id, s.token_expiry, s.status,
                   d.name AS department, d.faculty
            FROM students s
            LEFT JOIN departments d ON s.department_id = d.id
            WHERE s.magic_link_token = ?
            """,
                (rs, rowNum) -> {
                    LocalDateTime expiry = rs.getObject("token_expiry", LocalDateTime.class);
                    String status = rs.getString("status");

                    if ("SUBMITTED".equals(status) || "COMPLETED".equals(status)) {
                        throw new IllegalStateException("You have already completed onboarding.");
                    }

                    if (expiry != null && expiry.isBefore(LocalDateTime.now())) {
                        throw new IllegalStateException("This link has expired. Please contact the university for a new one.");
                    }

                    String nationalId = rs.getString("national_id");
                    String fullName = rs.getString("first_name") + " " + rs.getString("last_name");

                    return VerifyTokenResponse.builder()
                            .fullName(fullName)
                            .indexNumber(rs.getString("index_number"))
                            .regNo(rs.getString("reg_no"))
                            .programme(rs.getString("programme"))
                            .department(rs.getString("department"))
                            .faculty(rs.getString("faculty"))
                            .maskedNationalId(maskNationalId(nationalId))
                            .build();
                }, hashedToken
        );

        if (results.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired link.");
        }

        return results.getFirst();
    }

    /**
     * Step 2: Confirm identity by matching national ID.
     * Issues a session token if the ID matches.
     */
    public SessionResponse verifyIdentity(String rawToken) {
        String hashedToken = sha256(rawToken);

        List<SessionResponse> results = jdbc.query("""
            SELECT s.id, s.first_name, s.last_name, s.reg_no, s.programme,
                   s.national_id, s.current_step, s.status, s.session_token
            FROM students s
            WHERE s.magic_link_token = ?
            """,
                (rs, rowNum) -> {
                    String storedId = rs.getString("national_id");
                    String existingSession = rs.getString("session_token");


                    String sessionToken = existingSession;
                    if (sessionToken == null || sessionToken.isBlank()) {
                        sessionToken = UUID.randomUUID().toString();
                    }

                    return SessionResponse.builder()
                            .sessionToken(sessionToken)
                            .studentId(rs.getObject("id", UUID.class))
                            .firstName(rs.getString("first_name"))
                            .lastName(rs.getString("last_name"))
                            .regNo(rs.getString("reg_no"))
                            .programme(rs.getString("programme"))
                            .currentStep(rs.getInt("current_step"))
                            .status(rs.getString("status"))
                            .build();
                }, hashedToken
        );

        if (results.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired link.");
        }

        SessionResponse session = results.getFirst();

        jdbc.update("""
            UPDATE students
            SET session_token = ?,
                status = CASE WHEN status = 'INVITED' THEN 'ONBOARDING' ELSE status END,
                updated_at = NOW()
            WHERE magic_link_token = ?
            """, session.getSessionToken(), hashedToken);

        return session;
    }

    // ── Helpers ──

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String maskNationalId(String id) {
        if (id == null || id.length() < 4) return "****";
        return id.substring(0, 3) + "*".repeat(id.length() - 5) + id.substring(id.length() - 2);
    }

    public int getCurrentStep(String token){
        try {
            String hashed_token = sha256(token);
            String sql =
                    """
                            SELECT current_step FROM students WHERE magic_link_token = ?
                            """;
            return jdbc.queryForObject(sql, Integer.class, new Object[]{hashed_token});
        }
        catch(NullPointerException e){
            return -1;
        }
    }
}