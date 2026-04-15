package com.example.student_onb_svc.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        String schemeName = "Session Token";

        return new OpenAPI()
                .info(new Info()
                        .title("Student Onboarding Service API")
                        .description("REST API for the Egerton University Student Onboarding Portal. "
                                + "Handles magic link verification, onboarding wizard steps, "
                                + "file uploads, and status tracking.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Lwala Cecil Joel Munala")
                                .email("admin@egerton.ac.ke")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Session Token")
                                .description("Paste the session token from /api/v1/onboarding/verify-identity")));
    }
}