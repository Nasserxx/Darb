package com.darb.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Darb API — Mosque Management System")
                        .description(
                                "Darb is a comprehensive mosque management platform for managing Quran study circles (Halqat). "
                                + "It handles student enrollment, attendance tracking, memorization progress, payments, "
                                + "notifications, and reporting.\n\n"
                                + "## Authentication\n"
                                + "All endpoints (except `/api/v1/auth/**`) require a JWT Bearer token in the Authorization header.\n\n"
                                + "## Roles\n"
                                + "- **SUPER_ADMIN**: Full platform access\n"
                                + "- **MOSQUE_ADMIN**: Manages a specific mosque and its resources\n"
                                + "- **TEACHER**: Manages assigned circles and student progress\n"
                                + "- **STUDENT**: Views own data and progress\n"
                                + "- **PARENT**: Views children's data\n"
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Darb Team")
                                .email("dev@darb.app")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT access token")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local development")
                ));
    }

    @Bean
    public GroupedOpenApi v1PublicApi() {
        return GroupedOpenApi.builder()
                .group("1. Auth (Public)")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1UsersApi() {
        return GroupedOpenApi.builder()
                .group("2. Users")
                .pathsToMatch("/api/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1MosquesApi() {
        return GroupedOpenApi.builder()
                .group("3. Mosques")
                .pathsToMatch("/api/v1/mosques/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1TeachersApi() {
        return GroupedOpenApi.builder()
                .group("4. Teachers")
                .pathsToMatch("/api/v1/teachers/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1StudentsApi() {
        return GroupedOpenApi.builder()
                .group("5. Students")
                .pathsToMatch("/api/v1/students/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1CirclesApi() {
        return GroupedOpenApi.builder()
                .group("6. Circles")
                .pathsToMatch("/api/v1/circles/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1EnrollmentsApi() {
        return GroupedOpenApi.builder()
                .group("7. Enrollments")
                .pathsToMatch("/api/v1/enrollments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1AttendanceApi() {
        return GroupedOpenApi.builder()
                .group("8. Attendance")
                .pathsToMatch("/api/v1/attendance/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1MemorizationApi() {
        return GroupedOpenApi.builder()
                .group("9. Memorization Progress")
                .pathsToMatch("/api/v1/memorization/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1GoalsApi() {
        return GroupedOpenApi.builder()
                .group("10. Goals")
                .pathsToMatch("/api/v1/goals/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1PaymentsApi() {
        return GroupedOpenApi.builder()
                .group("11. Payments")
                .pathsToMatch("/api/v1/payments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1NotificationsApi() {
        return GroupedOpenApi.builder()
                .group("12. Notifications")
                .pathsToMatch("/api/v1/notifications/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1MessagesApi() {
        return GroupedOpenApi.builder()
                .group("13. Messages")
                .pathsToMatch("/api/v1/messages/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1AchievementsApi() {
        return GroupedOpenApi.builder()
                .group("14. Achievements")
                .pathsToMatch("/api/v1/achievements/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1MosqueAdminsApi() {
        return GroupedOpenApi.builder()
                .group("15. Mosque Admins")
                .pathsToMatch("/api/v1/mosque-admins/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1ParentStudentsApi() {
        return GroupedOpenApi.builder()
                .group("16. Parent Student Links")
                .pathsToMatch("/api/v1/parent-students/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1ReportsApi() {
        return GroupedOpenApi.builder()
                .group("17. Reports")
                .pathsToMatch("/api/v1/reports/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v1HealthApi() {
        return GroupedOpenApi.builder()
                .group("18. Health")
                .pathsToMatch("/api/v1/health")
                .build();
    }
}
