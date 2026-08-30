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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MosqueCitiesIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "P@ssw0rd1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void superAdmin_listCities_returnsDistinctForCountry() throws Exception {
        String superToken = createSuperAdmin("super-cities-distinct-" + UUID.randomUUID() + "@test.darb");
        String token = UUID.randomUUID().toString();
        // lexicographic order: Alpha before Zulu (token suffix shared)
        String cityAlpha = "AlphaCity-" + token;
        String cityZulu = "ZuluCity-" + token;
        String cityOtherCountry = "OtherCountryCity-" + token;

        seedMosque("Cities SA Alpha " + token, cityAlpha, "Riyadh Province", "SA");
        seedMosque("Cities SA Zulu " + token, cityZulu, "Makkah Province", "SA");
        // duplicate city — must appear only once
        seedMosque("Cities SA Alpha Dup " + token, cityAlpha, "Riyadh Province", "SA");
        seedMosque("Cities DE Other " + token, cityOtherCountry, "Berlin", "DE");

        List<String> cities = fetchCities(superToken, "SA");
        List<String> ours = cities.stream()
                .filter(c -> c.contains(token))
                .collect(Collectors.toList());

        assertThat(ours).containsExactly(cityAlpha, cityZulu);
        assertThat(cities).doesNotContain(cityOtherCountry);

        // full response for the country stays sorted ascending
        assertThat(cities).isSortedAccordingTo(String::compareTo);
    }

    @Test
    void superAdmin_listCities_invalidCountry_returns400() throws Exception {
        String superToken = createSuperAdmin("super-cities-bad-country-" + UUID.randomUUID() + "@test.darb");

        mockMvc.perform(get("/api/v1/mosques/cities")
                        .header("Authorization", "Bearer " + superToken)
                        .param("country", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listCities_includesArabicCity() throws Exception {
        String superToken = createSuperAdmin("super-cities-arabic-" + UUID.randomUUID() + "@test.darb");
        String token = UUID.randomUUID().toString();
        String arabicCity = "الرياض-" + token;
        seedMosque("Arabic Cities Mosque " + token, arabicCity, "Riyadh Province", "SA");

        List<String> cities = fetchCities(superToken, "SA");
        assertThat(cities).contains(arabicCity);
    }

    @Test
    void listCities_activeOnly_excludesInactive() throws Exception {
        String superToken = createSuperAdmin("super-cities-active-" + UUID.randomUUID() + "@test.darb");
        String token = UUID.randomUUID().toString();
        String inactiveCity = "InactiveCity-" + token;
        String activeCity = "ActiveCity-" + token;

        String inactiveId = seedMosque("Inactive Cities Mosque " + token, inactiveCity, "Eastern Province", "SA");
        seedMosque("Active Cities Mosque " + token, activeCity, "Eastern Province", "SA");
        deactivateMosque(superToken, inactiveId);

        List<String> activeOnly = fetchCities(superToken, "SA", true);
        assertThat(activeOnly).doesNotContain(inactiveCity);
        assertThat(activeOnly).contains(activeCity);

        List<String> includingInactive = fetchCities(superToken, "SA", false);
        assertThat(includingInactive).contains(inactiveCity, activeCity);
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

    private String seedMosque(String name, String city, String state, String country) throws Exception {
        String adminEmail = "seed-cities-" + UUID.randomUUID() + "@test.darb";
        registerRole(adminEmail, "mosque_admin");
        String adminToken = login(adminEmail);
        return onboardMosque(adminToken, name, city, state, country);
    }

    private String onboardMosque(String token, String name, String city, String state, String country)
            throws Exception {
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
                        .header("X-Audit-Reason", "Test deactivate mosque for filter coverage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private List<String> fetchCities(String authToken, String country) throws Exception {
        return fetchCities(authToken, country, null);
    }

    private List<String> fetchCities(String authToken, String country, Boolean activeOnly) throws Exception {
        var request = get("/api/v1/mosques/cities")
                .header("Authorization", "Bearer " + authToken)
                .param("country", country);
        if (activeOnly != null) {
            request.param("activeOnly", String.valueOf(activeOnly));
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data");
    }
}
