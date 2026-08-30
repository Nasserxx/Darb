package com.darb.controllers.v1;

import com.darb.repositories.NotificationRepository;
import com.darb.repositories.StudentRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MosqueMembershipInviteIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private UUID mosqueId;
    private String adminToken;
    private String adminEmail;

    @BeforeEach
    void seedMosque() throws Exception {
        adminEmail = "ma-invite-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        adminToken = login(adminEmail);
        MvcResult result = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invite Mosque",
                                  "city": "Riyadh",
                                  "addressState": "Riyadh Province"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        mosqueId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.data.mosque.id"));
    }

    @Test
    void postStudents_createsPendingAdminInviteWithoutStudentRow() throws Exception {
        String studentEmail = "invitee-student-" + UUID.randomUUID() + "@test.darb";
        registerRole(studentEmail, "student");
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s"
                                }
                                """.formatted(studentUserId, mosqueId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.direction").value("ADMIN_INVITE"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertThat(studentRepository.findByUserId(UUID.fromString(studentUserId))).isEmpty();
        assertThat(notificationRepository.findByRecipientId(
                UUID.fromString(studentUserId),
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void secondInviteSameSlot_returns400() throws Exception {
        String studentEmail = "dup-invite-" + UUID.randomUUID() + "@test.darb";
        registerRole(studentEmail, "student");
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String body = """
                {
                  "userId": "%s",
                  "mosqueId": "%s"
                }
                """.formatted(studentUserId, mosqueId);

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "User is already a member or has a pending invitation for this mosque and role"));
    }

    @Test
    void inviteeAccept_createsStudentRow() throws Exception {
        String studentEmail = "accept-student-" + UUID.randomUUID() + "@test.darb";
        registerRole(studentEmail, "student");
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();

        MvcResult invite = mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s"
                                }
                                """.formatted(studentUserId, mosqueId)))
                .andExpect(status().isCreated())
                .andReturn();
        String requestId = com.jayway.jsonpath.JsonPath.read(
                invite.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(post("/api/v1/mosques/join-requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + login(studentEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        assertThat(studentRepository.existsByUserIdAndMosqueId(UUID.fromString(studentUserId), mosqueId)).isTrue();
    }

    @Test
    void inviteeRefuse_notifiesMosqueAdminsOnce_secondRefuse400() throws Exception {
        String studentEmail = "refuse-student-" + UUID.randomUUID() + "@test.darb";
        registerRole(studentEmail, "student");
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        UUID adminUserId = userRepository.findByEmail(adminEmail).orElseThrow().getId();

        MvcResult invite = mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s"
                                }
                                """.formatted(studentUserId, mosqueId)))
                .andExpect(status().isCreated())
                .andReturn();
        String requestId = com.jayway.jsonpath.JsonPath.read(
                invite.getResponse().getContentAsString(), "$.data.id");

        long before = notificationRepository.findByRecipientId(
                adminUserId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        String studentToken = login(studentEmail);
        mockMvc.perform(post("/api/v1/mosques/join-requests/" + requestId + "/refuse")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        long after = notificationRepository.findByRecipientId(
                adminUserId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        assertThat(after).isEqualTo(before + 1);

        mockMvc.perform(post("/api/v1/mosques/join-requests/" + requestId + "/refuse")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Join request is not pending"));
    }

    @Test
    void adminApproveOnAdminInvite_returns400() throws Exception {
        String studentEmail = "admin-approve-invite-" + UUID.randomUUID() + "@test.darb";
        registerRole(studentEmail, "student");
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();

        MvcResult invite = mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s"
                                }
                                """.formatted(studentUserId, mosqueId)))
                .andExpect(status().isCreated())
                .andReturn();
        String requestId = com.jayway.jsonpath.JsonPath.read(
                invite.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(post("/api/v1/mosque-admins/join-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only the invited user can accept this invitation"));
    }

    @Test
    void memberRequest_stillAdminApprovable() throws Exception {
        String teacherEmail = "member-req-" + UUID.randomUUID() + "@test.darb";
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
                .andExpect(jsonPath("$.data.direction").value("MEMBER_REQUEST"))
                .andReturn();
        String requestId = com.jayway.jsonpath.JsonPath.read(
                requestResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(post("/api/v1/mosque-admins/join-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void roleMismatch_returns400() throws Exception {
        String teacherEmail = "mismatch-" + UUID.randomUUID() + "@test.darb";
        registerRole(teacherEmail, "teacher");
        String teacherUserId = userRepository.findByEmail(teacherEmail).orElseThrow().getId().toString();

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s"
                                }
                                """.formatted(teacherUserId, mosqueId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listMine_returnsPendingAdminInvites() throws Exception {
        String studentEmail = "mine-invite-" + UUID.randomUUID() + "@test.darb";
        registerRole(studentEmail, "student");
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s"
                                }
                                """.formatted(studentUserId, mosqueId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/mosques/join-requests/mine")
                        .header("Authorization", "Bearer " + login(studentEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].direction").value("ADMIN_INVITE"));
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
                result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
