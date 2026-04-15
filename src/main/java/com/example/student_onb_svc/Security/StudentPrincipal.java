package com.example.student_onb_svc.Security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class StudentPrincipal implements UserDetails {

    private final UUID studentId;
    private final String indexNumber;
    private final String firstName;
    private final String lastName;
    private final String regNo;
    private final int currentStep;

    public StudentPrincipal(UUID studentId, String indexNumber, String firstName,
                            String lastName, String regNo, int currentStep) {
        this.studentId = studentId;
        this.indexNumber = indexNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.regNo = regNo;
        this.currentStep = currentStep;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }

    @Override public String getUsername() { return indexNumber; }
    @Override public String getPassword() { return ""; } // no password — session token auth
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}