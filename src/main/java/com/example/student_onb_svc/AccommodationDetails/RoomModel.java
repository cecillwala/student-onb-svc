package com.example.student_onb_svc.AccommodationDetails;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RoomModel {
    private UUID id;
    private UUID hostel_id;
    private String room_number;
    private int floor;
    private String room_type;
    private int capacity;
    private int available_beds;
}
