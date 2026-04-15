package com.example.student_onb_svc.IdVerification;

import com.example.student_onb_svc.Common.FileStorageService;
import com.example.student_onb_svc.Common.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdVerificationService {

    private final JdbcTemplate jdbc;
    private final FileStorageService fileStorage;

    @Autowired
    Helper helper;

    /**
     * Save national ID image + selfie image.
     * Images come as base64 data URIs from the frontend canvas.
     */
    public Map<String, String> save(String token, String nationalIdBase64, String selfieBase64) {

        String hashed = helper.sha256(token);

        // Save files to disk
        String idPath = fileStorage.saveBase64Image(token, "id", nationalIdBase64, "national_id.jpg");
        String selfiePath = fileStorage.saveBase64Image(token, "id", selfieBase64, "selfie.jpg");

        log.info(hashed);

        UUID student_id = jdbc.queryForObject(
                "SELECT id FROM students WHERE magic_link_token = ?",
                UUID.class, hashed);

        // Check if record already exists
        int count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM id_verifications WHERE student_id = ?",
                Integer.class, student_id);

        if (count > 0) {
            // Update existing
            jdbc.update("""
                UPDATE id_verifications
                SET id_image_path = ?, selfie_image_path = ?,
                    id_match_status = 'PENDING', liveness_result = 'PENDING'
                WHERE student_id = ?
                """, idPath, selfiePath, student_id);
        } else {
            // Insert new
            jdbc.update("""
                INSERT INTO id_verifications (id, student_id, id_image_path, selfie_image_path)
                VALUES (gen_random_uuid(), ?, ?, ?)
                """, student_id, idPath, selfiePath);
        }

        // Advance step
        jdbc.update("""
            UPDATE students
            SET current_step = GREATEST(current_step, 2), updated_at = NOW()
            WHERE id = ?
            """, student_id);

        log.info("ID verification saved for student {}", token);

        return Map.of(
                "idImagePath", idPath,
                "selfieImagePath", selfiePath
        );
    }

    /**
     * Get saved verification data (file paths + status).
     */
    public Map<String, Object> get(UUID studentId) {
        List<Map<String, Object>> results = jdbc.query("""
            SELECT id_image_path, selfie_image_path,
                   id_match_status, liveness_result, liveness_confidence
            FROM id_verifications
            WHERE student_id = ?
            """,
                (rs, rowNum) -> Map.<String, Object>of(
                        "idImagePath", rs.getString("id_image_path") != null ? rs.getString("id_image_path") : "",
                        "selfieImagePath", rs.getString("selfie_image_path") != null ? rs.getString("selfie_image_path") : "",
                        "idMatchStatus", rs.getString("id_match_status") != null ? rs.getString("id_match_status") : "PENDING",
                        "livenessResult", rs.getString("liveness_result") != null ? rs.getString("liveness_result") : "PENDING"
                ), studentId
        );

        if (results.isEmpty()) {
            return Map.of(
                    "idImagePath", "",
                    "selfieImagePath", "",
                    "idMatchStatus", "PENDING",
                    "livenessResult", "PENDING"
            );
        }

        return results.getFirst();
    }
}