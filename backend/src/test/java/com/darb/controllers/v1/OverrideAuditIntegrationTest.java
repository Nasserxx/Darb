package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.OverrideAuditLogRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OverrideAuditIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";
    private static final String VALID_AUDIT_REASON = "Break-glass parent link repair";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OverrideAuditLogRepository overrideAuditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void superAdmin_deleteParentStudent_withoutAuditReason_returns400() throws Exception {
        String superAdminEmail = "sa-no-reason-delete@test.darb";
        createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-audit-delete@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Audit Delete");

        registerParent("parent-audit-delete@test.darb");
        registerStudent("student-audit-delete@test.darb");
        String parentUserId = userRepository.findByEmail("parent-audit-delete@test.darb").orElseThrow().getId().toString();
        String studentUserId = userRepository.findByEmail("student-audit-delete@test.darb").orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);
        String linkId = createParentStudentLink(adminToken, parentUserId, studentId);

        String superAdminToken = login(superAdminEmail);
        long auditCountBefore = overrideAuditLogRepository.count();

        mockMvc.perform(delete("/api/v1/parent-students/" + linkId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Audit reason required (min 8 characters) for super admin override"));

        assertThat(overrideAuditLogRepository.count()).isEqualTo(auditCountBefore);
    }

    @Test
    void superAdmin_deleteParentStudent_withAuditReason_returns200AndPersistsAuditRow() throws Exception {
        String superAdminEmail = "sa-with-reason-delete@test.darb";
        User superAdmin = createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-audit-ok-delete@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Audit OK Delete");

        registerParent("parent-audit-ok-delete@test.darb");
        registerStudent("student-audit-ok-delete@test.darb");
        String parentUserId = userRepository.findByEmail("parent-audit-ok-delete@test.darb").orElseThrow().getId().toString();
        String studentUserId = userRepository.findByEmail("student-audit-ok-delete@test.darb").orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);
        String linkId = createParentStudentLink(adminToken, parentUserId, studentId);

        String superAdminToken = login(superAdminEmail);
        long auditCountBefore = overrideAuditLogRepository.count();

        mockMvc.perform(delete("/api/v1/parent-students/" + linkId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", VALID_AUDIT_REASON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(overrideAuditLogRepository.count()).isEqualTo(auditCountBefore + 1);
        var auditRow = overrideAuditLogRepository.findAll().stream()
                .filter(row -> row.getResourceId().toString().equals(linkId))
                .findFirst()
                .orElseThrow();
        assertThat(auditRow.getActor().getId()).isEqualTo(superAdmin.getId());
        assertThat(auditRow.getMosque().getId().toString()).isEqualTo(mosqueId);
        assertThat(auditRow.getAction()).isEqualTo("PARENT_STUDENT_DELETE");
        assertThat(auditRow.getReason()).isEqualTo(VALID_AUDIT_REASON);
        assertThat(auditRow.getResourceType()).isEqualTo("ParentStudent");
    }

    @Test
    void superAdmin_createParentStudent_withoutAuditReason_returns400() throws Exception {
        String superAdminEmail = "sa-no-reason-create@test.darb";
        createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-audit-create@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Audit Create");

        registerParent("parent-audit-create@test.darb");
        registerStudent("student-audit-create@test.darb");
        String parentUserId = userRepository.findByEmail("parent-audit-create@test.darb").orElseThrow().getId().toString();
        String studentUserId = userRepository.findByEmail("student-audit-create@test.darb").orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);

        String superAdminToken = login(superAdminEmail);

        mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "parent"
                                }
                                """.formatted(parentUserId, studentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void superAdmin_updateEnrollmentStatus_withBodyAuditReason_returns200AndPersistsAuditRow() throws Exception {
        String superAdminEmail = "sa-enrollment-audit@test.darb";
        User superAdmin = createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-enrollment-audit@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Enrollment Audit");

        registerStudent("student-enrollment-audit@test.darb");
        registerTeacher("teacher-enrollment-audit@test.darb");
        String studentUserId = userRepository.findByEmail("student-enrollment-audit@test.darb").orElseThrow().getId().toString();
        String teacherUserId = userRepository.findByEmail("teacher-enrollment-audit@test.darb").orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);
        String teacherId = createTeacher(adminToken, teacherUserId, mosqueId);
        String circleId = createCircle(adminToken, mosqueId, teacherId, "Circle Enrollment Audit");
        String enrollmentId = createEnrollment(adminToken, studentId, circleId);

        String superAdminToken = login(superAdminEmail);

        mockMvc.perform(put("/api/v1/enrollments/" + enrollmentId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "WITHDRAWN",
                                  "auditReason": "Force withdraw after transfer request"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        var auditRow = overrideAuditLogRepository.findAll().stream()
                .filter(row -> row.getResourceId().toString().equals(enrollmentId))
                .findFirst()
                .orElseThrow();
        assertThat(auditRow.getActor().getId()).isEqualTo(superAdmin.getId());
        assertThat(auditRow.getMosque().getId().toString()).isEqualTo(mosqueId);
        assertThat(auditRow.getAction()).isEqualTo("ENROLLMENT_STATUS_FORCE");
        assertThat(auditRow.getReason()).isEqualTo("Force withdraw after transfer request");
        assertThat(auditRow.getResourceType()).isEqualTo("Enrollment");
    }

    @Test
    void mosqueAdmin_createParentStudent_withoutAuditReason_succeeds() throws Exception {
        String adminEmail = "admin-no-audit-needed@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque No Audit Needed");

        registerParent("parent-no-audit@test.darb");
        registerStudent("student-no-audit@test.darb");
        String parentUserId = userRepository.findByEmail("parent-no-audit@test.darb").orElseThrow().getId().toString();
        String studentUserId = userRepository.findByEmail("student-no-audit@test.darb").orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);

        mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "parent"
                                }
                                """.formatted(parentUserId, studentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    private User createSuperAdmin(String email) {
        User user = User.builder()
                .fullName("Super Admin")
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build();
        return userRepository.save(user);
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

    private void registerParent(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Parent User",
                                  "email": "%s",
                                  "password": "%s",
                                  "role": "parent"
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

    private void registerTeacher(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Teacher User",
                                  "email": "%s",
                                  "password": "%s",
                                  "role": "teacher"
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
                                  "mosqueId": "%s",
                                  "specialization": "Hifz"
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

    private String createEnrollment(String adminToken, String studentId, String circleId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "status": "ACTIVE"
                                }
                                """.formatted(studentId, circleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentName").value("Student User"))
                .andExpect(jsonPath("$.data.circleName").isNotEmpty())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }

    private String createParentStudentLink(String adminToken, String parentUserId, String studentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "parent"
                                }
                                """.formatted(parentUserId, studentId)))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }
}
