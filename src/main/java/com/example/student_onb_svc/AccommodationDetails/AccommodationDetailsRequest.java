package com.example.student_onb_svc.AccommodationDetails;

import lombok.Data;


@Data
public class AccommodationDetailsRequest {
    private String residenceType;
    private String hostelPreference;
    private String roomType;
    private String room;
    private String specialNeeds;
    private String offCampusReason;
    private String guardianAware;
    private String buildingName;
    private String floor;
    private String offCampusLocation;
    private String offCampusRoomType;
    private String landlordFirstName;
    private String landlordLastName;
    private String landlordPhone;
    private String roommateFirstName;
    private String roommateLastName;
    private String roommatePhone;
}
