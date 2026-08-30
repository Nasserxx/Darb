package com.darb.support;

import com.darb.entities.User;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Immediate per-mosque seat for an existing login (invite+accept is covered elsewhere). */
public final class MembershipFixtures {

    private MembershipFixtures() {
    }

    public static String seatStudent(
            MockMvc mockMvc,
            UserRepository userRepository,
            StudentRepository studentRepository,
            String adminToken,
            String userId,
            String mosqueId,
            String password) throws Exception {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        String token = login(mockMvc, user.getEmail(), password);
        mockMvc.perform(post("/api/v1/students/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated());
        return studentRepository.findByUserId(user.getId()).getFirst().getId().toString();
    }

    public static String seatTeacher(
            MockMvc mockMvc,
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            String adminToken,
            String userId,
            String mosqueId,
            String password) throws Exception {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        String token = login(mockMvc, user.getEmail(), password);
        mockMvc.perform(post("/api/v1/teachers/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated());
        return teacherRepository.findByUserId(user.getId()).getFirst().getId().toString();
    }

    public static void acceptInvite(MockMvc mockMvc, String inviteeToken, String requestId) throws Exception {
        mockMvc.perform(post("/api/v1/mosques/join-requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + inviteeToken))
                .andExpect(status().isOk());
    }

    public static void acceptParentInvite(
            MockMvc mockMvc,
            String parentEmail,
            String password,
            String requestId) throws Exception {
        acceptInvite(mockMvc, login(mockMvc, parentEmail, password), requestId);
    }

    private static String login(MockMvc mockMvc, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
