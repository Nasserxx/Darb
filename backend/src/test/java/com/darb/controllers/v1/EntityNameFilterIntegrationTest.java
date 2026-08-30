package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import com.darb.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EntityNameFilterIntegrationTest extends PostgresIntegrationTestBase {

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
    private PasswordEncoder passwordEncoder;

    @Test
    void superAdmin_enrollmentMosqueId_scopesToMosque() throws Exception {
        Fixture fx = seedEnrollmentFixture();

        MvcResult scoped = mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + fx.superToken())
                        .param("mosqueId", fx.mosqueAId())
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        List<String> ids = readEnrollmentIds(scoped);
        assertThat(ids).contains(fx.enrollmentAId()).doesNotContain(fx.enrollmentBId());
    }

    @Test
    void superAdmin_enrollmentNameFilter_matchesStudentName() throws Exception {
        Fixture fx = seedEnrollmentFixture();

        MvcResult result = mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + fx.superToken())
                        .param("mosqueId", fx.mosqueAId())
                        .param("q", "UniqueStudentAlpha")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        List<String> ids = readEnrollmentIds(result);
        assertThat(ids).containsExactly(fx.enrollmentAId());
    }

    @Test
    void superAdmin_nameFilterWithoutMosqueId_returns400() throws Exception {
        Fixture fx = seedEnrollmentFixture();

        mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + fx.superToken())
                        .param("q", "UniqueStudentAlpha")
                        .param("size", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void superAdmin_teacherNameFilter_matchesTeacherInMosque() throws Exception {
        Fixture fx = seedEnrollmentFixture();

        MvcResult result = mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + fx.superToken())
                        .param("mosqueId", fx.mosqueAId())
                        .param("q", "Teacher Alpha")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn();

        List<String> ids = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.content[*].id");
        assertThat(ids).containsExactly(fx.teacherAId());
    }

    private Fixture seedEnrollmentFixture() throws Exception {
        String token = UUID.randomUUID().toString();
        String superToken = createSuperAdmin("super-name-filter-" + token + "@test.darb");

        String adminAEmail = "admin-a-name-filter-" + token + "@test.darb";
        String adminBEmail = "admin-b-name-filter-" + token + "@test.darb";
        registerRole(adminAEmail, "mosque_admin");
        registerRole(adminBEmail, "mosque_admin");
        String adminAToken = login(adminAEmail);
        String adminBToken = login(adminBEmail);
        String adminAUserId = userRepository.findByEmail(adminAEmail).orElseThrow().getId().toString();
        String adminBUserId = userRepository.findByEmail(adminBEmail).orElseThrow().getId().toString();

        String mosqueAId = onboardMosque(adminAToken, "Name Filter Mosque A " + token);
        String mosqueBId = onboardMosque(adminBToken, "Name Filter Mosque B " + token);

        String teacherAEmail = "teacher-a-name-" + token + "@test.darb";
        String teacherBEmail = "teacher-b-name-" + token + "@test.darb";
        registerRoleWithName(teacherAEmail, "teacher", "Teacher Alpha");
        registerRoleWithName(teacherBEmail, "teacher", "Teacher Beta");
        String teacherAUserId = userRepository.findByEmail(teacherAEmail).orElseThrow().getId().toString();
        String teacherBUserId = userRepository.findByEmail(teacherBEmail).orElseThrow().getId().toString();
        String teacherAId = createTeacher(adminAToken, teacherAUserId, mosqueAId);
        String teacherBId = createTeacher(adminBToken, teacherBUserId, mosqueBId);

        String studentAEmail = "student-a-name-" + token + "@test.darb";
        String studentBEmail = "student-b-name-" + token + "@test.darb";
        registerRoleWithName(studentAEmail, "student", "UniqueStudentAlpha");
        registerRoleWithName(studentBEmail, "student", "UniqueStudentBeta");
        String studentAUserId = userRepository.findByEmail(studentAEmail).orElseThrow().getId().toString();
        String studentBUserId = userRepository.findByEmail(studentBEmail).orElseThrow().getId().toString();
        String studentAId = createStudent(adminAToken, studentAUserId, mosqueAId);
        String studentBId = createStudent(adminBToken, studentBUserId, mosqueBId);

        String circleAId = createCircle(adminAToken, mosqueAId, teacherAId, "Circle A");
        String circleBId = createCircle(adminBToken, mosqueBId, teacherBId, "Circle B");

        String enrollmentAId = createEnrollment(adminAToken, studentAId, circleAId, adminAUserId);
        String enrollmentBId = createEnrollment(adminBToken, studentBId, circleBId, adminBUserId);

        return new Fixture(
                superToken,
                mosqueAId,
                mosqueBId,
                teacherAId,
                teacherBId,
                enrollmentAId,
                enrollmentBId);
    }

    private record Fixture(
            String superToken,
            String mosqueAId,
            String mosqueBId,
            String teacherAId,
            String teacherBId,
            String enrollmentAId,
            String enrollmentBId) {
    }

    private String createSuperAdmin(String email) throws Exception {
        User user = User.builder()
                .fullName("Super Admin")
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build();
        userRepository.save(user);
        return login(email);
    }

    private void registerRole(String email, String role) throws Exception {
        registerRoleWithName(email, role, "Test User");
    }

    private void registerRoleWithName(String email, String role, String fullName) throws Exception {
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

    private String createTeacher(String adminToken, String userId, String mosqueId) throws Exception {
        return com.darb.support.MembershipFixtures.seatTeacher(
                mockMvc, userRepository, teacherRepository, adminToken, userId, mosqueId, PASSWORD);
    }

    private String createStudent(String adminToken, String userId, String mosqueId) throws Exception {
        return com.darb.support.MembershipFixtures.seatStudent(
                mockMvc, userRepository, studentRepository, adminToken, userId, mosqueId, PASSWORD);
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

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }

    private String createEnrollment(
            String adminToken,
            String studentId,
            String circleId,
            String approvedByUserId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/enrollments")
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
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }

    private List<String> readEnrollmentIds(MvcResult result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.content[*].id");
    }
}
