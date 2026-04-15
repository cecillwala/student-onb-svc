package com.example.student_onb_svc.AcademicDetails;

import com.example.student_onb_svc.Common.Helper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class AcademicDetailsService {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Helper helper;

    ObjectMapper mapper = new ObjectMapper();

    public boolean saveAcademicDetails(String token, AcademicDetailsRequest req) throws JsonProcessingException {

        UUID student_id = helper.getStudentId(token);

        String extraCurricular = mapper.writeValueAsString(req.getExtraCurricularActivities());

        int count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM academic_details WHERE student_id = ?",
                Integer.class, student_id);

        if (count > 0) {
            // Update existing
            jdbc.update("""
                UPDATE academic_details
                SET mode_of_study = ?, learning_support_needs = ?,
                    extracurricular_activities = ?
                WHERE student_id = ?
                """, req.getModeOfStudy(), req.getSpecialSupportNeeds(), extraCurricular, student_id);
        } else {
            // Insert new
            jdbc.update("""
                INSERT INTO academic_details (id, student_id, mode_of_study, learning_support_needs, extracurricular_activities)
                VALUES (gen_random_uuid(), ?, ?, ?, ?)
                """, student_id, req.getModeOfStudy(), req.getSpecialSupportNeeds(), extraCurricular);
        }

        // Advance step
        jdbc.update("""
            UPDATE students
            SET current_step = GREATEST(current_step, 3), updated_at = NOW()
            WHERE id = ?
            """, student_id);

        log.info("Academic details saved for student {}", token);
        return true;
    }
}