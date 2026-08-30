package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RemovedMemberAccessIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";
    private static final String AUDIT_REASON = "Break-glass member removal";

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
    void removedMosqueAdmin_losesTenantAccess_keepsAuthoredHistory() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String primaryEmail = "primary-ma-" + suffix + "@test.darb";
        String secondAdminEmail = "second-ma-" + suffix + "@test.darb";
        String student1Email = "student1-" + suffix + "@test.darb";
        String student2Email = "student2-" + suffix + "@test.darb";
        String superAdminEmail = "sa-ma-" + suffix + "@test.darb";

        registerMosqueAdmin(primaryEmail);
        registerMosqueAdmin(secondAdminEmail);
        createSuperAdmin(superAdminEmail);

        String primaryToken = login(primaryEmail);
        String mosqueId = onboardMosque(primaryToken, "Removed MA Mosque " + suffix);

        registerStudent(student1Email);
        registerStudent(student2Email);
        String student1UserId = userRepository.findByEmail(student1Email).orElseThrow().getId().toString();
        String student2UserId = userRepository.findByEmail(student2Email).orElseThrow().getId().toString();
        String student1Id = seatStudent(primaryToken, student1UserId, mosqueId);
        String student2Id = seatStudent(primaryToken, student2UserId, mosqueId);

        String primaryAchievementId = createAchievement(primaryToken, student1Id, mosqueId, "Primary Admin Award");

        String secondAdminUserId = userRepository.findByEmail(secondAdminEmail).orElseThrow().getId().toString();
        String superAdminToken = login(superAdminEmail);
        String secondAdminAssignmentId = assignMosqueAdmin(superAdminToken, secondAdminUserId, mosqueId);

        String secondAdminToken = login(secondAdminEmail);
        String secondAdminAchievementId = createAchievement(
                secondAdminToken, student2Id, mosqueId, "Second Admin Award");

        deleteMosqueAdmin(superAdminToken, secondAdminAssignmentId);
        secondAdminToken = login(secondAdminEmail);

        mockMvc.perform(get("/api/v1/achievements/mosque/" + mosqueId)
                        .header("Authorization", "Bearer " + secondAdminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/achievements/student/" + student1Id)
                        .header("Authorization", "Bearer " + secondAdminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + secondAdminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/achievements/" + secondAdminAchievementId)
                        .header("Authorization", "Bearer " + secondAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(secondAdminAchievementId));

        mockMvc.perform(post("/api/v1/achievements")
                        .header("Authorization", "Bearer " + secondAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "mosqueId": "%s",
                                  "type": "MEMORIZATION",
                                  "title": "Blocked Award"
                                }
                                """.formatted(student2Id, mosqueId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/achievements/mosque/" + mosqueId)
                        .header("Authorization", "Bearer " + primaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + primaryAchievementId + "')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + secondAdminAchievementId + "')]").exists());
    }

    @Test
    void removedStudent_keepsOwnHistory_losesTenantAndMutations() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String adminEmail = "ma-student-" + suffix + "@test.darb";
        String studentEmail = "withdrawn-student-" + suffix + "@test.darb";
        String otherStudentEmail = "other-student-" + suffix + "@test.darb";

        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Removed Student Mosque " + suffix);

        registerStudent(studentEmail);
        registerStudent(otherStudentEmail);
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String otherStudentUserId = userRepository.findByEmail(otherStudentEmail).orElseThrow().getId().toString();
        String studentId = seatStudent(adminToken, studentUserId, mosqueId);
        String otherStudentId = seatStudent(adminToken, otherStudentUserId, mosqueId);

        createAchievement(adminToken, studentId, mosqueId, "Withdrawn Student Award");

        mockMvc.perform(delete("/api/v1/students/" + studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String studentToken = login(studentEmail);

        mockMvc.perform(get("/api/v1/achievements/student/" + studentId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Withdrawn Student Award"));

        mockMvc.perform(get("/api/v1/achievements/student/" + otherStudentId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/v1/achievements")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "mosqueId": "%s",
                                  "type": "MEMORIZATION",
                                  "title": "Student Blocked Award"
                                }
                                """.formatted(studentId, mosqueId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/students/" + studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        mockMvc.perform(get("/api/v1/achievements/student/" + studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void removedTeacher_keepsAuthoredHistory_losesTenantAndMutations() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String adminEmail = "ma-teacher-" + suffix + "@test.darb";
        String teacherEmail = "inactive-teacher-" + suffix + "@test.darb";
        String studentEmail = "teacher-student-" + suffix + "@test.darb";

        registerMosqueAdmin(adminEmail);
        registerTeacher(teacherEmail);
        registerStudent(studentEmail);

        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Removed Teacher Mosque " + suffix);

        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String teacherUserId = userRepository.findByEmail(teacherEmail).orElseThrow().getId().toString();
        String studentId = seatStudent(adminToken, studentUserId, mosqueId);
        String teacherId = seatTeacher(adminToken, teacherUserId, mosqueId);

        String teacherToken = login(teacherEmail);
        String teacherAchievementId = createAchievement(
                teacherToken, studentId, mosqueId, "Teacher Award");

        mockMvc.perform(delete("/api/v1/teachers/" + teacherId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        teacherToken = login(teacherEmail);

        mockMvc.perform(get("/api/v1/achievements/student/" + studentId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/achievements/" + teacherAchievementId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(teacherAchievementId));

        mockMvc.perform(post("/api/v1/achievements")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "mosqueId": "%s",
                                  "type": "MEMORIZATION",
                                  "title": "Teacher Blocked Award"
                                }
                                """.formatted(studentId, mosqueId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + teacherId + "')].isActive").value(false));

        mockMvc.perform(get("/api/v1/achievements/" + teacherAchievementId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(teacherAchievementId));
    }

    @Test
    void activeAdmin_seesFormerMembersInLists() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String adminEmail = "ma-former-" + suffix + "@test.darb";
        String studentEmail = "former-student-" + suffix + "@test.darb";
        String teacherEmail = "former-teacher-" + suffix + "@test.darb";

        registerMosqueAdmin(adminEmail);
        registerStudent(studentEmail);
        registerTeacher(teacherEmail);

        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Former Members Mosque " + suffix);

        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String teacherUserId = userRepository.findByEmail(teacherEmail).orElseThrow().getId().toString();
        String studentId = seatStudent(adminToken, studentUserId, mosqueId);
        String teacherId = seatTeacher(adminToken, teacherUserId, mosqueId);

        mockMvc.perform(delete("/api/v1/students/" + studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/teachers/" + teacherId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + studentId + "')].status").value("WITHDRAWN"));

        mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + teacherId + "')].isActive").value(false));
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

    private String seatStudent(String adminToken, String userId, String mosqueId) throws Exception {
        return MembershipFixtures.seatStudent(
                mockMvc, userRepository, studentRepository, adminToken, userId, mosqueId, PASSWORD);
    }

    private String seatTeacher(String adminToken, String userId, String mosqueId) throws Exception {
        return MembershipFixtures.seatTeacher(
                mockMvc, userRepository, teacherRepository, adminToken, userId, mosqueId, PASSWORD);
    }

    private String createAchievement(String token, String studentId, String mosqueId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/achievements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "mosqueId": "%s",
                                  "type": "MEMORIZATION",
                                  "title": "%s"
                                }
                                """.formatted(studentId, mosqueId, title)))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }

    private String assignMosqueAdmin(String superAdminToken, String userId, String mosqueId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mosque-admins")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", AUDIT_REASON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s",
                                  "permission": "FULL_ACCESS",
                                  "isPrimaryAdmin": false
                                }
                                """.formatted(userId, mosqueId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }

    private void deleteMosqueAdmin(String superAdminToken, String assignmentId) throws Exception {
        mockMvc.perform(delete("/api/v1/mosque-admins/" + assignmentId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", AUDIT_REASON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
