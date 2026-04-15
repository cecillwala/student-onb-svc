package com.example.student_onb_svc.AccommodationDetails;

import com.example.student_onb_svc.Common.Helper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AccommodationDetailsService {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Helper helper;


    public void save(String token, AccommodationDetailsRequest req){
        UUID student_id = helper.getStudentId(token);

        int count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM accommodation_details WHERE student_id = ?",
                Integer.class, student_id);

        if (count > 0) {
            // Update existing
            jdbc.update("""
                UPDATE accommodation_details
                SET preferred_hostel_id = ?, off_campus_location = ?, special_needs = ?, residence_type = ?, room_type = ?, room = ?, off_campus_reason = ?,
                    guardian_aware = ?, building_name = ?, off_campus_room_type = ?, landlord_first_name = ?, 
                    landlord_last_name = ?, landlord_phone = ?, roommate_first_name = ?, roommate_last_name = ?, roommate_phone = ?, floor = ?
                WHERE student_id = ?
                """, req.getHostelPreference(), req.getOffCampusLocation(), req.getSpecialNeeds(), req.getResidenceType(),
                    req.getRoomType(), req.getRoom(), req.getOffCampusReason(), req.getGuardianAware(), req.getBuildingName(), req.getOffCampusRoomType(),
                    req.getLandlordFirstName(), req.getLandlordLastName(), req.getLandlordPhone(), req.getRoommateFirstName(), req.getRoommateLastName(),
                    req.getRoommatePhone(), req.getFloor(), student_id);
        } else {
            // Insert new
            jdbc.update("""
    INSERT INTO accommodation_details (id, student_id, preferred_hostel_id, off_campus_location, special_needs, residence_type, room_type, room, off_campus_reason, guardian_aware, building_name, off_campus_room_type, landlord_first_name, landlord_last_name, landlord_phone, roommate_first_name, roommate_last_name, roommate_phone, floor)
    VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """,
                    student_id,
                    req.getHostelPreference() != null ? UUID.fromString(req.getHostelPreference()) : null,
                    req.getOffCampusLocation(),
                    req.getSpecialNeeds(),
                    req.getResidenceType(),
                    req.getRoomType(),
                    req.getRoom() != null ? UUID.fromString(req.getRoom()) : null,
                    req.getOffCampusReason(),
                    req.getGuardianAware() != null ? req.getGuardianAware().toString() : null,
                    req.getBuildingName(),
                    req.getOffCampusRoomType(),
                    req.getLandlordFirstName(),
                    req.getLandlordLastName(),
                    req.getLandlordPhone(),
                    req.getRoommateFirstName(),
                    req.getRoommateLastName(),
                    req.getRoommatePhone(),
                    req.getFloor()
            );
        }

        // Advance step
        jdbc.update("""
            UPDATE students
            SET current_step = GREATEST(current_step, 4), updated_at = NOW()
            WHERE id = ?
            """, student_id);

        log.info("Accommodation details saved for student {}", token);
    }

    public List<HostelModel> getHostels(){

        String sql =
                """
                    SELECT id, name, gender_allocation, total_capacity FROM hostels
                """;
        return jdbc.query(sql, BeanPropertyRowMapper.newInstance(HostelModel.class));
    }

    public List<HostelModel> getAccommodationDetails(){
        String sql = """
        SELECT h.id AS hostel_id, h.name AS hostel,
               h.gender_allocation, h.total_capacity,
               r.id AS room_id, r.hostel_id AS r_hostel_id,
               r.room_number, r.floor, r.room_type,
               r.capacity, r.available_beds
        FROM hostels h
        JOIN rooms r ON h.id = r.hostel_id
        WHERE r.status = 'AVAILABLE' AND r.available_beds > 0
        ORDER BY h.name, r.floor, r.room_number
        """;

        Map<UUID, HostelModel> hostelMap = new LinkedHashMap<>();

        jdbc.query(sql, (rs) -> {
            UUID hostelId = rs.getObject("hostel_id", UUID.class);

            HostelModel hostel = hostelMap.computeIfAbsent(hostelId, id -> {
                HostelModel h = new HostelModel();
                try {
                    h.setHostel(rs.getString("hostel"));
                    h.setHostel_id(id);
                    h.setGender_allocation(rs.getString("gender_allocation"));
                    h.setTotal_capacity(rs.getInt("total_capacity"));
                    h.setFloors(new ArrayList<>());
                    h.setRoomTypes(new ArrayList<>());
                    h.setRooms(new ArrayList<>());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return h;
            });

            RoomModel room = new RoomModel();
            room.setId(rs.getObject("room_id", UUID.class));
            room.setHostel_id(rs.getObject("r_hostel_id", UUID.class));
            room.setRoom_number(rs.getString("room_number"));
            room.setFloor(rs.getInt("floor"));
            room.setRoom_type(rs.getString("room_type"));
            room.setCapacity(rs.getInt("capacity"));
            room.setAvailable_beds(rs.getInt("available_beds"));

            hostel.getRooms().add(room);
        });

        // Derive unique floors and roomTypes per hostel
        for (HostelModel hostel : hostelMap.values()) {
            hostel.setFloors(
                    hostel.getRooms().stream()
                            .map(RoomModel::getFloor)
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList())
            );
            hostel.setRoomTypes(
                    hostel.getRooms().stream()
                            .map(RoomModel::getRoom_type)
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList())
            );
        }

        return new ArrayList<>(hostelMap.values());
    }

    public List<RoomModel> getRooms(){
        String sql =
                """
                    SELECT id, hostel_id, room_number, capacity, available_beds, room_type FROM rooms WHERE status = 'AVAILABLE'
                """;
        return jdbc.query(sql, BeanPropertyRowMapper.newInstance(RoomModel.class));
    }
}
