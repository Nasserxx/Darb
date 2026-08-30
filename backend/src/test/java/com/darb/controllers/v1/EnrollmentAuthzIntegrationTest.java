package com.darb.controllers.v1;

import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D1 AuthZ, D2 capacity, D17 idempotent approve, D19 no activate into ENDED circle.
 * // ponytail: expects L1a EnrollmentService guards; may fail until those land
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnrollmentAuthzIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    void mosqueAdmin_createEnrollment_foreignStudentAndCircle_returns403() throws Exception {
        String tag = UUID.randomUUID().toString();
        String adminAEmail = "admin-a-enr-" + tag + "@test.darb";
        String adminBEmail = "admin-b-enr-" + tag + "@test.darb";
        registerRole(adminAEmail, "mosque_admin");
        registerRole(adminBEmail, "mosque_admin");
        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);

        onboardMosque(tokenA, "Mosque A Enr " + tag);
        String mosqueBId = onboardMosque(tokenB, "Mosque B Enr " + tag);

        String studentEmail = "student-b-enr-" + tag + "@test.darb";
        String teacherEmail = "teacher-b-enr-" + tag + "@test.darb";
        registerRole(studentEmail, "student");
        registerRole(teacherEmail, "teacher");
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String teacherUserId = userRepository.findByEmail(teacherEmail).orElseThrow().getId().toString();
        String studentBId = seatStudent(tokenB, studentUserId, mosqueBId);
        String teacherBId = seatTeacher(tokenB, teacherUserId, mosqueBId);
        String circleBId = createCircle(tokenB, mosqueBId, teacherBId, "Circle B", 20, "ACTIVE");

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "status": "ACTIVE"
                                }
                                """.formatted(studentBId, circleBId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void mosqueAdmin_secondActiveWhenCapacityOne_returns400() throws Exception {
        Fixture fx = seedOwnMosque("cap");
        String circleId = createCircle(fx.adminToken(), fx.mosqueId(), fx.teacherId(), "Cap1", 1, "ACTIVE");

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + fx.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "status": "ACTIVE"
                                }
                                """.formatted(fx.studentId(), circleId)))
                .andExpect(status().isCreated());

        String student2Email = "student2-cap-" + fx.tag() + "@test.darb";
        registerRole(student2Email, "student");
        String student2UserId = userRepository.findByEmail(student2Email).orElseThrow().getId().toString();
        String student2Id = seatStudent(fx.adminToken(), student2UserId, fx.mosqueId());

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + fx.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "status": "ACTIVE"
                                }
                                """.formatted(student2Id, circleId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mosqueAdmin_approveWhenCapacityFull_returns400() throws Exception {
        Fixture fx = seedOwnMosque("capapr");
        String circleId = createCircle(fx.adminToken(), fx.mosqueId(), fx.teacherId(), "Cap1Apr", 1, "ACTIVE");

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + fx.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "status": "ACTIVE"
                                }
                                """.formatted(fx.studentId(), circleId)))
                .andExpect(status().isCreated());

        String student2Email = "student2-capapr-" + fx.tag() + "@test.darb";
        registerRole(student2Email, "student");
        String student2UserId = userRepository.findByEmail(student2Email).orElseThrow().getId().toString();
        String student2Id = seatStudent(fx.adminToken(), student2UserId, fx.mosqueId());

        MvcResult pending = mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + fx.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "status": "PENDING"
                                }
                                """.formatted(student2Id, circleId)))
                .andExpect(status().isCreated())
                .andReturn();
        String pendingId = com.jayway.jsonpath.JsonPath.read(
                pending.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(put("/api/v1/enrollments/" + pendingId)
                        .header("Authorization", "Bearer " + fx.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mosqueAdmin_secondApproveOfActive_returns2xxOr4xxNot500() throws Exception {
        Fixture fx = seedOwnMosque("idem");
        String circleId = createCircle(fx.adminToken(), fx.mosqueId(), fx.teacherId(), "Idem", 5, "ACTIVE");

        MvcResult created = mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + fx.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "status": "ACTIVE"
                                }
                                """.formatted(fx.studentId(), circleId)))
                .andExpect(status().isCreated())
                .andReturn();
        String enrollmentId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.data.id");

        MvcResult second = mockMvc.perform(put("/api/v1/enrollments/" + enrollmentId)
                        .header("Authorization", "Bearer " + fx.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACTIVE"
                                }
                                """))
                .andReturn();

        int status = second.getResponse().getStatus();
        assertThat(status).as("idempotent re-approve must not 500").isBetween(200, 499);
    }

    @Test
    void mosqueAdmin_approveIntoEndedCircle_returns400() throws Exception {
        Fixture fx = seedOwnMosque("ended");
        String circleId = createCircle(fx.adminToken(), fx.mosqueId(), fx.teacherId(), "Ended", 5, "ACTIVE");

        MvcResult pending = mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + fx.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "status": "PENDING"
                                }
                                """.formatted(fx.studentId(), circleId)))
                .andExpect(status().isCreated())
                .andReturn();
        String enrollmentId = com.jayway.jsonpath.JsonPath.read(
                pending.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(put("/api/v1/circles/" + circleId)
                        .header("Authorization", "Bearer " + fx.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ENDED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/enrollments/" + enrollmentId)
                        .header("Authorization", "Bearer " + fx.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private Fixture seedOwnMosque(String prefix) throws Exception {
        String tag = UUID.randomUUID().toString();
        String adminEmail = "admin-" + prefix + "-" + tag + "@test.darb";
        String teacherEmail = "teacher-" + prefix + "-" + tag + "@test.darb";
        String studentEmail = "student-" + prefix + "-" + tag + "@test.darb";

        registerRole(adminEmail, "mosque_admin");
        registerRole(teacherEmail, "teacher");
        registerRole(studentEmail, "student");

        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque " + prefix + " " + tag);
        String teacherUserId = userRepository.findByEmail(teacherEmail).orElseThrow().getId().toString();
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String teacherId = seatTeacher(adminToken, teacherUserId, mosqueId);
        String studentId = seatStudent(adminToken, studentUserId, mosqueId);
        return new Fixture(tag, adminToken, mosqueId, teacherId, studentId);
    }

    private record Fixture(String tag, String adminToken, String mosqueId, String teacherId, String studentId) {
    }

    private void registerRole(String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Enr Test User",
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

    private String seatStudent(String adminToken, String userId, String mosqueId) throws Exception {
        return MembershipFixtures.seatStudent(
                mockMvc, userRepository, studentRepository, adminToken, userId, mosqueId, PASSWORD);
    }

    private String seatTeacher(String adminToken, String userId, String mosqueId) throws Exception {
        return MembershipFixtures.seatTeacher(
                mockMvc, userRepository, teacherRepository, adminToken, userId, mosqueId, PASSWORD);
    }

    private String createCircle(
            String adminToken, String mosqueId, String teacherId, String name, int capacity, String status)
            throws Exception {
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
                                  "status": "%s",
                                  "capacity": %d,
                                  "startTime": "16:00",
                                  "endTime": "18:00"
                                }
                                """.formatted(mosqueId, teacherId, name, status, capacity)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }
}
