package com.darb.controllers.v1;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.ParentStudentRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
class MosqueListFilterIntegrationTest extends PostgresIntegrationTestBase {

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
    private PasswordEncoder passwordEncoder;

    @Test
    void superAdmin_nameSearch_returnsMatching() throws Exception {
        String superToken = createSuperAdmin("super-name-search-" + UUID.randomUUID() + "@test.darb");
        String token = UUID.randomUUID().toString();
        String alpha = "Alpha Filter " + token;
        String beta = "Beta Filter " + token;
        String gamma = "Gamma Filter " + token;
        seedMosque(alpha);
        seedMosque(beta);
        seedMosque(gamma);

        // the shared substring matches all three seeded mosques and nothing else
        List<String> matching = fetchNames(superToken, "q", token);
        assertThat(matching).containsExactlyInAnyOrder(alpha, beta, gamma);

        // a substring matching a single mosque returns only that mosque
        mockMvc.perform(get("/api/v1/mosques")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", beta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(beta));
    }

    @Test
    void superAdmin_countryFilter_caseInsensitiveParam() throws Exception {
        String superToken = createSuperAdmin("super-country-filter-" + UUID.randomUUID() + "@test.darb");
        String token = UUID.randomUUID().toString();
        String deMosque = "Country DE Mosque " + token;
        seedMosque(deMosque, "Berlin", "Berlin", "DE");

        // lowercase parameter matches an uppercase stored code
        assertThat(fetchNames(superToken, "country", "de")).contains(deMosque);

        // uppercase parameter matches too
        assertThat(fetchNames(superToken, "country", "DE")).contains(deMosque);

        // a different country excludes the DE mosque (scoped by the unique name so the
        // assertion stays deterministic on the shared Testcontainers database)
        mockMvc.perform(get("/api/v1/mosques")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", token)
                        .param("country", "SA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void superAdmin_cityFilter_arabic() throws Exception {
        String superToken = createSuperAdmin("super-city-arabic-" + UUID.randomUUID() + "@test.darb");
        String token = UUID.randomUUID().toString();
        String arabicCity = "الرياض";
        String riyadhMosque = "Arabic City Mosque " + token;
        seedMosque(riyadhMosque, arabicCity, "Riyadh Province", "SA");

        // Arabic city value (no case folding) still matches
        assertThat(fetchNames(superToken, "city", arabicCity)).contains(riyadhMosque);

        // deterministic check: the unique-named mosque matches the Arabic city filter
        mockMvc.perform(get("/api/v1/mosques")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", token)
                        .param("city", arabicCity))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(riyadhMosque));
    }

    @Test
    void superAdmin_filtersCombineAnd() throws Exception {
        String superToken = createSuperAdmin("super-and-filter-" + UUID.randomUUID() + "@test.darb");
        String token = UUID.randomUUID().toString();
        String dammam = "And Filter One " + token;
        String riyadhDe = "And Filter Two " + token;
        String jeddah = "And Filter Three " + token;
        seedMosque(dammam, "Dammam", "Eastern Province", "SA");
        seedMosque(riyadhDe, "Riyadh", "Riyadh Province", "DE");
        seedMosque(jeddah, "Jeddah", "Makkah Province", "SA");

        // q matches all three; country + city narrow to the single intersection row
        mockMvc.perform(get("/api/v1/mosques")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", token)
                        .param("country", "SA")
                        .param("city", "Dammam"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(dammam));

        // a different combination narrows to a different single row
        mockMvc.perform(get("/api/v1/mosques")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", token)
                        .param("country", "DE")
                        .param("city", "Riyadh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(riyadhDe));
    }

    @Test
    void superAdmin_literalPercentInSearch() throws Exception {
        String superToken = createSuperAdmin("super-percent-search-" + UUID.randomUUID() + "@test.darb");
        String token = UUID.randomUUID().toString();
        String percentName = "Percent Only " + token + "%";
        String plainName = "Percent Only " + token + " Plain";
        seedMosque(percentName);
        seedMosque(plainName);

        // a literal '%' in the query must not act as a LIKE wildcard
        mockMvc.perform(get("/api/v1/mosques")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(percentName));

        // a token-scoped pattern containing '%' also only matches the literal '%' row
        mockMvc.perform(get("/api/v1/mosques")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", token + "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(percentName));
    }

    @Test
    void superAdmin_noParams_returnsAllIncludingInactive() throws Exception {
        String superToken = createSuperAdmin("super-inactive-list-" + UUID.randomUUID() + "@test.darb");
        String token = UUID.randomUUID().toString();
        String keepName = "Keep Active Mosque " + token;
        String deactivatedName = "Deactivated List Mosque " + token;
        seedMosque(keepName);
        String deactivatedId = seedMosque(deactivatedName);

        deactivateMosque(superToken, deactivatedId);

        // the unfiltered list still contains the deactivated mosque
        Set<String> allNames = fetchAllMosqueNames(superToken);
        assertThat(allNames).contains(keepName, deactivatedName);
    }

    @Test
    void superAdmin_inactiveVisibleUnderFilter() throws Exception {
        String superToken = createSuperAdmin("super-inactive-filter-" + UUID.randomUUID() + "@test.darb");
        String token = UUID.randomUUID().toString();
        String deactivatedName = "Inactive Filtered Mosque " + token;
        String deactivatedId = seedMosque(deactivatedName, "Hofuf", "Eastern Province", "SA");

        deactivateMosque(superToken, deactivatedId);

        mockMvc.perform(get("/api/v1/mosques")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(deactivatedName));
    }

    @Test
    void parent_withFilterParams_ignoresFilters() throws Exception {
        String token = UUID.randomUUID().toString();

        // the parent's own mosque, created by an admin whose token we keep for the links
        String adminEmail = "admin-parent-filter-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        String ownMosqueName = "Parent Own Mosque " + token;
        String ownMosqueId = onboardMosque(adminToken, ownMosqueName, "Riyadh", "Riyadh Province", "SA");

        // decoy mosque whose name/country/city match the filters the parent will send
        String decoyName = "Parent Decoy Matching " + token;
        seedMosque(decoyName, "Berlin", "Berlin", "DE");

        String parentEmail = "parent-filter-" + UUID.randomUUID() + "@test.darb";
        registerRole(parentEmail, "parent");
        String studentEmail = "student-filter-" + UUID.randomUUID() + "@test.darb";
        registerRole(studentEmail, "student");
        String parentUserId = userRepository.findByEmail(parentEmail).orElseThrow().getId().toString();
        String studentUserId = userRepository.findByEmail(studentEmail).orElseThrow().getId().toString();
        String studentId = createStudent(adminToken, studentUserId, ownMosqueId);
        createParentStudentLink(adminToken, parentUserId, studentId);

        String parentToken = login(parentEmail);

        // filters have no effect for PARENT: only the parent's own active mosque is returned,
        // even though the q/country/city filters match the decoy mosque
        mockMvc.perform(get("/api/v1/mosques")
                        .header("Authorization", "Bearer " + parentToken)
                        .param("q", "Parent Decoy")
                        .param("country", "DE")
                        .param("city", "Berlin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value(ownMosqueName));
    }

    @Test
    void superAdmin_malformedCountry_returns400() throws Exception {
        String superToken = createSuperAdmin("super-bad-country-" + UUID.randomUUID() + "@test.darb");

        mockMvc.perform(get("/api/v1/mosques")
                        .header("Authorization", "Bearer " + superToken)
                        .param("country", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void superAdmin_paginationDeterministic() throws Exception {
        String superToken = createSuperAdmin("super-pagination-" + UUID.randomUUID() + "@test.darb");
        String token = UUID.randomUUID().toString();
        int total = 12;
        List<String> expectedFirstPage = new ArrayList<>();
        for (int i = 1; i <= total; i++) {
            String name = String.format("Determinism %s %02d", token, i);
            seedMosque(name);
            if (i <= 5) {
                expectedFirstPage.add(name);
            }
        }

        mockMvc.perform(get("/api/v1/mosques")
                        .header("Authorization", "Bearer " + superToken)
                        .param("q", token)
                        .param("size", "5")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(total));

        // two identical calls return the exact same name sequence (stable name ASC ordering)
        List<String> firstCall = fetchNames(superToken, "q", token, "size", "5", "page", "0");
        List<String> secondCall = fetchNames(superToken, "q", token, "size", "5", "page", "0");
        assertThat(firstCall).isEqualTo(expectedFirstPage);
        assertThat(secondCall).isEqualTo(firstCall);
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

    private void deactivateMosque(String superAdminToken, String mosqueId) throws Exception {
        mockMvc.perform(delete("/api/v1/mosques/" + mosqueId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Audit-Reason", "Test deactivate mosque for list filter coverage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String createStudent(String adminToken, String userId, String mosqueId) throws Exception {
        return com.darb.support.MembershipFixtures.seatStudent(
                mockMvc, userRepository, studentRepository, adminToken, userId, mosqueId, PASSWORD);
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

    private List<String> fetchNames(String authToken, String... queryParams) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/mosques")
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

    private Set<String> fetchAllMosqueNames(String authToken) throws Exception {
        Set<String> names = new HashSet<>();
        int page = 0;
        while (page <= 100) {
            MvcResult result = mockMvc.perform(get("/api/v1/mosques")
                            .header("Authorization", "Bearer " + authToken)
                            .param("page", String.valueOf(page))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andReturn();
            String body = result.getResponse().getContentAsString();
            names.addAll(com.jayway.jsonpath.JsonPath.read(body, "$.data.content[*].name"));
            boolean last = com.jayway.jsonpath.JsonPath.read(body, "$.data.last");
            if (last) {
                return names;
            }
            page++;
        }
        throw new IllegalStateException("Pagination did not terminate while collecting mosque names");
    }
}
