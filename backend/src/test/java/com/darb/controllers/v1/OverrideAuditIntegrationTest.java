package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.OverrideAuditLogRepository;
import com.darb.repositories.ParentStudentRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ParentStudentRepository parentStudentRepository;

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

    @Test
    void superAdmin_deleteMosque_withoutAuditReason_returns400() throws Exception {
        String superAdminEmail = "sa-mosque-delete-no-reason@test.darb";
        createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-mosque-delete-no-reason@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Delete No Reason");

        String superAdminToken = login(superAdminEmail);
        mockMvc.perform(delete("/api/v1/mosques/" + mosqueId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Audit reason required (min 8 characters) for super admin override"));
    }

    @Test
    void superAdmin_deleteMosque_withAuditReason_returns200AndPersistsAuditRow() throws Exception {
        String superAdminEmail = "sa-mosque-delete-ok@test.darb";
        User superAdmin = createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-mosque-delete-ok@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Delete OK");

        String superAdminToken = login(superAdminEmail);
        long auditCountBefore = overrideAuditLogRepository.count();

        mockMvc.perform(delete("/api/v1/mosques/" + mosqueId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", VALID_AUDIT_REASON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(overrideAuditLogRepository.count()).isEqualTo(auditCountBefore + 1);
        var auditRow = overrideAuditLogRepository.findAll().stream()
                .filter(row -> row.getResourceId().toString().equals(mosqueId)
                        && "MOSQUE_DEACTIVATE".equals(row.getAction()))
                .findFirst()
                .orElseThrow();
        assertThat(auditRow.getActor().getId()).isEqualTo(superAdmin.getId());
        assertThat(auditRow.getReason()).isEqualTo(VALID_AUDIT_REASON);
        assertThat(auditRow.getResourceType()).isEqualTo("Mosque");
    }

    @Test
    void superAdmin_deleteCircle_withoutAuditReason_returns400() throws Exception {
        String superAdminEmail = "sa-circle-delete-no-reason@test.darb";
        createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-circle-delete-no-reason@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Circle Delete No Reason");

        registerTeacher("teacher-circle-delete-no-reason@test.darb");
        String teacherUserId = userRepository.findByEmail("teacher-circle-delete-no-reason@test.darb")
                .orElseThrow().getId().toString();
        String teacherId = createTeacher(adminToken, teacherUserId, mosqueId);
        String circleId = createCircle(adminToken, mosqueId, teacherId, "Circle Delete No Reason");

        String superAdminToken = login(superAdminEmail);
        mockMvc.perform(delete("/api/v1/circles/" + circleId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Audit reason required (min 8 characters) for super admin override"));
    }

    @Test
    void superAdmin_deleteCircle_withAuditReason_returns200AndPersistsAuditRow() throws Exception {
        String superAdminEmail = "sa-circle-delete-ok@test.darb";
        User superAdmin = createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-circle-delete-ok@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Circle Delete OK");

        registerTeacher("teacher-circle-delete-ok@test.darb");
        String teacherUserId = userRepository.findByEmail("teacher-circle-delete-ok@test.darb")
                .orElseThrow().getId().toString();
        String teacherId = createTeacher(adminToken, teacherUserId, mosqueId);
        String circleId = createCircle(adminToken, mosqueId, teacherId, "Circle Delete OK");

        String superAdminToken = login(superAdminEmail);
        long auditCountBefore = overrideAuditLogRepository.count();

        mockMvc.perform(delete("/api/v1/circles/" + circleId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", VALID_AUDIT_REASON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(overrideAuditLogRepository.count()).isEqualTo(auditCountBefore + 1);
        var auditRow = overrideAuditLogRepository.findAll().stream()
                .filter(row -> row.getResourceId().toString().equals(circleId)
                        && "CIRCLE_DEACTIVATE".equals(row.getAction()))
                .findFirst()
                .orElseThrow();
        assertThat(auditRow.getActor().getId()).isEqualTo(superAdmin.getId());
        assertThat(auditRow.getMosque().getId().toString()).isEqualTo(mosqueId);
        assertThat(auditRow.getReason()).isEqualTo(VALID_AUDIT_REASON);
        assertThat(auditRow.getResourceType()).isEqualTo("Circle");
    }

    @Test
    void mosqueAdmin_deleteCircle_withoutAuditReason_succeeds() throws Exception {
        String adminEmail = "admin-circle-delete-no-sa@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Circle Delete MA");

        registerTeacher("teacher-circle-delete-ma@test.darb");
        String teacherUserId = userRepository.findByEmail("teacher-circle-delete-ma@test.darb")
                .orElseThrow().getId().toString();
        String teacherId = createTeacher(adminToken, teacherUserId, mosqueId);
        String circleId = createCircle(adminToken, mosqueId, teacherId, "Circle Delete MA");

        mockMvc.perform(delete("/api/v1/circles/" + circleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void superAdmin_mutateInactiveMosqueAdmin_returns400_activeSoftLeaveStill200() throws Exception {
        String superAdminEmail = "sa-inactive-ma@test.darb";
        createSuperAdmin(superAdminEmail);

        String primaryEmail = "admin-inactive-ma-primary@test.darb";
        String secondEmail = "admin-inactive-ma-second@test.darb";
        registerMosqueAdmin(primaryEmail);
        registerMosqueAdmin(secondEmail);
        String primaryToken = login(primaryEmail);
        String mosqueId = onboardMosque(primaryToken, "Mosque Inactive MA");

        String secondUserId = userRepository.findByEmail(secondEmail).orElseThrow().getId().toString();
        String superAdminToken = login(superAdminEmail);

        MvcResult assignResult = mockMvc.perform(post("/api/v1/mosque-admins")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", VALID_AUDIT_REASON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s",
                                  "permission": "FULL_ACCESS",
                                  "isPrimaryAdmin": false
                                }
                                """.formatted(secondUserId, mosqueId)))
                .andExpect(status().isCreated())
                .andReturn();
        String assignmentId = com.jayway.jsonpath.JsonPath.read(
                assignResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(delete("/api/v1/mosque-admins/" + assignmentId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", VALID_AUDIT_REASON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/api/v1/mosque-admins/" + assignmentId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", VALID_AUDIT_REASON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permission": "VIEW_REPORTS",
                                  "isPrimaryAdmin": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot mutate inactive mosque admin assignment"));

        mockMvc.perform(delete("/api/v1/mosque-admins/" + assignmentId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", VALID_AUDIT_REASON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot mutate inactive mosque admin assignment"));
    }

    @Test
    void superAdmin_createEnrollment_withoutAuditReason_returns400() throws Exception {
        String superAdminEmail = "sa-enroll-create-no@test.darb";
        createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-enroll-create-no@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Enroll Create No");

        registerStudent("student-enroll-create-no@test.darb");
        registerTeacher("teacher-enroll-create-no@test.darb");
        String studentUserId = userRepository.findByEmail("student-enroll-create-no@test.darb").orElseThrow().getId().toString();
        String teacherUserId = userRepository.findByEmail("teacher-enroll-create-no@test.darb").orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);
        String teacherId = createTeacher(adminToken, teacherUserId, mosqueId);
        String circleId = createCircle(adminToken, mosqueId, teacherId, "Circle Enroll Create No");

        String superAdminToken = login(superAdminEmail);
        long auditCountBefore = overrideAuditLogRepository.count();

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "status": "ACTIVE"
                                }
                                """.formatted(studentId, circleId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Audit reason required (min 8 characters) for super admin override"));

        assertThat(overrideAuditLogRepository.count()).isEqualTo(auditCountBefore);
    }

    @Test
    void superAdmin_createEnrollment_withAuditReason_returns201AndPersistsAuditRow() throws Exception {
        String superAdminEmail = "sa-enroll-create-ok@test.darb";
        User superAdmin = createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-enroll-create-ok@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Enroll Create OK");

        registerStudent("student-enroll-create-ok@test.darb");
        registerTeacher("teacher-enroll-create-ok@test.darb");
        String studentUserId = userRepository.findByEmail("student-enroll-create-ok@test.darb").orElseThrow().getId().toString();
        String teacherUserId = userRepository.findByEmail("teacher-enroll-create-ok@test.darb").orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);
        String teacherId = createTeacher(adminToken, teacherUserId, mosqueId);
        String circleId = createCircle(adminToken, mosqueId, teacherId, "Circle Enroll Create OK");

        String superAdminToken = login(superAdminEmail);
        long auditCountBefore = overrideAuditLogRepository.count();

        MvcResult result = mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", VALID_AUDIT_REASON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "status": "ACTIVE"
                                }
                                """.formatted(studentId, circleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String enrollmentId = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.data.id");
        assertThat(overrideAuditLogRepository.count()).isEqualTo(auditCountBefore + 1);
        var auditRow = overrideAuditLogRepository.findAll().stream()
                .filter(row -> row.getResourceId().toString().equals(enrollmentId))
                .findFirst()
                .orElseThrow();
        assertThat(auditRow.getActor().getId()).isEqualTo(superAdmin.getId());
        assertThat(auditRow.getAction()).isEqualTo("ENROLLMENT_CREATE");
        assertThat(auditRow.getReason()).isEqualTo(VALID_AUDIT_REASON);
    }

    @Test
    void superAdmin_createAttendance_withoutAuditReason_returns400() throws Exception {
        String superAdminEmail = "sa-att-create-no@test.darb";
        createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-att-create-no@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Att Create No");

        registerStudent("student-att-create-no@test.darb");
        registerTeacher("teacher-att-create-no@test.darb");
        String studentUserId = userRepository.findByEmail("student-att-create-no@test.darb").orElseThrow().getId().toString();
        String teacherUserId = userRepository.findByEmail("teacher-att-create-no@test.darb").orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);
        String teacherId = createTeacher(adminToken, teacherUserId, mosqueId);
        String circleId = createCircle(adminToken, mosqueId, teacherId, "Circle Att Create No");
        String enrollmentId = createEnrollment(adminToken, studentId, circleId);

        String superAdminToken = login(superAdminEmail);

        mockMvc.perform(post("/api/v1/attendance")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enrollmentId": "%s",
                                  "circleId": "%s",
                                  "sessionDate": "2026-08-26",
                                  "status": "PRESENT",
                                  "scheduledStart": "16:00"
                                }
                                """.formatted(enrollmentId, circleId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Audit reason required (min 8 characters) for super admin override"));
    }

    @Test
    void superAdmin_createAttendance_withAuditReason_returns201AndPersistsAuditRow() throws Exception {
        String superAdminEmail = "sa-att-create-ok@test.darb";
        User superAdmin = createSuperAdmin(superAdminEmail);

        String adminEmail = "admin-att-create-ok@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Att Create OK");

        registerStudent("student-att-create-ok@test.darb");
        registerTeacher("teacher-att-create-ok@test.darb");
        String studentUserId = userRepository.findByEmail("student-att-create-ok@test.darb").orElseThrow().getId().toString();
        String teacherUserId = userRepository.findByEmail("teacher-att-create-ok@test.darb").orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);
        String teacherId = createTeacher(adminToken, teacherUserId, mosqueId);
        String circleId = createCircle(adminToken, mosqueId, teacherId, "Circle Att Create OK");
        String enrollmentId = createEnrollment(adminToken, studentId, circleId);

        String superAdminToken = login(superAdminEmail);
        long auditCountBefore = overrideAuditLogRepository.count();

        MvcResult result = mockMvc.perform(post("/api/v1/attendance")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", VALID_AUDIT_REASON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enrollmentId": "%s",
                                  "circleId": "%s",
                                  "sessionDate": "2026-08-26",
                                  "status": "PRESENT",
                                  "scheduledStart": "16:00"
                                }
                                """.formatted(enrollmentId, circleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String attendanceId = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.data.id");
        assertThat(overrideAuditLogRepository.count()).isEqualTo(auditCountBefore + 1);
        var auditRow = overrideAuditLogRepository.findAll().stream()
                .filter(row -> row.getResourceId().toString().equals(attendanceId))
                .findFirst()
                .orElseThrow();
        assertThat(auditRow.getActor().getId()).isEqualTo(superAdmin.getId());
        assertThat(auditRow.getAction()).isEqualTo("ATTENDANCE_CREATE");
        assertThat(auditRow.getReason()).isEqualTo(VALID_AUDIT_REASON);
    }

    @Test
    void unsupportedMethod_onAttendance_returns405ApiResponse() throws Exception {
        String adminEmail = "admin-method-405@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);

        mockMvc.perform(patch("/api/v1/attendance/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Method not allowed")));
    }

    @Test
    void memorizationCoverage_missingCircleId_returns400() throws Exception {
        String adminEmail = "admin-mem-circleid@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Mem CircleId");

        registerStudent("student-mem-circleid@test.darb");
        String studentUserId = userRepository.findByEmail("student-mem-circleid@test.darb").orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);

        mockMvc.perform(get("/api/v1/memorization/students/" + studentId + "/coverage")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("circleId required"));
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
        return com.darb.support.MembershipFixtures.seatStudent(
                mockMvc, userRepository, studentRepository, adminToken, userId, mosqueId, PASSWORD);
    }

    private String createTeacher(String adminToken, String userId, String mosqueId) throws Exception {
        return com.darb.support.MembershipFixtures.seatTeacher(
                mockMvc, userRepository, teacherRepository, adminToken, userId, mosqueId, PASSWORD);
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
                                  "status": "ACTIVE",
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

        String requestId = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
        User parent = userRepository.findById(java.util.UUID.fromString(parentUserId)).orElseThrow();
        MembershipFixtures.acceptParentInvite(mockMvc, parent.getEmail(), PASSWORD, requestId);
        return parentStudentRepository.findByParentId(parent.getId()).stream()
                .filter(link -> link.getStudent().getId().toString().equals(studentId))
                .findFirst()
                .orElseThrow()
                .getId()
                .toString();
    }
}
