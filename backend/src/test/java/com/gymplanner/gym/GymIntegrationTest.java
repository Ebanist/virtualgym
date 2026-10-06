package com.gymplanner.gym;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.support.AbstractIntegrationTest;
import com.gymplanner.support.TestGyms;
import com.gymplanner.support.TestUsers;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class GymIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createGymWarnsAboutPossibleDuplicateInSameCity() throws Exception {
        TestUsers.Registered user = TestUsers.register(mvc, objectMapper);
        String city = "Łódź-" + UUID.randomUUID().toString().substring(0, 8);
        TestGyms.create(mvc, objectMapper, user, "Fitness Platinium Manufaktura", city);

        Map<String, Object> similar = Map.of("name", "Platinium Fitness  manufaktura", "city", city.toUpperCase(),
                "address", "ul. Drewnowska 58");
        mvc.perform(post("/api/v1/gyms").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(similar)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("gym_possible_duplicate"))
                .andExpect(jsonPath("$.candidates", hasSize(1)))
                .andExpect(jsonPath("$.candidates[0].name").value("Fitness Platinium Manufaktura"));

        // Ta sama nazwa w innym mieście – bez ostrzeżenia.
        mvc.perform(post("/api/v1/gyms").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Fitness Platinium Manufaktura",
                                "city", "Inne-" + UUID.randomUUID(), "address", "ul. Inna 1"))))
                .andExpect(status().isCreated());

        // Potwierdzenie duplikatu – zapis.
        mvc.perform(post("/api/v1/gyms").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Platinium Fitness  manufaktura", "city", city,
                                "address", "ul. Drewnowska 58", "confirmDuplicate", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.member").value(true))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.status").value("COMMUNITY"));
    }

    @Test
    void searchByNameAndCityIgnoresCaseAndDiacritics() throws Exception {
        TestUsers.Registered user = TestUsers.register(mvc, objectMapper);
        String tag = UUID.randomUUID().toString().substring(0, 8);
        TestGyms.create(mvc, objectMapper, user, "Siłownia Żelazna " + tag, "Kraków");
        TestGyms.create(mvc, objectMapper, user, "Inna " + tag, "Kraków");

        mvc.perform(get("/api/v1/gyms").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .param("q", "zelazna " + tag).param("city", "krak"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Siłownia Żelazna " + tag))
                .andExpect(jsonPath("$.content[0].member").value(true));

        mvc.perform(get("/api/v1/gyms").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .param("q", tag).param("size", "1"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void joinAndLeaveGym() throws Exception {
        TestUsers.Registered owner = TestUsers.register(mvc, objectMapper);
        TestUsers.Registered other = TestUsers.register(mvc, objectMapper);
        UUID gymId = TestGyms.create(mvc, objectMapper, owner);

        mvc.perform(get("/api/v1/gyms/{id}", gymId).header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member").value(false))
                .andExpect(jsonPath("$.memberCount").value(1));

        TestGyms.join(mvc, other, gymId);
        TestGyms.join(mvc, other, gymId); // idempotentne

        mvc.perform(get("/api/v1/gyms/{id}", gymId).header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(jsonPath("$.member").value(true))
                .andExpect(jsonPath("$.memberCount").value(2));
        mvc.perform(get("/api/v1/me/gyms").header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(gymId.toString()));

        mvc.perform(delete("/api/v1/gyms/{id}/membership", gymId).header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/gyms").header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unknownGymReturns404() throws Exception {
        TestUsers.Registered user = TestUsers.register(mvc, objectMapper);
        mvc.perform(get("/api/v1/gyms/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
        mvc.perform(post("/api/v1/gyms/{id}/membership", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createGymValidatesInput() throws Exception {
        TestUsers.Registered user = TestUsers.register(mvc, objectMapper);
        mvc.perform(post("/api/v1/gyms").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "X"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(3)));
    }
}
