package com.example.student_onb_svc.Summary;

import com.example.student_onb_svc.Common.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final JdbcTemplate jdbc;

    @Autowired
    Helper helper;

    /**
     * Aggregate all onboarding data for the summary page.
     */
    public Map<String, Object> getSummary(String token) {
        Map<String, Object> summary = new LinkedHashMap<>();

        UUID studentId = helper.getStudentId(token);

        // ── Personal Details ──
        jdbc.query("""
            SELECT gender, religion, nationality, email, phone,
                   county, constituency, ward, postal_code,
                   guardian_first_name, guardian_last_name, guardian_phone,
                   guardian_email, guardian_occupation, guardian_relationship
            FROM students WHERE id = ?
            """, rs -> {
            Map<String, String> personal = new LinkedHashMap<>();
            personal.put("gender", rs.getString("gender"));
            personal.put("religion", rs.getString("religion"));
            personal.put("nationality", rs.getString("nationality"));
            personal.put("email", rs.getString("email"));
            personal.put("phone", rs.getString("phone"));
            personal.put("county", rs.getString("county"));
            personal.put("constituency", rs.getString("constituency"));
            personal.put("ward", rs.getString("ward"));
            personal.put("postalCode", rs.getString("postal_code"));
            personal.put("guardianFirstName", rs.getString("guardian_first_name"));
            personal.put("guardianLastName", rs.getString("guardian_last_name"));
            personal.put("guardianPhone", rs.getString("guardian_phone"));
            personal.put("guardianEmail", rs.getString("guardian_email"));
            personal.put("guardianOccupation", rs.getString("guardian_occupation"));
            personal.put("guardianRelationship", rs.getString("guardian_relationship"));
            summary.put("personalDetails", personal);
        }, studentId);

        // ── ID Verification ──
        jdbc.query("""
            SELECT id_match_status, liveness_result, liveness_confidence
            FROM id_verifications WHERE student_id = ?
            """, rs -> {
            Map<String, String> idVerification = new LinkedHashMap<>();
            idVerification.put("idMatchStatus", rs.getString("id_match_status"));
            idVerification.put("livenessResult", rs.getString("liveness_result"));
            summary.put("idVerification", idVerification);
        }, studentId);

        // ── Academic Details ──
        jdbc.query("""
            SELECT mode_of_study, academic_interests, learning_support_needs, extracurricular_activities
            FROM academic_details WHERE student_id = ?
            """, rs -> {
            Map<String, String> academic = new LinkedHashMap<>();
            academic.put("modeOfStudy", rs.getString("mode_of_study"));
            academic.put("academicInterests", rs.getString("academic_interests"));
            academic.put("learningSupportNeeds", rs.getString("learning_support_needs"));
            academic.put("extracurricularActivities", rs.getString("extracurricular_activities"));
            summary.put("academicDetails", academic);
        }, studentId);

        // ── Accommodation ──
        jdbc.query("""
            SELECT a.residence_type, h.name AS hostel_name, a.room_type,
                   r.room_number, a.floor, a.special_needs,
                   a.off_campus_reason, a.guardian_aware, a.building_name,
                   a.off_campus_location, a.off_campus_room_type,
                   a.landlord_first_name, a.landlord_last_name, a.landlord_phone,
                   a.roommate_first_name, a.roommate_last_name, a.roommate_phone
            FROM accommodation_details a
            LEFT JOIN hostels h ON a.preferred_hostel_id = h.id
            LEFT JOIN rooms r ON a.room = r.id
            WHERE a.student_id = ?
            """, rs -> {
            Map<String, String> accommodation = new LinkedHashMap<>();
            accommodation.put("residenceType", rs.getString("residence_type"));
            accommodation.put("hostelName", rs.getString("hostel_name"));
            accommodation.put("roomType", rs.getString("room_type"));
            accommodation.put("roomNumber", rs.getString("room_number"));
            accommodation.put("floor", rs.getString("floor"));
            accommodation.put("specialNeeds", rs.getString("special_needs"));
            accommodation.put("offCampusReason", rs.getString("off_campus_reason"));
            accommodation.put("guardianAware", rs.getString("guardian_aware"));
            accommodation.put("buildingName", rs.getString("building_name"));
            accommodation.put("offCampusLocation", rs.getString("off_campus_location"));
            accommodation.put("offCampusRoomType", rs.getString("off_campus_room_type"));
            accommodation.put("landlordFirstName", rs.getString("landlord_first_name"));
            accommodation.put("landlordLastName", rs.getString("landlord_last_name"));
            accommodation.put("landlordPhone", rs.getString("landlord_phone"));
            accommodation.put("roommateFirstName", rs.getString("roommate_first_name"));
            accommodation.put("roommateLastName", rs.getString("roommate_last_name"));
            accommodation.put("roommatePhone", rs.getString("roommate_phone"));
            summary.put("accommodation", accommodation);
        }, studentId);

        // ── Health Details ──
        jdbc.query("""
            SELECT blood_group, preferred_hospital, medical_conditions, allergies,
                   insurance_provider, insurance_policy_number,
                   emergency_first_name, emergency_last_name, emergency_relationship,
                   emergency_phone, emergency_email, medical_report_path
            FROM health_details WHERE student_id = ?
            """, rs -> {
            Map<String, String> health = new LinkedHashMap<>();
            health.put("bloodGroup", rs.getString("blood_group"));
            health.put("preferredHospital", rs.getString("preferred_hospital"));
            health.put("medicalConditions", rs.getString("medical_conditions"));
            health.put("allergies", rs.getString("allergies"));
            health.put("insuranceProvider", rs.getString("insurance_provider"));
            health.put("policyNumber", rs.getString("insurance_policy_number"));
            health.put("emergencyFirstName", rs.getString("emergency_first_name"));
            health.put("emergencyLastName", rs.getString("emergency_last_name"));
            health.put("emergencyRelationship", rs.getString("emergency_relationship"));
            health.put("emergencyPhone", rs.getString("emergency_phone"));
            health.put("emergencyEmail", rs.getString("emergency_email"));
            health.put("medicalReportPath", rs.getString("medical_report_path"));
            summary.put("healthDetails", health);
        }, studentId);

        // ── Documents ──
        List<Map<String, String>> documents = jdbc.query("""
            SELECT document_type, file_name, status
            FROM documents WHERE student_id = ?
            ORDER BY uploaded_at
            """,
                (rs, rowNum) -> {
                    Map<String, String> doc = new LinkedHashMap<>();
                    doc.put("documentType", rs.getString("document_type"));
                    doc.put("fileName", rs.getString("file_name"));
                    doc.put("status", rs.getString("status"));
                    return doc;
                }, studentId);
        summary.put("documents", documents);

        return summary;
    }

    /**
     * Submit the application.
     */
    public UUID submit(String token) {

        UUID studentId = helper.getStudentId(token);

        // Verify all required data exists
        Integer currentStep = jdbc.queryForObject(
                "SELECT current_step FROM students WHERE id = ?",
                Integer.class, studentId);

        if (currentStep == null || currentStep < 6) {
            throw new IllegalStateException("Please complete all steps before submitting.");
        }

        // Update student status
        jdbc.update("""
            UPDATE students
            SET status = 'SUBMITTED', current_step = 7, updated_at = NOW()
            WHERE id = ? AND status != 'SUBMITTED'
            """, studentId);

        // Create onboarding status record
        Integer statusExists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM onboarding_statuses WHERE student_id = ?",
                Integer.class, studentId);

        if (statusExists == null || statusExists == 0) {
            jdbc.update("""
                INSERT INTO onboarding_statuses (id, student_id, current_stage)
                VALUES (gen_random_uuid(), ?, 'DEPT_VERIFICATION')
                """, studentId);
        } else {
            jdbc.update("""
                UPDATE onboarding_statuses SET current_stage = 'DEPT_VERIFICATION'
                WHERE student_id = ?
                """, studentId);
        }

        log.info("Application submitted for student {}", studentId);

        return studentId;
    }
}
