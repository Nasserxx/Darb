package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.StudentRepository;
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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAddressIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void putMe_addressRoundTripsOnGetMe() throws Exception {
        String tokenSuffix = UUID.randomUUID().toString();
        String email = "me-address-" + tokenSuffix + "@test.darb";
        registerRole(email, "student");
        String token = login(email);

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "city": "Riyadh",
                                  "addressCountry": "SA",
                                  "addressStreet": "King Fahd Road",
                                  "addressHouseNumber": "12",
                                  "addressPostalCode": "12211",
                                  "addressState": "Riyadh Province"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.city").value("Riyadh"))
                .andExpect(jsonPath("$.data.addressCountry").value("SA"))
                .andExpect(jsonPath("$.data.addressStreet").value("King Fahd Road"))
                .andExpect(jsonPath("$.data.addressHouseNumber").value("12"))
                .andExpect(jsonPath("$.data.addressPostalCode").value("12211"))
                .andExpect(jsonPath("$.data.addressState").value("Riyadh Province"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.city").value("Riyadh"))
                .andExpect(jsonPath("$.data.addressCountry").value("SA"))
                .andExpect(jsonPath("$.data.addressStreet").value("King Fahd Road"))
                .andExpect(jsonPath("$.data.addressHouseNumber").value("12"))
                .andExpect(jsonPath("$.data.addressPostalCode").value("12211"))
                .andExpect(jsonPath("$.data.addressState").value("Riyadh Province"));
    }

    @Test
    void searchUsers_asSuperAdmin_omitsAddressStreet() throws Exception {
        String tokenSuffix = UUID.randomUUID().toString();
        String uniqueName = "SearchAddrUser" + tokenSuffix.substring(0, 8);
        String email = "search-addr-" + tokenSuffix + "@test.darb";
        registerRoleWithName(email, "student", uniqueName);
        String userToken = login(email);

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "city": "Jeddah",
                                  "addressCountry": "SA",
                                  "addressStreet": "Secret Street",
                                  "addressState": "Makkah Province"
                                }
                                """))
                .andExpect(status().isOk());

        String superToken = createSuperAdmin("super-addr-search-" + tokenSuffix + "@test.darb");

        mockMvc.perform(get("/api/v1/users/search")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", uniqueName)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value(email))
                .andExpect(jsonPath("$.data.content[0].addressStreet").value(nullValue()));
    }

    @Test
    void mosqueAdmin_putLinkedStudentUser_succeeds_unlinkedForbidden() throws Exception {
        String tokenSuffix = UUID.randomUUID().toString();

        String adminEmail = "admin-addr-" + tokenSuffix + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        String mosqueId = onboardMosque(adminToken, "Address Mosque " + tokenSuffix);

        String linkedEmail = "linked-student-" + tokenSuffix + "@test.darb";
        registerRole(linkedEmail, "student");
        String linkedUserId = userRepository.findByEmail(linkedEmail).orElseThrow().getId().toString();
        createStudent(adminToken, linkedUserId, mosqueId);

        String unlinkedEmail = "unlinked-student-" + tokenSuffix + "@test.darb";
        registerRole(unlinkedEmail, "student");
        String unlinkedUserId = userRepository.findByEmail(unlinkedEmail).orElseThrow().getId().toString();

        mockMvc.perform(put("/api/v1/users/" + linkedUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "city": "Dammam",
                                  "addressCountry": "SA",
                                  "addressStreet": "Corniche",
                                  "addressState": "Eastern Province"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.city").value("Dammam"))
                .andExpect(jsonPath("$.data.addressStreet").value("Corniche"));

        mockMvc.perform(put("/api/v1/users/" + unlinkedUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "city": "HackerCity",
                                  "addressCountry": "SA",
                                  "addressStreet": "Should Fail",
                                  "addressState": "Nowhere"
                                }
                                """))
                .andExpect(status().isForbidden());
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

    private void createStudent(String adminToken, String userId, String mosqueId) throws Exception {
        com.darb.support.MembershipFixtures.seatStudent(
                mockMvc, userRepository, studentRepository, adminToken, userId, mosqueId, PASSWORD);
    }
}
