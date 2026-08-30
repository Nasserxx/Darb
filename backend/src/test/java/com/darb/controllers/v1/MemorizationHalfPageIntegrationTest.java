package com.darb.controllers.v1;

import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import com.darb.services.MushafMapService;
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
class MemorizationHalfPageIntegrationTest extends PostgresIntegrationTestBase {

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
    private MushafMapService mushafMapService;

    @Test
    void teacher_createsAttempt_returns201WithCounts() throws Exception {
        Fixture fx = seedFixture();

        mockMvc.perform(post("/api/v1/memorization/students/" + fx.studentId() + "/attempts")
                        .header("Authorization", "Bearer " + fx.teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "circleId": "%s",
                                  "page": 1,
                                  "half": "A",
                                  "sessionDate": "2026-08-25",
                                  "grade": "GOOD",
                                  "notes": "Review madd",
                                  "stamps": [
                                    { "type": "TAJWEED", "surah": 1, "ayah": 1, "x": 0.45, "y": 0.22 },
                                    { "type": "HIFZ", "surah": 1, "ayah": 2 }
                                  ]
                                }
                                """.formatted(fx.circleId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tajweedCount").value(1))
                .andExpect(jsonPath("$.data.hifzCount").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.half").value("A"));
    }

    @Test
    void student_postAttempt_returns403() throws Exception {
        Fixture fx = seedFixture();

        mockMvc.perform(post("/api/v1/memorization/students/" + fx.studentId() + "/attempts")
                        .header("Authorization", "Bearer " + fx.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "circleId": "%s",
                                  "page": 1,
                                  "half": "A",
                                  "sessionDate": "2026-08-25",
                                  "stamps": []
                                }
                                """.formatted(fx.circleId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("memorization.assessment.staffOnly"));
    }

    @Test
    void ayahOutOfRange_returns400() throws Exception {
        Fixture fx = seedFixture();

        mockMvc.perform(post("/api/v1/memorization/students/" + fx.studentId() + "/attempts")
                        .header("Authorization", "Bearer " + fx.teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "circleId": "%s",
                                  "page": 1,
                                  "half": "A",
                                  "sessionDate": "2026-08-25",
                                  "stamps": [
                                    { "type": "TAJWEED", "surah": 2, "ayah": 1 }
                                  ]
                                }
                                """.formatted(fx.circleId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("memorization.ayahOutOfRange"));
    }

    @Test
    void lesson_assignAndGet() throws Exception {
        Fixture fx = seedFixture();

        mockMvc.perform(put("/api/v1/memorization/students/" + fx.studentId() + "/lesson")
                        .header("Authorization", "Bearer " + fx.teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "circleId": "%s",
                                  "halfPageIds": ["1-A", "1-B"],
                                  "note": "Start here"
                                }
                                """.formatted(fx.circleId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.halfPageIds[0]").value("1-A"))
                .andExpect(jsonPath("$.data.note").value("Start here"));

        mockMvc.perform(get("/api/v1/memorization/students/" + fx.studentId() + "/lesson")
                        .header("Authorization", "Bearer " + fx.studentToken())
                        .param("circleId", fx.circleId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.halfPageIds[1]").value("1-B"));
    }

    @Test
    void upsert_replacesPriorAttempt() throws Exception {
        Fixture fx = seedFixture();
        String body = """
                {
                  "circleId": "%s",
                  "page": 1,
                  "half": "A",
                  "sessionDate": "2026-08-25",
                  "stamps": [
                    { "type": "TAJWEED", "surah": 1, "ayah": 1 }
                  ]
                }
                """.formatted(fx.circleId());

        MvcResult first = mockMvc.perform(post("/api/v1/memorization/students/" + fx.studentId() + "/attempts")
                        .header("Authorization", "Bearer " + fx.teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String firstId = com.jayway.jsonpath.JsonPath.read(
                first.getResponse().getContentAsString(), "$.data.id");

        MvcResult second = mockMvc.perform(post("/api/v1/memorization/students/" + fx.studentId() + "/attempts")
                        .header("Authorization", "Bearer " + fx.teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"TAJWEED\"", "\"HIFZ\"")))
                .andExpect(status().isCreated())
                .andReturn();
        String secondId = com.jayway.jsonpath.JsonPath.read(
                second.getResponse().getContentAsString(), "$.data.id");

        org.assertj.core.api.Assertions.assertThat(secondId).isEqualTo(firstId);
        org.assertj.core.api.Assertions.assertThat((Integer) com.jayway.jsonpath.JsonPath.read(
                second.getResponse().getContentAsString(), "$.data.hifzCount")).isEqualTo(1);
    }

    @Test
    void coverage_returnsJuzProgress() throws Exception {
        Fixture fx = seedFixture();

        mockMvc.perform(post("/api/v1/memorization/students/" + fx.studentId() + "/attempts")
                        .header("Authorization", "Bearer " + fx.teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "circleId": "%s",
                                  "page": 1,
                                  "half": "A",
                                  "sessionDate": "2026-08-25",
                                  "stamps": []
                                }
                                """.formatted(fx.circleId())))
                .andExpect(status().isCreated());

        int juz1Total = mushafMapService.getHalvesForJuz(1).size();

        mockMvc.perform(get("/api/v1/memorization/students/" + fx.studentId() + "/coverage")
                        .header("Authorization", "Bearer " + fx.studentToken())
                        .param("circleId", fx.circleId())
                        .param("grain", "juz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grain").value("juz"))
                .andExpect(jsonPath("$.data.items[0].key").value("1"))
                .andExpect(jsonPath("$.data.items[0].assessed").value(1))
                .andExpect(jsonPath("$.data.items[0].total").value(juz1Total));
    }

    private Fixture seedFixture() throws Exception {
        String token = UUID.randomUUID().toString();
        String adminEmail = "admin-mem-" + token + "@test.darb";
        String teacherEmail = "teacher-mem-" + token + "@test.darb";
        String studentEmail = "student-mem-" + token + "@test.darb";

        registerRole(adminEmail, "mosque_admin");
        registerRole(teacherEmail, "teacher");
        registerRole(studentEmail, "student");

        String adminToken = login(adminEmail);
        String teacherToken = login(teacherEmail);
        String studentToken = login(studentEmail);
        String adminUserId = userRepository.findByEmail(adminEmail).orElseThrow().getId().toString();

        String mosqueId = onboardMosque(adminToken, "Mem Mosque " + token);
        String teacherUserId = userRepository.findByEmail(teacherEmail).orElseThrow().getId().toString();
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String teacherId = MembershipFixtures.seatTeacher(
                mockMvc, userRepository, teacherRepository, adminToken, teacherUserId, mosqueId, PASSWORD);
        String studentId = MembershipFixtures.seatStudent(
                mockMvc, userRepository, studentRepository, adminToken, studentUserId, mosqueId, PASSWORD);
        String circleId = createCircle(adminToken, mosqueId, teacherId, "Mem Circle");
        createEnrollment(adminToken, studentId, circleId, adminUserId);

        return new Fixture(teacherToken, studentToken, studentId, circleId);
    }

    private record Fixture(String teacherToken, String studentToken, String studentId, String circleId) {
    }

    private void registerRole(String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Mem Test User",
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
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
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
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.mosque.id");
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
                                  "level": "BEGINNER",
                                  "type": "IN_PERSON",
                                  "capacity": 20,
                                  "startTime": "16:00",
                                  "endTime": "18:00"
                                }
                                """.formatted(mosqueId, teacherId, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    private void createEnrollment(String adminToken, String studentId, String circleId, String approvedByUserId)
            throws Exception {
        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "status": "ACTIVE",
                                  "approvedBy": "%s"
                                }
                                """.formatted(studentId, circleId, approvedByUserId)))
                .andExpect(status().isCreated());
    }
}
