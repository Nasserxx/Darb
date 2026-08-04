package com.darb.controllers.v1;

import com.darb.repositories.UserRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.StudentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherStudentOnboardIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID mosqueId;

    @BeforeEach
    void seedMosque() throws Exception {
        String founderEmail = "founder-ts-onboard-" + UUID.randomUUID() + "@test.darb";
        registerRole(founderEmail, "mosque_admin");
        String founderToken = login(founderEmail);

        MvcResult result = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + founderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Onboard Test Mosque",
                                  "city": "Riyadh"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        mosqueId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.mosque.id"));
    }

    @Test
    void teacherOnboard_createsProfile() throws Exception {
        String email = "teacher-onboard-ok@test.darb";
        registerRole(email, "teacher");
        String token = login(email);

        mockMvc.perform(post("/api/v1/teachers/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mosqueId").value(mosqueId.toString()));

        var user = userRepository.findByEmail(email).orElseThrow();
        assertThat(teacherRepository.findByUserId(user.getId())).hasSize(1);
    }

    @Test
    void teacherOnboard_whenAlreadyAssigned_returns403() throws Exception {
        String email = "teacher-onboard-dup@test.darb";
        registerRole(email, "teacher");
        String token = login(email);
        String body = """
                {
                  "mosqueId": "%s"
                }
                """.formatted(mosqueId);

        mockMvc.perform(post("/api/v1/teachers/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/teachers/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void studentOnboard_createsProfile() throws Exception {
        String email = "student-onboard-ok@test.darb";
        registerRole(email, "student");
        String token = login(email);

        mockMvc.perform(post("/api/v1/students/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mosqueId").value(mosqueId.toString()));
    }

    @Test
    void studentOnboard_whenAlreadyAssigned_returns403() throws Exception {
        String email = "student-onboard-dup@test.darb";
        registerRole(email, "student");
        String token = login(email);
        String body = """
                {
                  "mosqueId": "%s"
                }
                """.formatted(mosqueId);

        mockMvc.perform(post("/api/v1/students/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/students/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
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
