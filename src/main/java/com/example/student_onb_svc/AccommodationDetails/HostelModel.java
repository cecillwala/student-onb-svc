package com.example.student_onb_svc.AccommodationDetails;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class HostelModel {
    private String hostel;
    private UUID hostel_id;
    private String gender_allocation;
    private int total_capacity;
    private List<Integer> floors;
    private List<String> roomTypes;
    private List<RoomModel> rooms;
}
