package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StuckWorkControllerIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void superAdmin_getStuckWork_returnsPendingJoinsAcrossMosques() throws Exception {
        String superAdminEmail = "super-stuck-" + UUID.randomUUID() + "@test.darb";
        String superAdminToken = createSuperAdmin(superAdminEmail);

        String adminAEmail = "admin-a-stuck-" + UUID.randomUUID() + "@test.darb";
        String adminBEmail = "admin-b-stuck-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminAEmail, "mosque_admin");
        registerRole(adminBEmail, "mosque_admin");

        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);

        UUID mosqueAId = onboardMosque(tokenA, "Mosque Alpha Stuck");
        UUID mosqueBId = onboardMosque(tokenB, "Mosque Beta Stuck");

        createJoinRequest("teacher-a-stuck-" + UUID.randomUUID() + "@test.darb", mosqueAId);
        createJoinRequest("teacher-b-stuck-" + UUID.randomUUID() + "@test.darb", mosqueBId);

        MvcResult stuckWorkResult = mockMvc.perform(get("/api/v1/admin/stuck-work")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // ponytail: one shared container across test classes can leave other pending joins in the
        // DB, so locate this test's own items instead of asserting an exact global count
        List<Map<String, Object>> stuckWorkData = com.jayway.jsonpath.JsonPath.read(
                stuckWorkResult.getResponse().getContentAsString(), "$.data");
        assertPendingJoin(stuckWorkData, mosqueAId, "Mosque Alpha Stuck");
        assertPendingJoin(stuckWorkData, mosqueBId, "Mosque Beta Stuck");
    }

    @Test
    void mosqueAdmin_getStuckWork_returns403() throws Exception {
        String adminEmail = "admin-stuck-forbidden-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        onboardMosque(adminToken, "Forbidden Stuck Mosque");

        mockMvc.perform(get("/api/v1/admin/stuck-work")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
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

    private UUID onboardMosque(String adminToken, String mosqueName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "city": "Riyadh",
                                  "addressState": "Riyadh Province"
                                }
                                """.formatted(mosqueName)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.mosque.id"));
    }

    private void createJoinRequest(String teacherEmail, UUID mosqueId) throws Exception {
        registerRole(teacherEmail, "teacher");
        String teacherToken = login(teacherEmail);

        mockMvc.perform(post("/api/v1/mosques/join-requests")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated());
    }

    private void assertPendingJoin(List<Map<String, Object>> stuckWorkData, UUID mosqueId, String mosqueName) {
        List<Map<String, Object>> matches = stuckWorkData.stream()
                .filter(item -> mosqueId.toString().equals(item.get("mosqueId")))
                .toList();
        assertEquals(1, matches.size(), "expected exactly one pending join for " + mosqueName);
        assertEquals("PENDING_JOIN", matches.get(0).get("kind"));
        assertEquals(mosqueName, matches.get(0).get("mosqueName"));
        assertEquals(1, matches.get(0).get("densityScore"));
    }
}
