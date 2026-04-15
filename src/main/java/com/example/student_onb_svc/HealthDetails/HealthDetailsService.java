package com.example.student_onb_svc.HealthDetails;

import com.example.student_onb_svc.Common.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.student_onb_svc.Common.FileStorageService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthDetailsService {

    private final JdbcTemplate jdbc;
    private final FileStorageService fileStorage;

    @Autowired
    Helper helper;

    public HealthDetailsRequest get(String token) {

        UUID student_id = helper.getStudentId(token);
        List<HealthDetailsRequest> results = jdbc.query("""
            SELECT blood_group, preferred_hospital, medical_conditions, allergies,
                   insurance_provider, insurance_policy_number,
                   emergency_first_name, emergency_last_name, emergency_relationship,
                   emergency_phone, emergency_email
            FROM health_details
            WHERE student_id = ?
            """,
                (rs, rowNum) -> {
                    HealthDetailsRequest dto = new HealthDetailsRequest();
                    dto.setBloodGroup(rs.getString("blood_group"));
                    dto.setPreferredHospital(rs.getString("preferred_hospital"));
                    dto.setMedicalConditions(csvToList(rs.getString("medical_conditions")));
                    dto.setAllergies(csvToList(rs.getString("allergies")));
                    dto.setInsuranceProvider(rs.getString("insurance_provider"));
                    dto.setPolicyNumber(rs.getString("insurance_policy_number"));
                    dto.setEmergencyFirstName(rs.getString("emergency_first_name"));
                    dto.setEmergencyLastName(rs.getString("emergency_last_name"));
                    dto.setEmergencyRelationship(rs.getString("emergency_relationship"));
                    dto.setEmergencyPhone(rs.getString("emergency_phone"));
                    dto.setEmergencyEmail(rs.getString("emergency_email"));
                    return dto;
                }, student_id
        );

        return results.isEmpty() ? new HealthDetailsRequest() : results.getFirst();
    }

    public void save(String token, HealthDetailsRequest req) {
        String conditions = listToCsv(req.getMedicalConditions());
        String allergies = listToCsv(req.getAllergies());

        UUID student_id = helper.getStudentId(token);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM health_details WHERE student_id = ?",
                Integer.class, student_id);

        if (count != null && count > 0) {
            jdbc.update("""
                UPDATE health_details
                SET blood_group = ?, preferred_hospital = ?, medical_conditions = ?, allergies = ?,
                    insurance_provider = ?, insurance_policy_number = ?,
                    emergency_first_name = ?, emergency_last_name = ?, emergency_relationship = ?,
                    emergency_phone = ?, emergency_email = ?
                WHERE student_id = ?
                """,
                    req.getBloodGroup(), req.getPreferredHospital(), conditions, allergies,
                    req.getInsuranceProvider(), req.getPolicyNumber(),
                    req.getEmergencyFirstName(), req.getEmergencyLastName(), req.getEmergencyRelationship(),
                    req.getEmergencyPhone(), req.getEmergencyEmail(),
                    student_id
            );
        } else {
            jdbc.update("""
                INSERT INTO health_details (id, student_id, blood_group, preferred_hospital,
                    medical_conditions, allergies, insurance_provider, insurance_policy_number,
                    emergency_first_name, emergency_last_name, emergency_relationship,
                    emergency_phone, emergency_email)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                    student_id,
                    req.getBloodGroup(), req.getPreferredHospital(), conditions, allergies,
                    req.getInsuranceProvider(), req.getPolicyNumber(),
                    req.getEmergencyFirstName(), req.getEmergencyLastName(), req.getEmergencyRelationship(),
                    req.getEmergencyPhone(), req.getEmergencyEmail()
            );
        }

        // Advance step
        jdbc.update("""
            UPDATE students SET current_step = GREATEST(current_step, 5), updated_at = NOW()
            WHERE id = ?
            """, student_id);

        log.info("Health details saved for student {}", student_id);
    }

    /**
     * Upload medical report separately.
     */
    public String uploadMedicalReport(String token, MultipartFile file) {

        UUID student_id = helper.getStudentId(token);
        String path = fileStorage.saveFile(student_id, "medical", file);

        // Ensure health_details row exists
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM health_details WHERE student_id = ?",
                Integer.class, student_id);

        if (count != null && count > 0) {
            jdbc.update("UPDATE health_details SET medical_report_path = ? WHERE student_id = ?",
                    path, student_id);
        } else {
            // Create a minimal row — the rest will be filled when the form is saved
            jdbc.update("""
                INSERT INTO health_details (id, student_id, blood_group, emergency_first_name,
                    emergency_last_name, emergency_phone, medical_report_path)
                VALUES (gen_random_uuid(), ?, '', '', '', '', ?)
                """, student_id, path);
        }

        log.info("Medical report uploaded for student {}: {}", student_id, path);
        return path;
    }

    // ── Helpers ──

    private String listToCsv(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }

    private List<String> csvToList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.asList(csv.split(","));
    }
}
