package com.example.student_onb_svc.AccommodationDetails;

import com.example.student_onb_svc.Security.StudentPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/onboarding")
public class AccommodationDetailsController {

    @Autowired
    AccommodationDetailsService accommodationDetailsService;
    @PostMapping("/accommodation")
    public ResponseEntity<?> saveAccommodationDetails(
            @RequestParam String token,
            @AuthenticationPrincipal StudentPrincipal principal,
            @RequestBody AccommodationDetailsRequest request
    ){

        accommodationDetailsService.save(token, request);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/hostels")
    public ResponseEntity<List<HostelModel>> getHostels(){
        return ResponseEntity.ok(accommodationDetailsService.getAccommodationDetails());
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<RoomModel>> getRooms(){
        return ResponseEntity.ok(accommodationDetailsService.getRooms());
    }
}
