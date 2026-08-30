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
        assertEquals("MEMBER_REQUEST", matches.get(0).get("direction"));
        assertEquals(mosqueName, matches.get(0).get("mosqueName"));
        assertEquals(1, matches.get(0).get("densityScore"));
        String summary = (String) matches.get(0).get("summary");
        org.junit.jupiter.api.Assertions.assertTrue(
                summary != null && summary.contains("requested to join as"),
                "expected MEMBER_REQUEST summary wording, got: " + summary);
    }

    @Test
    void superAdmin_approveMemberRequest_withoutAudit_returns400() throws Exception {
        String superAdminToken = createSuperAdmin("super-join-no-audit-" + UUID.randomUUID() + "@test.darb");
        String adminEmail = "admin-join-no-audit-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        UUID mosqueId = onboardMosque(adminToken, "Join No Audit Mosque");
        UUID requestId = createJoinRequestReturningId(
                "teacher-no-audit-" + UUID.randomUUID() + "@test.darb", mosqueId);

        mockMvc.perform(post("/api/v1/mosque-admins/join-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void superAdmin_approveMemberRequest_withAudit_returns200() throws Exception {
        String superAdminToken = createSuperAdmin("super-join-audit-" + UUID.randomUUID() + "@test.darb");
        String adminEmail = "admin-join-audit-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        UUID mosqueId = onboardMosque(adminToken, "Join Audit Mosque");
        UUID requestId = createJoinRequestReturningId(
                "teacher-audit-" + UUID.randomUUID() + "@test.darb", mosqueId);

        mockMvc.perform(post("/api/v1/mosque-admins/join-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", "QA force approve pending join"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.direction").value("MEMBER_REQUEST"));
    }

    @Test
    void superAdmin_forceAcceptInvite_withAudit_returns200() throws Exception {
        String superAdminToken = createSuperAdmin("super-force-accept-" + UUID.randomUUID() + "@test.darb");
        String adminEmail = "admin-force-accept-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        UUID mosqueId = onboardMosque(adminToken, "Force Accept Mosque");

        String teacherEmail = "invitee-force-" + UUID.randomUUID() + "@test.darb";
        registerRole(teacherEmail, "teacher");
        UUID teacherUserId = userRepository.findByEmail(teacherEmail).orElseThrow().getId();

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s"
                                }
                                """.formatted(teacherUserId, mosqueId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.direction").value("ADMIN_INVITE"))
                .andReturn();
        UUID inviteId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                inviteResult.getResponse().getContentAsString(), "$.data.id"));

        MvcResult stuckResult = mockMvc.perform(get("/api/v1/admin/stuck-work")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> stuckWorkData = com.jayway.jsonpath.JsonPath.read(
                stuckResult.getResponse().getContentAsString(), "$.data");
        Map<String, Object> inviteItem = stuckWorkData.stream()
                .filter(item -> inviteId.toString().equals(item.get("resourceId")))
                .findFirst()
                .orElseThrow();
        assertEquals("ADMIN_INVITE", inviteItem.get("direction"));
        org.junit.jupiter.api.Assertions.assertTrue(
                ((String) inviteItem.get("summary")).contains("invited as"));

        mockMvc.perform(post("/api/v1/mosques/join-requests/" + inviteId + "/accept")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", "QA force accept invitee seat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.direction").value("ADMIN_INVITE"));
    }

    @Test
    void superAdmin_approveOnAdminInvite_returns400() throws Exception {
        String superAdminToken = createSuperAdmin("super-wrong-verb-" + UUID.randomUUID() + "@test.darb");
        String adminEmail = "admin-wrong-verb-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        UUID mosqueId = onboardMosque(adminToken, "Wrong Verb Mosque");

        String teacherEmail = "invitee-wrong-" + UUID.randomUUID() + "@test.darb";
        registerRole(teacherEmail, "teacher");
        UUID teacherUserId = userRepository.findByEmail(teacherEmail).orElseThrow().getId();

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s"
                                }
                                """.formatted(teacherUserId, mosqueId)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID inviteId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                inviteResult.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(post("/api/v1/mosque-admins/join-requests/" + inviteId + "/approve")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", "QA wrong direction verb"))
                .andExpect(status().isBadRequest());
    }

    private UUID createJoinRequestReturningId(String teacherEmail, UUID mosqueId) throws Exception {
        registerRole(teacherEmail, "teacher");
        String teacherToken = login(teacherEmail);

        MvcResult result = mockMvc.perform(post("/api/v1/mosques/join-requests")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.data.id"));
    }
}
