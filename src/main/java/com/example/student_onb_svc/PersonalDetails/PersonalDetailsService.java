package com.example.student_onb_svc.PersonalDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalDetailsService {

    private final JdbcTemplate jdbc;

    public PersonalDetailsRequest get(UUID studentId) {
        List<PersonalDetailsRequest> results = jdbc.query("""
            SELECT gender, religion, nationality, email, phone,
                   county, constituency, ward, postal_code,
                   guardian_first_name, guardian_last_name,
                   guardian_phone, guardian_email,
                   guardian_occupation, guardian_relationship
            FROM students
            WHERE id = ?
            """,
                (rs, rowNum) -> {
                    PersonalDetailsRequest dto = new PersonalDetailsRequest();
                    dto.setGender(rs.getString("gender"));
                    dto.setReligion(rs.getString("religion"));
                    dto.setNationality(rs.getString("nationality"));
                    dto.setEmail(rs.getString("email"));
                    dto.setPhone(rs.getString("phone"));
                    dto.setCounty(rs.getString("county"));
                    dto.setConstituency(rs.getString("constituency"));
                    dto.setWard(rs.getString("ward"));
                    dto.setPostalCode(rs.getString("postal_code"));
                    dto.setGuardianFirstName(rs.getString("guardian_first_name"));
                    dto.setGuardianLastName(rs.getString("guardian_last_name"));
                    dto.setGuardianPhone(rs.getString("guardian_phone"));
                    dto.setGuardianEmail(rs.getString("guardian_email"));
                    dto.setGuardianOccupation(rs.getString("guardian_occupation"));
                    dto.setGuardianRelationship(rs.getString("guardian_relationship"));
                    return dto;
                }, studentId
        );

        return results.isEmpty() ? new PersonalDetailsRequest() : results.getFirst();
    }

    public void save(UUID studentId, PersonalDetailsRequest req) {
        int updated = jdbc.update("""
            UPDATE students
            SET gender = ?, religion = ?, nationality = ?,
                email = ?, phone = ?,
                county = ?, constituency = ?, ward = ?, postal_code = ?,
                guardian_first_name = ?, guardian_last_name = ?,
                guardian_phone = ?, guardian_email = ?,
                guardian_occupation = ?, guardian_relationship = ?,
                current_step = GREATEST(current_step, 1),
                updated_at = NOW()
            WHERE id = ?
            """,
                req.getGender(), req.getReligion(), req.getNationality(),
                req.getEmail(), req.getPhone(),
                req.getCounty(), req.getConstituency(), req.getWard(), req.getPostalCode(),
                req.getGuardianFirstName(), req.getGuardianLastName(),
                req.getGuardianPhone(), req.getGuardianEmail(),
                req.getGuardianOccupation(), req.getGuardianRelationship(),
                studentId
        );

        if (updated == 0) {
            throw new IllegalArgumentException("Student not found");
        }

        log.info("Personal details saved for student {}", studentId);
    }
}