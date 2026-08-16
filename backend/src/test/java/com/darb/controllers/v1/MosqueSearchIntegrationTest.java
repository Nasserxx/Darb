package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MosqueSearchIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void search_byCountry_isCaseInsensitive() throws Exception {
        String teacherToken = createTeacher();
        String token = UUID.randomUUID().toString();
        String saMosque = "Country Case Mosque " + token;
        seedMosque(saMosque, "Riyadh", "Riyadh Province", "SA");

        // a lowercase parameter matches an uppercase stored code
        performSearch(teacherToken, "q", token, "country", "sa")
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(saMosque));

        // an uppercase parameter matches too
        performSearch(teacherToken, "q", token, "country", "SA")
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(saMosque));

        // a different country excludes the mosque (scoped by the unique name so the
        // assertion stays deterministic on the shared Testcontainers database)
        performSearch(teacherToken, "q", token, "country", "DE")
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void search_byCity_isContainsMatch() throws Exception {
        String teacherToken = createTeacher();
        String token = UUID.randomUUID().toString();
        String riyadhMosque = "City Contains Mosque " + token;
        seedMosque(riyadhMosque, "Riyadh", "Riyadh Province", null);

        // a partial city value matches (contains semantics)
        performSearch(teacherToken, "q", token, "city", "Riyad")
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(riyadhMosque));

        // a lowercase city value matches an uppercase stored city (case-insensitive contains)
        performSearch(teacherToken, "q", token, "city", "riyadh")
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(riyadhMosque));
    }

    @Test
    void search_combinesFiltersWithAnd() throws Exception {
        String teacherToken = createTeacher();
        String token = UUID.randomUUID().toString();
        String dammam = "Search And One " + token;
        String riyadhDe = "Search And Two " + token;
        String jeddah = "Search And Three " + token;
        seedMosque(dammam, "Dammam", "Eastern Province", "SA");
        seedMosque(riyadhDe, "Riyadh", "Riyadh Province", "DE");
        seedMosque(jeddah, "Jeddah", "Makkah Province", "SA");

        // q matches all three; country + city narrow to the single intersection row
        performSearch(teacherToken, "q", token, "country", "SA", "city", "Dammam")
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(dammam));

        // a different combination narrows to a different single row
        performSearch(teacherToken, "q", token, "country", "DE", "city", "Riyadh")
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(riyadhDe));
    }

    @Test
    void search_wildcardCharsAreLiteral() throws Exception {
        String teacherToken = createTeacher();
        String token = UUID.randomUUID().toString();
        String percentName = "Search Percent " + token + "%";
        String plainName = "Search Percent " + token + " Plain";
        // a unique country scopes the bare '%' query deterministically on the shared database
        seedMosque(percentName, "Riyadh", "Riyadh Province", "AF");
        seedMosque(plainName, "Riyadh", "Riyadh Province", "AF");

        // a literal '%' in the query must not act as a LIKE wildcard:
        // without escaping it would match the plain-name row as well
        performSearch(teacherToken, "q", "%", "country", "AF")
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(percentName));

        // token-scoped: only the row that literally contains '<token>%' matches
        performSearch(teacherToken, "q", token + "%")
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(percentName));

        // '_' must not act as a single-character wildcard: no seeded name contains a literal underscore
        mockMvc.perform(get("/api/v1/mosques/search")
                        .header("Authorization", "Bearer " + teacherToken)
                        .param("q", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void search_paginates() throws Exception {
        String teacherToken = createTeacher();
        String token = UUID.randomUUID().toString();
        int total = 25;
        List<String> expectedFirstPage = new ArrayList<>();
        for (int i = 1; i <= total; i++) {
            String name = String.format("Search Determinism %s %02d", token, i);
            seedMosque(name);
            if (i <= 10) {
                expectedFirstPage.add(name);
            }
        }

        performSearch(teacherToken, "q", token, "size", "10", "page", "0")
                .andExpect(jsonPath("$.data.totalElements").value(25))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.content.length()").value(10));

        // stable name ASC ordering makes page 0 deterministic
        List<String> firstPage = fetchNames(teacherToken, "q", token, "size", "10", "page", "0");
        assertThat(firstPage).isEqualTo(expectedFirstPage);

        performSearch(teacherToken, "q", token, "size", "10", "page", "2")
                .andExpect(jsonPath("$.data.totalElements").value(25))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(5));
    }

    @Test
    void search_returnsOnlyActive() throws Exception {
        String teacherToken = createTeacher();
        String token = UUID.randomUUID().toString();
        String activeName = "Active Only Mosque " + token;
        String inactiveName = "Inactive Only Mosque " + token;
        seedMosque(activeName);
        String inactiveId = seedMosque(inactiveName);

        String superToken = createSuperAdmin("super-search-inactive-" + UUID.randomUUID() + "@test.darb");
        deactivateMosque(superToken, inactiveId);

        performSearch(teacherToken, "q", token)
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(activeName));
    }

    @Test
    void search_invalidCountry_returns400() throws Exception {
        String teacherToken = createTeacher();

        mockMvc.perform(get("/api/v1/mosques/search")
                        .header("Authorization", "Bearer " + teacherToken)
                        .param("country", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void search_withPendingJoinRequest_returns403() throws Exception {
        String teacherEmail = "teacher-pending-search-" + UUID.randomUUID() + "@test.darb";
        registerRole(teacherEmail, "teacher");
        String teacherToken = login(teacherEmail);

        String mosqueId = seedMosque("Pending Search Mosque " + UUID.randomUUID());

        mockMvc.perform(post("/api/v1/mosques/join-requests")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mosqueId": "%s"
                                }
                                """.formatted(mosqueId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/mosques/search")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void search_sensitiveFieldsNotExposed() throws Exception {
        String teacherToken = createTeacher();
        String token = UUID.randomUUID().toString();
        String name = "Sensitive Field Mosque " + token;
        seedMosqueWithContacts(name, "Riyadh", "Riyadh Province", "SA",
                "+966512345678", "info@test.darb", "https://example.com/logo.png");

        performSearch(teacherToken, "q", token)
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].name").value(name))
                .andExpect(jsonPath("$.data.content[0].city").value("Riyadh"))
                .andExpect(jsonPath("$.data.content[0].addressCountry").value("SA"))
                .andExpect(jsonPath("$.data.content[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].logoUrl").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].settings").doesNotExist());
    }

    private String createTeacher() throws Exception {
        String email = "teacher-search-" + UUID.randomUUID() + "@test.darb";
        registerRole(email, "teacher");
        return login(email);
    }

    private ResultActions performSearch(String authToken, String... queryParams) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/mosques/search")
                .header("Authorization", "Bearer " + authToken);
        for (int i = 0; i < queryParams.length; i += 2) {
            request.param(queryParams[i], queryParams[i + 1]);
        }
        return mockMvc.perform(request).andExpect(status().isOk());
    }

    private List<String> fetchNames(String authToken, String... queryParams) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/mosques/search")
                .header("Authorization", "Bearer " + authToken);
        for (int i = 0; i < queryParams.length; i += 2) {
            request.param(queryParams[i], queryParams[i + 1]);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.content[*].name");
    }

    private String seedMosque(String name) throws Exception {
        return seedMosque(name, "Riyadh", "Riyadh Province", null);
    }

    private String seedMosque(String name, String city, String state, String country) throws Exception {
        String adminEmail = "seed-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        return onboardMosque(adminToken, name, city, state, country);
    }

    private String onboardMosque(String token, String name, String city, String state, String country) throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("{\n");
        body.append("  \"name\": \"").append(name).append("\",\n");
        body.append("  \"city\": \"").append(city).append("\",\n");
        body.append("  \"addressState\": \"").append(state).append("\"");
        if (country != null && !country.isBlank()) {
            body.append(",\n  \"addressCountry\": \"").append(country).append("\"");
        }
        body.append("\n}");

        MvcResult result = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.mosque.id");
    }

    private String seedMosqueWithContacts(String name, String city, String state, String country,
                                          String phone, String email, String logoUrl) throws Exception {
        String adminEmail = "seed-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);

        StringBuilder body = new StringBuilder();
        body.append("{\n");
        body.append("  \"name\": \"").append(name).append("\",\n");
        body.append("  \"city\": \"").append(city).append("\",\n");
        body.append("  \"addressState\": \"").append(state).append("\"");
        if (country != null && !country.isBlank()) {
            body.append(",\n  \"addressCountry\": \"").append(country).append("\"");
        }
        if (phone != null) {
            body.append(",\n  \"phone\": \"").append(phone).append("\"");
        }
        if (email != null) {
            body.append(",\n  \"email\": \"").append(email).append("\"");
        }
        if (logoUrl != null) {
            body.append(",\n  \"logoUrl\": \"").append(logoUrl).append("\"");
        }
        body.append("\n}");

        MvcResult result = mockMvc.perform(post("/api/v1/mosques/onboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.mosque.id");
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

    private void deactivateMosque(String superAdminToken, String mosqueId) throws Exception {
        mockMvc.perform(delete("/api/v1/mosques/" + mosqueId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
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
}
