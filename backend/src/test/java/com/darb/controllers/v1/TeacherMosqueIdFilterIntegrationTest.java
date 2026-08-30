package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import com.darb.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Super Admin optional {@code mosqueId} list filter on GET /api/v1/teachers,
 * plus MOSQUE_ADMIN cannot widen scope via a foreign mosqueId.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherMosqueIdFilterIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void superAdmin_withMosqueId_returnsOnlyThatMosqueTeachers() throws Exception {
        Fixture fx = seedTwoMosquesWithTeachers();

        MvcResult result = mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + fx.superToken())
                        .param("mosqueId", fx.mosqueAId())
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        List<String> ids = readTeacherIds(result);
        List<String> mosqueIds = readMosqueIds(result);

        assertThat(ids).contains(fx.teacherAId()).doesNotContain(fx.teacherBId());
        assertThat(mosqueIds).containsOnly(fx.mosqueAId());
    }

    @Test
    void superAdmin_withoutMosqueId_canSeeTeachersFromBothMosques() throws Exception {
        Fixture fx = seedTwoMosquesWithTeachers();

        MvcResult result = mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + fx.superToken())
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        List<String> ids = readTeacherIds(result);
        assertThat(ids).contains(fx.teacherAId(), fx.teacherBId());
    }

    @Test
    void mosqueAdmin_passingForeignMosqueId_stillScopedToOwnMosque() throws Exception {
        Fixture fx = seedTwoMosquesWithTeachers();

        // Foreign mosqueId must not widen: still own mosque only (not 403 — matches pageForCaller).
        MvcResult result = mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + fx.adminAToken())
                        .param("mosqueId", fx.mosqueBId())
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        List<String> ids = readTeacherIds(result);
        List<String> mosqueIds = readMosqueIds(result);

        assertThat(ids).contains(fx.teacherAId()).doesNotContain(fx.teacherBId());
        assertThat(mosqueIds).containsOnly(fx.mosqueAId());
    }

    private Fixture seedTwoMosquesWithTeachers() throws Exception {
        String token = UUID.randomUUID().toString();
        String superToken = createSuperAdmin("super-teacher-filter-" + token + "@test.darb");

        String adminAEmail = "admin-a-teacher-filter-" + token + "@test.darb";
        String adminBEmail = "admin-b-teacher-filter-" + token + "@test.darb";
        registerRole(adminAEmail, "mosque_admin");
        registerRole(adminBEmail, "mosque_admin");
        String adminAToken = login(adminAEmail);
        String adminBToken = login(adminBEmail);

        String mosqueAId = onboardMosque(adminAToken, "Teacher Filter Mosque A " + token);
        String mosqueBId = onboardMosque(adminBToken, "Teacher Filter Mosque B " + token);

        String teacherAEmail = "teacher-a-filter-" + token + "@test.darb";
        String teacherBEmail = "teacher-b-filter-" + token + "@test.darb";
        registerRole(teacherAEmail, "teacher");
        registerRole(teacherBEmail, "teacher");
        String teacherAUserId = userRepository.findByEmail(teacherAEmail).orElseThrow().getId().toString();
        String teacherBUserId = userRepository.findByEmail(teacherBEmail).orElseThrow().getId().toString();

        String teacherAId = createTeacher(adminAToken, teacherAUserId, mosqueAId);
        String teacherBId = createTeacher(adminBToken, teacherBUserId, mosqueBId);

        return new Fixture(superToken, adminAToken, mosqueAId, mosqueBId, teacherAId, teacherBId);
    }

    private record Fixture(
            String superToken,
            String adminAToken,
            String mosqueAId,
            String mosqueBId,
            String teacherAId,
            String teacherBId) {
    }

    private String createSuperAdmin(String email) throws Exception {
        User user = User.builder()
                .fullName("Super Admin")
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build();
        userRepository.save(user);
        return login(email);
    }

    private void registerRole(String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Test User",
                                  "email": "%s",
                                  "password": "%s",
                                  "role": "%s"
                                }
                                """.formatted(email, PASSWORD, role)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.accessToken");
    }

    private String onboardMosque(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "city": "Riyadh",
                                  "addressState": "Riyadh Province"
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.mosque.id");
    }

    private String createTeacher(String adminToken, String userId, String mosqueId) throws Exception {
        return com.darb.support.MembershipFixtures.seatTeacher(
                mockMvc, userRepository, teacherRepository, adminToken, userId, mosqueId, PASSWORD);
    }

    private List<String> readTeacherIds(MvcResult result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.content[*].id");
    }

    private List<String> readMosqueIds(MvcResult result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.content[*].mosqueId");
    }
}
