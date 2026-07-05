package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.MosqueAdminRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.UserRepository;
import com.darb.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MosqueOnboardIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MosqueRepository mosqueRepository;

    @Autowired
    private MosqueAdminRepository mosqueAdminRepository;

    @Test
    void onboard_createsMosquePrimaryAdminAndInviteCode() throws Exception {
        String email = "founder-onboard@test.darb";
        registerMosqueAdmin(email);
        String token = login(email);

        mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Founder Mosque",
                                  "city": "Riyadh",
                                  "timezone": "Asia/Riyadh"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mosque.name").value("Founder Mosque"))
                .andExpect(jsonPath("$.data.admin.isPrimaryAdmin").value(true))
                .andExpect(jsonPath("$.data.admin.permission").value("FULL_ACCESS"))
                .andExpect(jsonPath("$.data.inviteCode").isNotEmpty());

        User user = userRepository.findByEmail(email).orElseThrow();
        var admins = mosqueAdminRepository.findByUserId(user.getId());
        assertThat(admins).hasSize(1);
        assertThat(admins.getFirst().getIsPrimaryAdmin()).isTrue();

        var mosque = mosqueRepository.findById(admins.getFirst().getMosque().getId()).orElseThrow();
        assertThat(mosque.getSettings()).contains("adminInviteCode");
    }

    @Test
    void join_withValidInviteCodeCreatesSecondaryAdmin() throws Exception {
        String founderEmail = "founder-join@test.darb";
        registerMosqueAdmin(founderEmail);
        String founderToken = login(founderEmail);

        MvcResult onboardResult = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + founderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Shared Mosque",
                                  "city": "Jeddah"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String inviteCode = com.jayway.jsonpath.JsonPath.read(
                onboardResult.getResponse().getContentAsString(),
                "$.data.inviteCode");

        String joinerEmail = "joiner@test.darb";
        registerMosqueAdmin(joinerEmail);
        String joinerToken = login(joinerEmail);

        mockMvc.perform(get("/api/v1/mosques/join/preview")
                        .header("Authorization", "Bearer " + joinerToken)
                        .param("code", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mosqueName").value("Shared Mosque"));

        mockMvc.perform(post("/api/v1/mosques/join")
                        .header("Authorization", "Bearer " + joinerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "%s"
                                }
                                """.formatted(inviteCode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mosque.name").value("Shared Mosque"))
                .andExpect(jsonPath("$.data.admin.isPrimaryAdmin").value(false));

        User joiner = userRepository.findByEmail(joinerEmail).orElseThrow();
        var joinerAdmins = mosqueAdminRepository.findByUserId(joiner.getId());
        assertThat(joinerAdmins).hasSize(1);
        assertThat(joinerAdmins.getFirst().getIsPrimaryAdmin()).isFalse();
    }

    @Test
    void onboard_whenAlreadyAssigned_returns403() throws Exception {
        String email = "duplicate-onboard@test.darb";
        registerMosqueAdmin(email);
        String token = login(email);

        mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "First Mosque"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Second Mosque"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void join_withInvalidInviteCode_returns404() throws Exception {
        String email = "invalid-join@test.darb";
        registerMosqueAdmin(email);
        String token = login(email);

        mockMvc.perform(post("/api/v1/mosques/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "not-a-valid-code"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void preview_whenAlreadyAssigned_returns403() throws Exception {
        String founderEmail = "founder-preview-403@test.darb";
        registerMosqueAdmin(founderEmail);
        String founderToken = login(founderEmail);

        MvcResult onboardResult = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + founderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Preview Block Mosque"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String inviteCode = com.jayway.jsonpath.JsonPath.read(
                onboardResult.getResponse().getContentAsString(),
                "$.data.inviteCode");

        mockMvc.perform(get("/api/v1/mosques/join/preview")
                        .header("Authorization", "Bearer " + founderToken)
                        .param("code", inviteCode))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getMosque_doesNotLeakInviteCode() throws Exception {
        String founderEmail = "founder-leak@test.darb";
        registerMosqueAdmin(founderEmail);
        String founderToken = login(founderEmail);

        MvcResult onboardResult = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + founderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Leak Check Mosque"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String mosqueId = com.jayway.jsonpath.JsonPath.read(
                onboardResult.getResponse().getContentAsString(),
                "$.data.mosque.id");

        String studentEmail = "student-leak@test.darb";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Student User",
                                  "email": "%s",
                                  "password": "%s",
                                  "role": "student"
                                }
                                """.formatted(studentEmail, PASSWORD)))
                .andExpect(status().isCreated());

        String studentToken = login(studentEmail);

        mockMvc.perform(post("/api/v1/students/onboard")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/mosques/" + mosqueId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("adminInviteCode"));
    }

    @Test
    void onboard_asTeacher_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Teacher User",
                                  "email": "teacher-onboard@test.darb",
                                  "password": "%s",
                                  "role": "teacher"
                                }
                                """.formatted(PASSWORD)))
                .andExpect(status().isCreated());

        String token = login("teacher-onboard@test.darb");

        mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Should Fail"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private void registerMosqueAdmin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Mosque Admin",
                                  "email": "%s",
                                  "password": "%s",
                                  "role": "mosque_admin"
                                }
                                """.formatted(email, PASSWORD)))
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
