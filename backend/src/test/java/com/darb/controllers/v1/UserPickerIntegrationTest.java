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

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserPickerIntegrationTest extends PostgresIntegrationTestBase {

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
    void mosqueAdmin_pickerFindsUserOutsideMosque_searchDoesNot() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String adminEmail = "admin-picker-" + suffix + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        onboardMosque(adminToken, "Picker Mosque " + suffix.substring(0, 8));

        String outsiderName = "ZaydPicker" + suffix.substring(0, 8);
        String outsiderEmail = "outsider-picker-" + suffix + "@test.darb";
        registerNamed(outsiderEmail, "student", outsiderName);
        String outsiderToken = login(outsiderEmail);
        putAddress(outsiderToken, "Munich", "DE", "Bavaria", "Leopoldstrasse", "2010-05-20");

        mockMvc.perform(get("/api/v1/users/picker")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("q", outsiderName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].fullName").value(outsiderName))
                .andExpect(jsonPath("$.data.content[0].city").value("Munich"))
                .andExpect(jsonPath("$.data.content[0].addressCountry").value("DE"))
                .andExpect(jsonPath("$.data.content[0].addressState").value("Bavaria"))
                .andExpect(jsonPath("$.data.content[0].dateOfBirth").value("2010-05-20"))
                .andExpect(jsonPath("$.data.content[0].addressStreet").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].email").doesNotExist());

        mockMvc.perform(get("/api/v1/users/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("q", outsiderName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void picker_filtersByUserAddressAndDateOfBirth() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String superToken = createSuperAdmin("super-picker-filter-" + suffix + "@test.darb");

        String matchName = "FatimaPicker" + suffix.substring(0, 8);
        String matchEmail = "match-picker-" + suffix + "@test.darb";
        String missEmail = "miss-picker-" + suffix + "@test.darb";
        registerNamed(matchEmail, "parent", matchName);
        registerNamed(missEmail, "parent", matchName);
        putAddress(login(matchEmail), "Munich", "DE", "Bavaria", "Secret Ave", "2010-05-20");
        putAddress(login(missEmail), "Berlin", "DE", "Berlin", "Other St", "2011-01-01");

        mockMvc.perform(get("/api/v1/users/picker")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", matchName)
                        .param("country", "de")
                        .param("state", "Bavaria")
                        .param("city", "Munich")
                        .param("dateOfBirth", "2010-05-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].city").value("Munich"));
    }

    @Test
    void picker_requiresACriterion() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String superToken = createSuperAdmin("super-picker-empty-" + suffix + "@test.darb");

        mockMvc.perform(get("/api/v1/users/picker")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void picker_paginatesAndCapsSize() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String prefix = "PagePicker" + suffix.substring(0, 6);
        String superToken = createSuperAdmin("super-picker-page-" + suffix + "@test.darb");

        for (int i = 0; i < 21; i++) {
            registerNamed("page-picker-" + i + "-" + suffix + "@test.darb", "parent", prefix + i);
        }

        mockMvc.perform(get("/api/v1/users/picker")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", prefix)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(21))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        mockMvc.perform(get("/api/v1/users/picker")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", prefix)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void picker_statesAndCitiesComeFromUserAddress() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String superToken = createSuperAdmin("super-picker-geo-" + suffix + "@test.darb");
        String email = "geo-picker-" + suffix + "@test.darb";
        registerRole(email, "parent");
        putAddress(login(email), "Jeddah", "SA", "Makkah Province", "Corniche", "1990-02-02");

        mockMvc.perform(get("/api/v1/users/picker/states")
                        .header("Authorization", "Bearer " + superToken)
                        .param("country", "sa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasItem("Makkah Province")));

        mockMvc.perform(get("/api/v1/users/picker/cities")
                        .header("Authorization", "Bearer " + superToken)
                        .param("country", "SA")
                        .param("state", "Makkah Province"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasItem("Jeddah")));

        mockMvc.perform(get("/api/v1/users/picker/cities")
                        .header("Authorization", "Bearer " + superToken)
                        .param("country", "SA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasItem("Jeddah")));
    }

    @Test
    void picker_statesAndCitiesEmptyWhenCountryHasNoUsers() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String superToken = createSuperAdmin("super-picker-geo-empty-" + suffix + "@test.darb");
        String email = "geo-picker-sa-only-" + suffix + "@test.darb";
        registerRole(email, "parent");
        putAddress(login(email), "Jeddah", "SA", "Makkah Province", "Corniche", "1990-02-02");

        mockMvc.perform(get("/api/v1/users/picker/states")
                        .header("Authorization", "Bearer " + superToken)
                        .param("country", "IS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/v1/users/picker/cities")
                        .header("Authorization", "Bearer " + superToken)
                        .param("country", "IS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void student_canPick_parentCannot() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String studentEmail = "student-picker-" + suffix + "@test.darb";
        String parentEmail = "parent-picker-role-" + suffix + "@test.darb";
        registerRole(studentEmail, "student");
        registerRole(parentEmail, "parent");

        mockMvc.perform(get("/api/v1/users/picker")
                        .header("Authorization", "Bearer " + login(studentEmail))
                        .param("q", "student-picker"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/picker")
                        .header("Authorization", "Bearer " + login(parentEmail))
                        .param("q", "student-picker"))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_createsParentLinkForSelfOnly() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String adminEmail = "admin-ps-" + suffix + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "PS Mosque " + suffix.substring(0, 8));

        String studentEmail = "self-student-" + suffix + "@test.darb";
        String otherStudentEmail = "other-student-" + suffix + "@test.darb";
        String parentEmail = "link-parent-" + suffix + "@test.darb";
        registerRole(studentEmail, "student");
        registerRole(otherStudentEmail, "student");
        registerRole(parentEmail, "parent");
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String otherUserId = userRepository.findByEmail(otherStudentEmail).orElseThrow().getId().toString();
        String parentUserId = userRepository.findByEmail(parentEmail).orElseThrow().getId().toString();
        String studentToken = login(studentEmail);
        mockMvc.perform(post("/api/v1/students/onboard")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated());
        String studentId = studentRepository.findByUserId(java.util.UUID.fromString(studentUserId))
                .getFirst().getId().toString();
        String otherStudentId = createStudent(adminToken, otherUserId, mosqueId);

        MvcResult inviteSelf = mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "FATHER"
                                }
                                """.formatted(parentUserId, studentId)))
                .andExpect(status().isCreated())
                .andReturn();
        String selfRequestId = com.jayway.jsonpath.JsonPath.read(
                inviteSelf.getResponse().getContentAsString(), "$.data.id");
        com.darb.support.MembershipFixtures.acceptParentInvite(mockMvc, parentEmail, PASSWORD, selfRequestId);

        mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "MOTHER"
                                }
                                """.formatted(parentUserId, otherStudentId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(studentId));
    }

    @Test
    void picker_occupancyExcludesSeatedAndPendingStudents() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String adminEmail = "admin-occ-" + suffix + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Occ Mosque " + suffix.substring(0, 8));

        String seatedName = "SeatedPick" + suffix.substring(0, 8);
        String pendingName = "PendingPick" + suffix.substring(0, 8);
        String freeName = "FreePick" + suffix.substring(0, 8);
        String seatedEmail = "seated-" + suffix + "@test.darb";
        String pendingEmail = "pending-" + suffix + "@test.darb";
        String freeEmail = "free-" + suffix + "@test.darb";
        registerNamed(seatedEmail, "student", seatedName);
        registerNamed(pendingEmail, "student", pendingName);
        registerNamed(freeEmail, "student", freeName);
        String seatedToken = login(seatedEmail);
        putAddress(seatedToken, "Riyadh", "SA", "Riyadh Province", "A St", "2010-01-01");
        putAddress(login(pendingEmail), "Riyadh", "SA", "Riyadh Province", "B St", "2010-01-01");
        putAddress(login(freeEmail), "Riyadh", "SA", "Riyadh Province", "C St", "2010-01-01");

        mockMvc.perform(post("/api/v1/students/onboard")
                        .header("Authorization", "Bearer " + seatedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "mosqueId": "%s"
                                }
                                """.formatted(
                                userRepository.findByEmail(pendingEmail).orElseThrow().getId(), mosqueId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users/picker")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("q", "Pick" + suffix.substring(0, 8))
                        .param("mosqueId", mosqueId)
                        .param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].fullName").value(freeName));

        mockMvc.perform(get("/api/v1/users/picker")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("q", "Pick")
                        .param("mosqueId", mosqueId))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/users/picker")
                        .header("Authorization", "Bearer " + seatedToken)
                        .param("q", freeName)
                        .param("mosqueId", mosqueId)
                        .param("role", "STUDENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacher_canCreateParentLinkInOwnMosque() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String adminEmail = "admin-teach-ps-" + suffix + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Teach PS " + suffix.substring(0, 8));

        String teacherEmail = "teacher-ps-" + suffix + "@test.darb";
        String studentEmail = "st-teach-ps-" + suffix + "@test.darb";
        String parentEmail = "par-teach-ps-" + suffix + "@test.darb";
        registerRole(teacherEmail, "teacher");
        registerRole(studentEmail, "student");
        registerRole(parentEmail, "parent");
        String teacherUserId = userRepository.findByEmail(teacherEmail).orElseThrow().getId().toString();
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String parentUserId = userRepository.findByEmail(parentEmail).orElseThrow().getId().toString();
        String teacherToken = login(teacherEmail);
        mockMvc.perform(post("/api/v1/teachers/onboard")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated());
        String studentId = createStudent(adminToken, studentUserId, mosqueId);

        mockMvc.perform(post("/api/v1/parent-students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentUserId": "%s",
                                  "studentId": "%s",
                                  "relationship": "GUARDIAN"
                                }
                                """.formatted(parentUserId, studentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.relationship").value("GUARDIAN"));
    }

    private String createSuperAdmin(String email) {
        User user = User.builder()
                .fullName("Super Admin")
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build();
        userRepository.save(user);
        try {
            return login(email);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void registerRole(String email, String role) throws Exception {
        registerNamed(email, role, "Test User");
    }

    private void registerNamed(String email, String role, String fullName) throws Exception {
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

    private void putAddress(
            String token, String city, String country, String state, String street, String dob)
            throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "city": "%s",
                                  "addressCountry": "%s",
                                  "addressState": "%s",
                                  "addressStreet": "%s",
                                  "dateOfBirth": "%s"
                                }
                                """.formatted(city, country, state, street, dob)))
                .andExpect(status().isOk());
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
                result.getResponse().getContentAsString(), "$.data.accessToken");
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
                result.getResponse().getContentAsString(), "$.data.mosque.id");
    }

    private String createStudent(String adminToken, String userId, String mosqueId) throws Exception {
        return com.darb.support.MembershipFixtures.seatStudent(
                mockMvc, userRepository, studentRepository, adminToken, userId, mosqueId, PASSWORD);
    }

    private void createTeacher(String adminToken, String userId, String mosqueId) throws Exception {
        com.darb.support.MembershipFixtures.seatTeacher(
                mockMvc, userRepository, teacherRepository, adminToken, userId, mosqueId, PASSWORD);
    }
}
