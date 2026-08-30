package com.darb.controllers.v1;

import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.ParentStudentRepository;
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
class FindByIdAccessIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentStudentRepository parentStudentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    void mosqueAdmin_cannotGetAttendanceFromOtherMosque() throws Exception {
        String adminAEmail = "admin-a-attendance-findbyid@test.darb";
        String adminBEmail = "admin-b-attendance-findbyid@test.darb";
        registerMosqueAdmin(adminAEmail);
        registerMosqueAdmin(adminBEmail);

        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);

        onboardMosque(tokenA, "Mosque Alpha Attendance");
        String mosqueBId = onboardMosque(tokenB, "Mosque Beta Attendance");

        registerStudent("student-b-attendance-findbyid@test.darb");
        String studentUserId = userRepository.findByEmail("student-b-attendance-findbyid@test.darb")
                .orElseThrow().getId().toString();
        String studentBId = createStudent(tokenB, studentUserId, mosqueBId);

        registerTeacher("teacher-b-attendance-findbyid@test.darb");
        String teacherUserId = userRepository.findByEmail("teacher-b-attendance-findbyid@test.darb")
                .orElseThrow().getId().toString();
        String teacherBId = createTeacher(tokenB, teacherUserId, mosqueBId);

        String circleBId = createCircle(tokenB, mosqueBId, teacherBId, "Circle Beta Attendance");
        String enrollmentBId = createEnrollment(tokenB, studentBId, circleBId);
        String attendanceBId = createAttendance(tokenB, enrollmentBId, circleBId);

        mockMvc.perform(get("/api/v1/attendance/" + attendanceBId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void mosqueAdmin_cannotGetEnrollmentFromOtherMosque() throws Exception {
        String adminAEmail = "admin-a-enrollment-findbyid@test.darb";
        String adminBEmail = "admin-b-enrollment-findbyid@test.darb";
        registerMosqueAdmin(adminAEmail);
        registerMosqueAdmin(adminBEmail);

        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);

        onboardMosque(tokenA, "Mosque Alpha Enrollment");
        String mosqueBId = onboardMosque(tokenB, "Mosque Beta Enrollment");

        registerStudent("student-b-enrollment-findbyid@test.darb");
        String studentUserId = userRepository.findByEmail("student-b-enrollment-findbyid@test.darb")
                .orElseThrow().getId().toString();
        String studentBId = createStudent(tokenB, studentUserId, mosqueBId);

        registerTeacher("teacher-b-enrollment-findbyid@test.darb");
        String teacherUserId = userRepository.findByEmail("teacher-b-enrollment-findbyid@test.darb")
                .orElseThrow().getId().toString();
        String teacherBId = createTeacher(tokenB, teacherUserId, mosqueBId);

        String circleBId = createCircle(tokenB, mosqueBId, teacherBId, "Circle Beta Enrollment");
        String enrollmentBId = createEnrollment(tokenB, studentBId, circleBId);

        mockMvc.perform(get("/api/v1/enrollments/" + enrollmentBId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void parent_cannotGetForeignParentStudentLink() throws Exception {
        String adminEmail = "admin-parent-link-findbyid@test.darb";
        String parent1Email = "parent1-link-findbyid@test.darb";
        String parent2Email = "parent2-link-findbyid@test.darb";
        registerMosqueAdmin(adminEmail);
        registerParent(parent1Email);
        registerParent(parent2Email);

        String adminToken = login(adminEmail);
        String parent1Token = login(parent1Email);
        String parent2Token = login(parent2Email);

        String mosqueId = onboardMosque(adminToken, "Mosque Parent Link");

        registerStudent("student-parent-link-findbyid@test.darb");
        String studentUserId = userRepository.findByEmail("student-parent-link-findbyid@test.darb")
                .orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);

        String parent1UserId = userRepository.findByEmail(parent1Email).orElseThrow().getId().toString();
        String linkId = createParentStudentLink(adminToken, parent1UserId, studentId);

        mockMvc.perform(get("/api/v1/parent-students/" + linkId)
                        .header("Authorization", "Bearer " + parent2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/parent-students/" + linkId)
                        .header("Authorization", "Bearer " + parent1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void parent_canGetOwnParentStudentLink() throws Exception {
        String adminEmail = "admin-own-link-findbyid@test.darb";
        String parentEmail = "parent-own-link-findbyid@test.darb";
        registerMosqueAdmin(adminEmail);
        registerParent(parentEmail);

        String adminToken = login(adminEmail);
        String parentToken = login(parentEmail);

        String mosqueId = onboardMosque(adminToken, "Mosque Own Link");

        registerStudent("student-own-link-findbyid@test.darb");
        String studentUserId = userRepository.findByEmail("student-own-link-findbyid@test.darb")
                .orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);

        String parentUserId = userRepository.findByEmail(parentEmail).orElseThrow().getId().toString();
        String linkId = createParentStudentLink(adminToken, parentUserId, studentId);

        mockMvc.perform(get("/api/v1/parent-students/" + linkId)
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(linkId))
                .andExpect(jsonPath("$.data.parentName").isNotEmpty())
                .andExpect(jsonPath("$.data.studentName").isNotEmpty());
    }

    @Test
    void parent_canGetMyChildren_exposesStudentFullName() throws Exception {
        String adminEmail = "admin-my-children-findbyid@test.darb";
        String parentEmail = "parent-my-children-findbyid@test.darb";
        registerMosqueAdmin(adminEmail);
        registerParent(parentEmail);

        String adminToken = login(adminEmail);
        String parentToken = login(parentEmail);

        String mosqueId = onboardMosque(adminToken, "Mosque My Children");

        registerStudent("student-my-children-findbyid@test.darb");
        String studentUserId = userRepository.findByEmail("student-my-children-findbyid@test.darb")
                .orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);

        String parentUserId = userRepository.findByEmail(parentEmail).orElseThrow().getId().toString();
        createParentStudentLink(adminToken, parentUserId, studentId);

        mockMvc.perform(get("/api/v1/parent-students/my-children")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(studentId))
                .andExpect(jsonPath("$.data[0].fullName").value("Student User"));
    }

    @Test
    void mosqueAdmin_cannotGetGoalFromOtherMosque() throws Exception {
        String adminAEmail = "admin-a-goal-findbyid@test.darb";
        String adminBEmail = "admin-b-goal-findbyid@test.darb";
        registerMosqueAdmin(adminAEmail);
        registerMosqueAdmin(adminBEmail);

        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);

        onboardMosque(tokenA, "Mosque Alpha Goal");
        String mosqueBId = onboardMosque(tokenB, "Mosque Beta Goal");

        registerStudent("student-b-goal-findbyid@test.darb");
        String studentUserId = userRepository.findByEmail("student-b-goal-findbyid@test.darb")
                .orElseThrow().getId().toString();
        String studentBId = createStudent(tokenB, studentUserId, mosqueBId);

        registerTeacher("teacher-b-goal-findbyid@test.darb");
        String teacherUserId = userRepository.findByEmail("teacher-b-goal-findbyid@test.darb")
                .orElseThrow().getId().toString();
        String teacherBId = createTeacher(tokenB, teacherUserId, mosqueBId);

        String circleBId = createCircle(tokenB, mosqueBId, teacherBId, "Circle Beta Goal");
        String goalBId = createGoal(tokenB, studentBId, circleBId);

        mockMvc.perform(get("/api/v1/goals/" + goalBId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void mosqueAdmin_cannotGetAchievementFromOtherMosque() throws Exception {
        String adminAEmail = "admin-a-achievement-findbyid@test.darb";
        String adminBEmail = "admin-b-achievement-findbyid@test.darb";
        registerMosqueAdmin(adminAEmail);
        registerMosqueAdmin(adminBEmail);

        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);

        onboardMosque(tokenA, "Mosque Alpha Achievement");
        String mosqueBId = onboardMosque(tokenB, "Mosque Beta Achievement");

        registerStudent("student-b-achievement-findbyid@test.darb");
        String studentUserId = userRepository.findByEmail("student-b-achievement-findbyid@test.darb")
                .orElseThrow().getId().toString();
        String studentBId = createStudent(tokenB, studentUserId, mosqueBId);

        String achievementBId = createAchievement(tokenB, studentBId, mosqueBId);

        mockMvc.perform(get("/api/v1/achievements/" + achievementBId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void mosqueAdmin_cannotGetReportFromOtherMosque() throws Exception {
        String adminAEmail = "admin-a-report-findbyid@test.darb";
        String adminBEmail = "admin-b-report-findbyid@test.darb";
        registerMosqueAdmin(adminAEmail);
        registerMosqueAdmin(adminBEmail);

        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);

        onboardMosque(tokenA, "Mosque Alpha Report");
        String mosqueBId = onboardMosque(tokenB, "Mosque Beta Report");

        String reportBId = createReport(tokenB, mosqueBId);

        mockMvc.perform(get("/api/v1/reports/" + reportBId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void mosqueAdmin_cannotGetUserFromOtherMosque() throws Exception {
        String adminAEmail = "admin-a-user-findbyid@test.darb";
        String adminBEmail = "admin-b-user-findbyid@test.darb";
        registerMosqueAdmin(adminAEmail);
        registerMosqueAdmin(adminBEmail);

        String tokenA = login(adminAEmail);
        String tokenB = login(adminBEmail);

        onboardMosque(tokenA, "Mosque Alpha User");
        String mosqueBId = onboardMosque(tokenB, "Mosque Beta User");

        registerStudent("student-b-user-findbyid@test.darb");
        String studentUserId = userRepository.findByEmail("student-b-user-findbyid@test.darb")
                .orElseThrow().getId().toString();
        createStudent(tokenB, studentUserId, mosqueBId);

        mockMvc.perform(get("/api/v1/users/" + studentUserId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void mosqueAdmin_canCreatePayment_exposesStudentName() throws Exception {
        String adminEmail = "admin-payment-create@test.darb";
        registerMosqueAdmin(adminEmail);
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Mosque Payment Create");

        registerStudent("student-payment-create@test.darb");
        registerTeacher("teacher-payment-create@test.darb");
        String studentUserId = userRepository.findByEmail("student-payment-create@test.darb")
                .orElseThrow().getId().toString();
        String teacherUserId = userRepository.findByEmail("teacher-payment-create@test.darb")
                .orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, mosqueId);
        String teacherId = createTeacher(adminToken, teacherUserId, mosqueId);
        String circleId = createCircle(adminToken, mosqueId, teacherId, "Circle Payment Create");

        createPayment(adminToken, studentId, circleId, mosqueId);
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

    private String createAttendance(String adminToken, String enrollmentId, String circleId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/attendance")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enrollmentId": "%s",
                                  "circleId": "%s",
                                  "sessionDate": "2026-07-25",
                                  "status": "PRESENT",
                                  "scheduledStart": "16:00"
                                }
                                """.formatted(enrollmentId, circleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentName").value("Student User"))
                .andExpect(jsonPath("$.data.circleName").isNotEmpty())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }

    private String createPayment(String adminToken, String studentId, String circleId, String mosqueId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "mosqueId": "%s",
                                  "amount": 150.00,
                                  "status": "PENDING",
                                  "method": "CASH",
                                  "cycle": "MONTHLY",
                                  "dueDate": "2026-08-01"
                                }
                                """.formatted(studentId, circleId, mosqueId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentName").value("Student User"))
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
        var parent = userRepository.findById(java.util.UUID.fromString(parentUserId)).orElseThrow();
        com.darb.support.MembershipFixtures.acceptParentInvite(mockMvc, parent.getEmail(), PASSWORD, requestId);
        return parentStudentRepository.findByParentId(parent.getId()).stream()
                .filter(link -> link.getStudent().getId().toString().equals(studentId))
                .findFirst()
                .orElseThrow()
                .getId()
                .toString();
    }

    private String createGoal(String adminToken, String studentId, String circleId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "circleId": "%s",
                                  "title": "Memorize Surah Al-Mulk",
                                  "status": "IN_PROGRESS"
                                }
                                """.formatted(studentId, circleId)))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }

    private String createAchievement(String adminToken, String studentId, String mosqueId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/achievements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "mosqueId": "%s",
                                  "type": "MEMORIZATION",
                                  "title": "Completed Juz Amma"
                                }
                                """.formatted(studentId, mosqueId)))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }

    private String createReport(String adminToken, String mosqueId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s",
                                  "generatedBy": "00000000-0000-0000-0000-000000000001",
                                  "type": "ATTENDANCE_SUMMARY",
                                  "title": "April 2026 Attendance Summary",
                                  "filters": "{\\"month\\": \\"2026-04\\"}"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.id");
    }
}
