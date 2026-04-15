package com.example.student_onb_svc.Common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

@Component
public class Helper {

    @Autowired
    JdbcTemplate jdbc;

    public UUID getStudentId(String token){
        String hashed = sha256(token);
        return jdbc.queryForObject(
                "SELECT id FROM students WHERE magic_link_token = ?",
                UUID.class, hashed);
    }


    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
