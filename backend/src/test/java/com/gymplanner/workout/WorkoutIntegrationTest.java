package com.gymplanner.workout;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymplanner.equipment.EquipmentTypeRepository;
import com.gymplanner.support.AbstractIntegrationTest;
import com.gymplanner.support.TestEquipment;
import com.gymplanner.support.TestGyms;
import com.gymplanner.support.TestUsers;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class WorkoutIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    EquipmentTypeRepository types;

    TestUsers.Registered user;
    UUID gymId;
    UUID cableId;
    String pulldownId;
    String planId;
    String dayId;

    @BeforeEach
    void setUp() throws Exception {
        user = TestUsers.register(mvc, objectMapper);
        gymId = TestGyms.create(mvc, objectMapper, user);
        cableId = TestEquipment.create(mvc, objectMapper, user, gymId, "Wyciąg górny", "CABLE",
                types.findByCode("LAT_PULLDOWN").orElseThrow().getId());
        pulldownId = body(mvc.perform(get("/api/v1/exercises").header(HttpHeaders.AUTHORIZATION, user.bearer())
                .param("q", "sciaganie drazka wyciagu gornego")).andReturn()).get(0).get("id").asText();

        planId = body(mvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("gymId", gymId, "name", "Plan"))))
                .andReturn()).get("id").asText();
        dayId = body(mvc.perform(post("/api/v1/plans/{id}/days", planId).header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "Dzień A"))))
                .andReturn()).at("/days/0/id").asText();
        Map<String, Object> item = new HashMap<>(Map.of("exerciseId", pulldownId, "equipmentId", cableId, "sets", 3,
                "repsMin", 8, "repsMax", 12, "restSeconds", 90));
        item.put("targetWeightKg", 40);
        mvc.perform(post("/api/v1/plans/{p}/days/{d}/items", planId, dayId)
                .header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(item)));
    }

    @Test
    void fullWorkoutFromPlanWithPreviousResult() throws Exception {
        JsonNode session = start();
        mvc.perform(get("/api/v1/sessions/active").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.get("id").asText()));
        org.assertj.core.api.Assertions.assertThat(session.at("/exercises/0/sets")).hasSize(3);
        org.assertj.core.api.Assertions.assertThat(session.at("/exercises/0/sets/0/weightKg").asDouble()).isEqualTo(40);
        org.assertj.core.api.Assertions.assertThat(session.at("/exercises/0/previous").isMissingNode()).isTrue();

        String sessionId = session.get("id").asText();
        completeSet(sessionId, session.at("/exercises/0/sets/0/id").asText(), 12, 40)
                .andExpect(jsonPath("$.exercises[0].sets[0].completed").value(true))
                .andExpect(jsonPath("$.exercises[0].sets[0].reps").value(12));
        completeSet(sessionId, session.at("/exercises/0/sets/1/id").asText(), 10, 42.5);

        // Drugi raz nie można wystartować
        mvc.perform(post("/api/v1/sessions").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("gymId", gymId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_already_active"))
                .andExpect(jsonPath("$.activeSessionId").value(sessionId));

        mvc.perform(post("/api/v1/sessions/{id}/finish", sessionId).header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("note", "Dobrze poszło"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
        mvc.perform(get("/api/v1/sessions/active").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isNoContent());

        // Zakończonego treningu nie można edytować
        completeSet(sessionId, session.at("/exercises/0/sets/2/id").asText(), 8, 40)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("session_not_active"));

        mvc.perform(get("/api/v1/sessions").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Plan – Dzień A"))
                .andExpect(jsonPath("$.content[0].completedSets").value(2))
                .andExpect(jsonPath("$.content[0].volumeKg").value(905.0));

        // Kolejny trening pokazuje poprzedni wynik
        JsonNode next = start();
        org.assertj.core.api.Assertions.assertThat(next.at("/exercises/0/previous/sessionId").asText())
                .isEqualTo(sessionId);
        org.assertj.core.api.Assertions.assertThat(next.at("/exercises/0/previous/sets")).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(next.at("/exercises/0/previous/sets/1/weightKg").asDouble())
                .isEqualTo(42.5);

        mvc.perform(get("/api/v1/exercises/{id}/history", pulldownId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sets[0].reps").value(12));
    }

    @Test
    void adHocWorkoutAddsOnlyAvailableExercises() throws Exception {
        JsonNode session = body(mvc.perform(post("/api/v1/sessions").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("gymId", gymId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises", hasSize(0)))
                .andReturn());
        String sessionId = session.get("id").asText();

        mvc.perform(post("/api/v1/sessions/{id}/exercises", sessionId).header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("exerciseId", pulldownId))))
                .andExpect(status().isUnprocessableEntity());

        JsonNode updated = body(mvc.perform(post("/api/v1/sessions/{id}/exercises", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("exerciseId", pulldownId, "equipmentId", cableId, "sets", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets", hasSize(2)))
                .andReturn());

        mvc.perform(post("/api/v1/sessions/{s}/exercises/{e}/sets", sessionId,
                        updated.at("/exercises/0/id").asText()).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.exercises[0].sets", hasSize(3)))
                .andExpect(jsonPath("$.exercises[0].sets[2].setNumber").value(3));

        mvc.perform(post("/api/v1/sessions/{id}/abandon", sessionId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/sessions").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void sessionsArePrivate() throws Exception {
        String sessionId = start().get("id").asText();
        TestUsers.Registered other = TestUsers.register(mvc, objectMapper);
        mvc.perform(get("/api/v1/sessions/{id}", sessionId).header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/sessions").header(HttpHeaders.AUTHORIZATION, other.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("planId", planId, "planDayId", dayId))))
                .andExpect(status().isNotFound());
    }

    private JsonNode start() throws Exception {
        return body(mvc.perform(post("/api/v1/sessions").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("planId", planId, "planDayId", dayId))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private org.springframework.test.web.servlet.ResultActions completeSet(String sessionId, String setId, int reps,
            double weight) throws Exception {
        return mvc.perform(patch("/api/v1/sessions/{s}/sets/{set}", sessionId, setId)
                .header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("reps", reps, "weightKg", weight, "completed", true))));
    }
}
