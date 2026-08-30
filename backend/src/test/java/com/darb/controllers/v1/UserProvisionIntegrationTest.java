package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.StudentRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProvisionIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private ParentStudentRepository parentStudentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void mosqueAdmin_provisionStudent_loginWorks_noPasswordInJson() throws Exception {
        String adminEmail = "ma-prov-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Provision Mosque");

        String email = "prov-student-" + UUID.randomUUID() + "@test.darb";
        MvcResult result = mockMvc.perform(post("/api/v1/students/provision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s",
                                  "fullName": "Prov Student",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(mosqueId, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andExpect(jsonPath("$.data.mosqueId").value(mosqueId))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk());

        UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
        assertThat(studentRepository.existsByUserIdAndMosqueId(userId, UUID.fromString(mosqueId))).isTrue();
    }

    @Test
    void superAdmin_provisionTeacher_noPasswordInJson() throws Exception {
        String superToken = createSuperAdmin("sa-prov-" + UUID.randomUUID() + "@test.darb");
        String adminEmail = "ma-for-sa-prov-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String mosqueId = onboardMosque(login(adminEmail), "SA Provision Mosque");

        String email = "prov-teacher-" + UUID.randomUUID() + "@test.darb";
        mockMvc.perform(post("/api/v1/teachers/provision")
                        .header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s",
                                  "fullName": "Prov Teacher",
                                  "email": "%s",
                                  "password": "%s",
                                  "specialization": "Tajweed"
                                }
                                """.formatted(mosqueId, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.specialization").value("Tajweed"));

        UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
        assertThat(teacherRepository.existsByUserIdAndMosqueId(userId, UUID.fromString(mosqueId))).isTrue();
    }

    @Test
    void parentInvite_accept_createsParentStudent() throws Exception {
        String adminEmail = "ma-parent-prov-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Parent Invite Mosque");

        String studentEmail = "child-" + UUID.randomUUID() + "@test.darb";
        registerRole(studentEmail, "student");
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String studentId = com.darb.support.MembershipFixtures.seatStudent(
                mockMvc, userRepository, studentRepository, adminToken, studentUserId, mosqueId, PASSWORD);

        String parentEmail = "parent-inv-" + UUID.randomUUID() + "@test.darb";
        registerRole(parentEmail, "parent");
        String parentUserId = userRepository.findByEmail(parentEmail).orElseThrow().getId().toString();

        MvcResult invite = mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "FATHER"
                                }
                                """.formatted(parentUserId, studentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.direction").value("ADMIN_INVITE"))
                .andReturn();
        String requestId = com.jayway.jsonpath.JsonPath.read(
                invite.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(post("/api/v1/mosques/join-requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + login(parentEmail)))
                .andExpect(status().isOk());

        assertThat(parentStudentRepository.existsByParent_IdAndStudent_Id(
                UUID.fromString(parentUserId), UUID.fromString(studentId))).isTrue();
    }

    private String createSuperAdmin(String email) {
        userRepository.save(User.builder()
                .fullName("Super Admin")
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build());
        try {
            return login(email);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                result.getResponse().getContentAsString(), "$.data.mosque.id");
    }
}
