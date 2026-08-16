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
class MessageIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByCircle_includesSenderReceiverAndCircleNames() throws Exception {
        String adminEmail = "admin-names@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Names Mosque");

        String teacherEmail = "teacher-names@test.darb";
        registerUser(teacherEmail, "Teacher User", "teacher");
        String teacherUserId = userRepository.findByEmail(teacherEmail).orElseThrow().getId().toString();
        String teacherId = createTeacher(adminToken, teacherUserId, mosqueId);

        String senderEmail = "sender-names@test.darb";
        String receiverEmail = "receiver-names@test.darb";
        registerUser(senderEmail, "Sender User", "student");
        registerUser(receiverEmail, "Receiver User", "student");
        String senderUserId = userRepository.findByEmail(senderEmail).orElseThrow().getId().toString();
        String receiverUserId = userRepository.findByEmail(receiverEmail).orElseThrow().getId().toString();
        createStudent(adminToken, senderUserId, mosqueId);
        createStudent(adminToken, receiverUserId, mosqueId);

        String circleId = createCircle(adminToken, mosqueId, teacherId, "Names Circle");

        String senderToken = login(senderEmail);
        sendMessage(senderToken, receiverUserId, circleId, "Salam, check the homework.");

        mockMvc.perform(get("/api/v1/messages/circle/" + circleId)
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].senderName").value("Sender User"))
                .andExpect(jsonPath("$.data.content[0].receiverName").value("Receiver User"))
                .andExpect(jsonPath("$.data.content[0].circleName").value("Names Circle"));
    }

    @Test
    void callerFromAnotherMosque_cannotReadCircleMessages() throws Exception {
        String adminAEmail = "admin-a-messages@test.darb";
        String adminBEmail = "admin-b-messages@test.darb";
        registerMosqueAdmin(adminAEmail);
        registerMosqueAdmin(adminBEmail);
        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);
        String mosqueAId = onboardMosque(tokenA, "Mosque A Messages");
        onboardMosque(tokenB, "Mosque B Messages");

        String teacherEmail = "teacher-a-messages@test.darb";
        registerUser(teacherEmail, "Teacher A", "teacher");
        String teacherUserId = userRepository.findByEmail(teacherEmail).orElseThrow().getId().toString();
        String teacherId = createTeacher(tokenA, teacherUserId, mosqueAId);

        String senderEmail = "sender-a-messages@test.darb";
        String receiverEmail = "receiver-a-messages@test.darb";
        registerUser(senderEmail, "Sender A", "student");
        registerUser(receiverEmail, "Receiver A", "student");
        String senderUserId = userRepository.findByEmail(senderEmail).orElseThrow().getId().toString();
        String receiverUserId = userRepository.findByEmail(receiverEmail).orElseThrow().getId().toString();
        createStudent(tokenA, senderUserId, mosqueAId);
        createStudent(tokenA, receiverUserId, mosqueAId);

        String circleId = createCircle(tokenA, mosqueAId, teacherId, "Circle A Messages");

        String senderToken = login(senderEmail);
        sendMessage(senderToken, receiverUserId, circleId, "Private message");

        mockMvc.perform(get("/api/v1/messages/circle/" + circleId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void registerMosqueAdmin(String email) throws Exception {
        registerUser(email, "Mosque Admin", "mosque_admin");
    }

    private void registerUser(String email, String fullName, String role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "%s",
                                  "email": "%s",
                                  "password": "%s",
                                  "role": "%s"
                                }
                                """.formatted(fullName, email, PASSWORD, role)))
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
                                  "city": "Riyadh",
                                  "addressState": "Riyadh Province"
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

    private String createTeacher(String adminToken, String userId, String mosqueId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/teachers")
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

    private String createCircle(String adminToken, String mosqueId, String teacherId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/circles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s",
                                  "teacherId": "%s",
                                  "name": "%s",
                                  "level": "INTERMEDIATE",
                                  "type": "IN_PERSON",
                                  "capacity": 15,
                                  "startTime": "16:00",
                                  "endTime": "18:00"
                                }
                                """.formatted(mosqueId, teacherId, name)))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }

    private void sendMessage(String senderToken, String receiverId, String circleId, String content) throws Exception {
        mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senderId": "00000000-0000-0000-0000-000000000000",
                                  "receiverId": "%s",
                                  "circleId": "%s",
                                  "content": "%s"
                                }
                                """.formatted(receiverId, circleId, content)))
                .andExpect(status().isCreated());
    }
}
