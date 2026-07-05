package com.darb.controllers.v1;

import com.darb.repositories.MosqueRepository;
import com.darb.repositories.UserRepository;
import com.darb.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MosqueMemberJoinRequestIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MosqueRepository mosqueRepository;

    private UUID mosqueId;
    private String adminToken;
    private String teacherInviteCode;

    @BeforeEach
    void seedMosque() throws Exception {
        String founderEmail = "founder-join-req-" + UUID.randomUUID() + "@test.darb";
        registerRole(founderEmail, "mosque_admin");
        adminToken = login(founderEmail);

        MvcResult result = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Join Request Mosque",
                                  "city": "Riyadh"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        mosqueId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.mosque.id"));
        teacherInviteCode = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.teacherInviteCode");
    }

    @Test
    void pendingJoinRequest_returnsPendingProfile() throws Exception {
        String teacherEmail = "teacher-pending-" + UUID.randomUUID() + "@test.darb";
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

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.membershipStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.pendingMosqueName").value("Join Request Mosque"));
    }

    @Test
    void adminApproveJoinRequest_createsTeacherProfile() throws Exception {
        String teacherEmail = "teacher-approve-" + UUID.randomUUID() + "@test.darb";
        registerRole(teacherEmail, "teacher");
        String teacherToken = login(teacherEmail);

        MvcResult requestResult = mockMvc.perform(post("/api/v1/mosques/join-requests")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = com.jayway.jsonpath.JsonPath.read(
                requestResult.getResponse().getContentAsString(),
                "$.data.id");

        mockMvc.perform(post("/api/v1/mosque-admins/join-requests/%s/approve".formatted(requestId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.membershipStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.mosqueId").value(mosqueId.toString()));
    }

    @Test
    void searchMosques_doesNotExposeSensitiveFields() throws Exception {
        String teacherEmail = "teacher-search-" + UUID.randomUUID() + "@test.darb";
        registerRole(teacherEmail, "teacher");
        String teacherToken = login(teacherEmail);

        mockMvc.perform(get("/api/v1/mosques/search?q=Join")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Join Request Mosque"))
                .andExpect(jsonPath("$.data[0].city").value("Riyadh"))
                .andExpect(jsonPath("$.data[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data[0].email").doesNotExist());
    }

    @Test
    void teacherJoinByInviteCode_createsProfile() throws Exception {
        String teacherEmail = "teacher-invite-" + UUID.randomUUID() + "@test.darb";
        registerRole(teacherEmail, "teacher");
        String teacherToken = login(teacherEmail);

        mockMvc.perform(post("/api/v1/teachers/join")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "%s"
                                }
                                """.formatted(teacherInviteCode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mosqueId").value(mosqueId.toString()));
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
}
