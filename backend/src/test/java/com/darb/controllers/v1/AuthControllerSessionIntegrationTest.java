package com.darb.controllers.v1;

import com.darb.support.PostgresIntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerSessionIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void changePassword_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "old-password",
                                  "newPassword": "NewP@ssw0rd1!"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_logout_thenRefreshWithOldToken_returns401() throws Exception {
        String email = "session-logout@test.darb";
        register(email);

        MvcResult login = login(email);
        String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
        String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.data.refreshToken");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginTwice_firstRefreshToken_returns401_second_returns200() throws Exception {
        String email = "session-relogin@test.darb";
        register(email);

        String firstRefresh = JsonPath.read(login(email).getResponse().getContentAsString(), "$.data.refreshToken");
        String secondRefresh = JsonPath.read(login(email).getResponse().getContentAsString(), "$.data.refreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(firstRefresh)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(secondRefresh)))
                .andExpect(status().isOk());
    }

    @Test
    void register_sameEmailDifferentCase_returns409() throws Exception {
        register("Case.Email@test.darb");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Duplicate Case",
                                  "email": "case.email@test.darb",
                                  "password": "%s",
                                  "role": "student"
                                }
                                """.formatted(PASSWORD)))
                .andExpect(status().isConflict());
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Session Test User",
                                  "email": "%s",
                                  "password": "%s",
                                  "role": "student"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private MvcResult login(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
    }
}
