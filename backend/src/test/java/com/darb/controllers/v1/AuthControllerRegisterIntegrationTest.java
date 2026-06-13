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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerRegisterIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void register_returns201WithEmptyBodyAndPersistsTeacherRole() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Teacher User",
                                  "email": "teacher-register@test.darb",
                                  "password": "P@ssw0rd1!",
                                  "role": "teacher"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        User saved = userRepository.findByEmail("teacher-register@test.darb").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(UserRole.TEACHER);
    }

    @Test
    void register_validationError_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "",
                                  "email": "not-an-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void register_invalidRole_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Bad Role",
                                  "email": "bad-role@test.darb",
                                  "password": "P@ssw0rd1!",
                                  "role": "superhero"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_mosqueAdminRole_returns201AndPersistsInDb() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Mosque Admin User",
                                  "email": "mosque-admin-register@test.darb",
                                  "password": "P@ssw0rd1!",
                                  "role": "mosque_admin"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        User saved = userRepository.findByEmail("mosque-admin-register@test.darb").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(UserRole.MOSQUE_ADMIN);
    }

    @Test
    void register_superAdminRole_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Super Admin Attempt",
                                  "email": "super-admin-attempt@test.darb",
                                  "password": "P@ssw0rd1!",
                                  "role": "super_admin"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
