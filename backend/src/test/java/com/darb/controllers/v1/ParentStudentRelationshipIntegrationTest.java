package com.darb.controllers.v1;

import com.darb.repositories.StudentRepository;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
import com.darb.support.MembershipFixtures;
import com.darb.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParentStudentRelationshipIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentStudentRepository parentStudentRepository;

    @Test
    void mosqueAdmin_createWithFather_returnsFather() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Fixture fixture = setupMosqueWithParentAndStudent(suffix);

        mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + fixture.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "FATHER"
                                }
                                """.formatted(fixture.parentUserId(), fixture.studentId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.relationship").value("FATHER"));
    }

    @Test
    void mosqueAdmin_createWithInvalidRelationship_returns400() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Fixture fixture = setupMosqueWithParentAndStudent(suffix);

        mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + fixture.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "not-a-rel"
                                }
                                """.formatted(fixture.parentUserId(), fixture.studentId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mosqueAdmin_createWithParentKey_returnsParentEnum() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Fixture fixture = setupMosqueWithParentAndStudent(suffix);

        mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + fixture.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "parent"
                                }
                                """.formatted(fixture.parentUserId(), fixture.studentId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.relationship").value("PARENT"));
    }

    @Test
    void mosqueAdmin_updateStudentAndRelationship_keepsSameId() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Fixture fixture = setupMosqueWithParentAndStudent(suffix);

        String secondStudentEmail = "student2-rel-" + suffix + "@test.darb";
        registerRole(secondStudentEmail, "student");
        String secondStudentUserId = userRepository.findByEmail(secondStudentEmail).orElseThrow().getId().toString();
        String secondStudentId = createStudent(fixture.adminToken(), secondStudentUserId, fixture.mosqueId());

        MvcResult createResult = mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + fixture.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "FATHER"
                                }
                                """.formatted(fixture.parentUserId(), fixture.studentId())))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = com.jayway.jsonpath.JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.data.id");
        MembershipFixtures.acceptParentInvite(mockMvc, fixture.parentEmail(), PASSWORD, requestId);
        String linkId = parentStudentRepository.findByParentId(
                        java.util.UUID.fromString(fixture.parentUserId()))
                .getFirst()
                .getId()
                .toString();

        mockMvc.perform(put("/api/v1/parent-students/" + linkId)
                        .header("Authorization", "Bearer " + fixture.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "relationship": "MOTHER"
                                }
                                """.formatted(secondStudentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(linkId))
                .andExpect(jsonPath("$.data.studentId").value(secondStudentId))
                .andExpect(jsonPath("$.data.relationship").value("MOTHER"));

        mockMvc.perform(get("/api/v1/parent-students/" + linkId)
                        .header("Authorization", "Bearer " + fixture.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(linkId))
                .andExpect(jsonPath("$.data.studentId").value(secondStudentId))
                .andExpect(jsonPath("$.data.relationship").value("MOTHER"));
    }

    @Test
    void mosqueAdmin_duplicateParentStudentPair_returns400() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Fixture fixture = setupMosqueWithParentAndStudent(suffix);

        mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + fixture.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "FATHER"
                                }
                                """.formatted(fixture.parentUserId(), fixture.studentId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + fixture.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "MOTHER"
                                }
                                """.formatted(fixture.parentUserId(), fixture.studentId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private Fixture setupMosqueWithParentAndStudent(String suffix) throws Exception {
        String adminEmail = "admin-rel-" + suffix + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Rel " + suffix.substring(0, 8));

        String parentEmail = "parent-rel-" + suffix + "@test.darb";
        String studentEmail = "student-rel-" + suffix + "@test.darb";
        registerRole(parentEmail, "parent");
        registerRole(studentEmail, "student");
        String parentUserId = userRepository.findByEmail(parentEmail).orElseThrow().getId().toString();
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);

        return new Fixture(adminToken, mosqueId, parentUserId, parentEmail, studentId);
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
        return com.darb.support.MembershipFixtures.seatStudent(
                mockMvc, userRepository, studentRepository, adminToken, userId, mosqueId, PASSWORD);
    }

    private record Fixture(String adminToken, String mosqueId, String parentUserId, String parentEmail, String studentId) {
    }
}
