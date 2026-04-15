package com.example.student_onb_svc.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final JdbcTemplate jdbc;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            List<StudentPrincipal> results = jdbc.query("""
                SELECT id, index_number, first_name, last_name, reg_no, current_step
                FROM students
                WHERE session_token = ?
                """,
                    (rs, rowNum) -> new StudentPrincipal(
                            rs.getObject("id", UUID.class),
                            rs.getString("index_number"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("reg_no"),
                            rs.getInt("current_step")
                    ), token
            );

            if (!results.isEmpty()) {
                StudentPrincipal principal = results.getFirst();
                var auth = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}