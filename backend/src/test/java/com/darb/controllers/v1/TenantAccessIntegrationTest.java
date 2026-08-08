package com.darb.controllers.v1;

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
class TenantAccessIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void mosqueAdmin_cannotCreateStudentInOtherMosque() throws Exception {
        String adminAEmail = "admin-a-create-tenant@test.darb";
        String adminBEmail = "admin-b-create-tenant@test.darb";
        registerMosqueAdmin(adminAEmail);
        registerMosqueAdmin(adminBEmail);

        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);

        onboardMosque(tokenA, "Mosque Alpha Create");
        String mosqueBId = onboardMosque(tokenB, "Mosque Beta Create");

        registerStudent("student-create-tenant@test.darb");
        String studentUserId = userRepository.findByEmail("student-create-tenant@test.darb").orElseThrow().getId().toString();

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s"
                                }
                                """.formatted(studentUserId, mosqueBId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        createStudent(tokenB, studentUserId, mosqueBId);
    }

    @Test
    void mosqueAdmin_cannotGetStudentFromOtherMosque() throws Exception {
        String adminAEmail = "admin-a-tenant@test.darb";
        String adminBEmail = "admin-b-tenant@test.darb";
        registerMosqueAdmin(adminAEmail);
        registerMosqueAdmin(adminBEmail);

        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);

        onboardMosque(tokenA, "Mosque Alpha");
        String mosqueBId = onboardMosque(tokenB, "Mosque Beta");

        registerStudent("student-b-tenant@test.darb");
        String studentUserId = userRepository.findByEmail("student-b-tenant@test.darb").orElseThrow().getId().toString();
        String studentBId = createStudent(tokenB, studentUserId, mosqueBId);

        mockMvc.perform(get("/api/v1/students/" + studentBId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void mosqueAdmin_cannotSearchUserFromOtherMosque() throws Exception {
        String adminAEmail = "admin-a-search-tenant@test.darb";
        String adminBEmail = "admin-b-search-tenant@test.darb";
        registerMosqueAdmin(adminAEmail);
        registerMosqueAdmin(adminBEmail);

        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);

        onboardMosque(tokenA, "Mosque Alpha Search");
        String mosqueBId = onboardMosque(tokenB, "Mosque Beta Search");

        registerStudent("student-b-search-tenant@test.darb");
        String studentUserId = userRepository.findByEmail("student-b-search-tenant@test.darb").orElseThrow().getId().toString();
        createStudent(tokenB, studentUserId, mosqueBId);

        mockMvc.perform(get("/api/v1/users/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "student-b-search-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void mosqueAdmin_canSearchUserInOwnMosque() throws Exception {
        String adminAEmail = "admin-a-search-own@test.darb";
        registerMosqueAdmin(adminAEmail);

        String tokenA = login(adminAEmail);

        String mosqueAId = onboardMosque(tokenA, "Mosque Alpha Search Own");

        registerStudent("student-a-search-own@test.darb");
        String studentUserId = userRepository.findByEmail("student-a-search-own@test.darb").orElseThrow().getId().toString();
        createStudent(tokenA, studentUserId, mosqueAId);

        mockMvc.perform(get("/api/v1/users/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "student-a-search-own"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Student User"))
                .andExpect(jsonPath("$.data.content[0].email").value("student-a-search-own@test.darb"));
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

    private void registerStudent(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Student User",
                                  "email": "%s",
                                  "password": "%s",
                                  "role": "student"
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

    private String onboardMosque(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "city": "Riyadh"
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.mosque.id");
    }

    private String createStudent(String adminToken, String userId, String mosqueId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s"
                                }
                                """.formatted(userId, mosqueId)))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }
}
