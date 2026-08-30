package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.repositories.MosqueAdminRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkspaceProfileIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MosqueAdminRepository mosqueAdminRepository;

    @Test
    void mosqueAdminAfterOnboard_returnsProfileWithMosqueId() throws Exception {
        String email = "admin-profile@test.darb";
        registerMosqueAdmin(email);
        String token = login(email);

        mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Profile Mosque",
                                  "city": "Riyadh",
                                  "addressState": "Riyadh Province"
                                }
                                """))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();
        var admin = mosqueAdminRepository.findByUserId(user.getId()).getFirst();

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profileId").value(admin.getId().toString()))
                .andExpect(jsonPath("$.data.mosqueId").value(admin.getMosque().getId().toString()))
                .andExpect(jsonPath("$.data.membershipStatus").value("ASSIGNED"));
    }

    @Test
    void unassignedTeacher_returns404() throws Exception {
        registerTeacher("unassigned-teacher@test.darb");
        String token = login("unassigned-teacher@test.darb");

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void teacherAfterAdminCreatesProfile_returnsProfile() throws Exception {
        String teacherEmail = "assigned-teacher@test.darb";
        registerTeacher(teacherEmail);
        User teacher = userRepository.findByEmail(teacherEmail).orElseThrow();

        String adminEmail = "teacher-admin@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);

        MvcResult onboardResult = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Teacher Mosque",
                                  "city": "Jeddah",
                                  "addressState": "Makkah Province"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String mosqueId = com.jayway.jsonpath.JsonPath.read(
                onboardResult.getResponse().getContentAsString(),
                "$.data.mosque.id");

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s",
                                  "specialization": "Tajweed"
                                }
                                """.formatted(teacher.getId(), mosqueId)))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = com.jayway.jsonpath.JsonPath.read(
                inviteResult.getResponse().getContentAsString(), "$.data.id");
        String teacherToken = login(teacherEmail);
        mockMvc.perform(post("/api/v1/mosques/join-requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mosqueId").value(mosqueId))
                .andExpect(jsonPath("$.data.teacherId").isNotEmpty())
                .andExpect(jsonPath("$.data.profileId").isNotEmpty());
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

    private void registerTeacher(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Teacher User",
                                  "email": "%s",
                                  "password": "%s",
                                  "role": "teacher"
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
