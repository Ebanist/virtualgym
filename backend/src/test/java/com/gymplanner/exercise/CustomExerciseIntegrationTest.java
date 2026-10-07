package com.gymplanner.exercise;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.equipment.EquipmentTypeRepository;
import com.gymplanner.support.AbstractIntegrationTest;
import com.gymplanner.support.TestEquipment;
import com.gymplanner.support.TestGyms;
import com.gymplanner.support.TestUsers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class CustomExerciseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    EquipmentTypeRepository types;

    TestUsers.Registered author;
    TestUsers.Registered member;
    UUID gymId;
    UUID treadmill;

    @BeforeEach
    void setUp() throws Exception {
        author = TestUsers.register(mvc, objectMapper);
        member = TestUsers.register(mvc, objectMapper);
        gymId = TestGyms.create(mvc, objectMapper, author);
        TestGyms.join(mvc, member, gymId);
        treadmill = TestEquipment.create(mvc, objectMapper, author, gymId, "Bieżnia", "CARDIO",
                types.findByCode("TREADMILL").orElseThrow().getId());
    }

    @Test
    void newExerciseIsPrivateByDefaultAndHiddenFromOtherMembers() throws Exception {
        String id = create(author, "Chodzenie na bieżni", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.equipmentIds[0]").value(treadmill.toString()))
                .andReturn().getResponse().getContentAsString();
        String exerciseId = objectMapper.readTree(id).get("id").asText();

        // Autor widzi je jako dostępne na bieżni
        mvc.perform(get("/api/v1/gyms/{g}/exercises/available", gymId).param("q", "chodzenie")
                        .header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].equipment[0].id").value(treadmill.toString()));

        // Inny członek go nie widzi – ani na listach, ani bezpośrednio
        mvc.perform(get("/api/v1/gyms/{g}/exercises/available", gymId).param("q", "chodzenie")
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/equipment/{id}/exercises", treadmill).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(jsonPath("$[*].exercise.id", not(hasItem(exerciseId))));
        mvc.perform(get("/api/v1/exercises/{id}", exerciseId).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isNotFound());
        // ...ani nie doda do planu
        String planId = createPlan(member);
        String dayId = addDay(member, planId);
        addItem(member, planId, dayId, exerciseId, treadmill).andExpect(status().isUnprocessableEntity());
        addItem(author, createPlan(author), null, exerciseId, treadmill).andExpect(status().isCreated());
    }

    @Test
    void publicExerciseIsVisibleToMembersWithOriginFlags() throws Exception {
        String exerciseId = id(create(author, "Marsz pod górę", "GYM").andExpect(status().isCreated()));

        mvc.perform(get("/api/v1/exercises").param("gymId", gymId.toString()).param("q", "marsz pod")
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].scope").value("CUSTOM"))
                .andExpect(jsonPath("$[0].visibility").value("GYM"))
                .andExpect(jsonPath("$[0].mine").value(false));

        String planId = createPlan(member);
        addItem(member, planId, addDay(member, planId), exerciseId, treadmill).andExpect(status().isCreated());

        // Używane w cudzym planie => nie można uczynić prywatnym
        update(author, exerciseId, "Marsz pod górę", "PRIVATE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("exercise_used_by_others"));
        update(author, exerciseId, "Marsz pod górę (incline)", "GYM")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Marsz pod górę (incline)"));
    }

    @Test
    void onlyAuthorCanEditOrDelete() throws Exception {
        String exerciseId = id(create(author, "Interwały na bieżni", "GYM"));
        update(member, exerciseId, "Hack", "GYM").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("exercise_not_owner"));
        mvc.perform(delete("/api/v1/exercises/{id}", exerciseId).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isForbidden());

        // Biblioteki nie da się edytować
        String global = objectMapper.readTree(mvc.perform(get("/api/v1/exercises").param("q", "pompki")
                .header(HttpHeaders.AUTHORIZATION, author.bearer())).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asText();
        update(author, global, "Pompki 2", "GYM").andExpect(status().isForbidden());
    }

    @Test
    void deleteHidesExerciseButPlansKeepIt() throws Exception {
        String exerciseId = id(create(author, "Sprint na bieżni", "PRIVATE"));
        String planId = createPlan(author);
        addItem(author, planId, addDay(author, planId), exerciseId, treadmill).andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/exercises/{id}", exerciseId).header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/gyms/{g}/exercises/available", gymId).param("q", "sprint")
                        .header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/plans/{id}", planId).header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(jsonPath("$.days[0].items[0].exercise.name").value("Sprint na bieżni"));
    }

    @Test
    void updateReplacesEquipmentLinks() throws Exception {
        UUID bike = TestEquipment.create(mvc, objectMapper, author, gymId, "Rower", "CARDIO", null);
        String exerciseId = id(create(author, "Rozgrzewka", "PRIVATE"));
        Map<String, Object> body = body("Rozgrzewka", "PRIVATE");
        body.put("equipmentIds", List.of(bike));
        mvc.perform(put("/api/v1/exercises/{id}", exerciseId).header(HttpHeaders.AUTHORIZATION, author.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentIds", hasSize(1)))
                .andExpect(jsonPath("$.equipmentIds[0]").value(bike.toString()));
        mvc.perform(get("/api/v1/equipment/{id}/exercises", treadmill).header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(jsonPath("$[*].exercise.name", not(hasItem("Rozgrzewka"))));
    }

    @Test
    void similarNamesAreSuggested() throws Exception {
        mvc.perform(get("/api/v1/gyms/{g}/exercises/similar", gymId).param("name", "bieg na biezni")
                        .header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(jsonPath("$[0].name").value("Bieg na bieżni"))
                .andExpect(jsonPath("$[0].scope").value("GLOBAL"));
    }

    private ResultActions create(TestUsers.Registered user, String name, String visibility) throws Exception {
        return mvc.perform(post("/api/v1/gyms/{g}/exercises", gymId).header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(body(name, visibility))));
    }

    private ResultActions update(TestUsers.Registered user, String id, String name, String visibility) throws Exception {
        return mvc.perform(put("/api/v1/exercises/{id}", id).header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(body(name, visibility))));
    }

    private Map<String, Object> body(String name, String visibility) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("primaryMuscle", "CARDIO");
        body.put("equipmentIds", List.of(treadmill));
        if (visibility != null) {
            body.put("visibility", visibility);
        }
        return body;
    }

    private String id(ResultActions result) throws Exception {
        return body(result.andReturn()).get("id").asText();
    }

    private String createPlan(TestUsers.Registered user) throws Exception {
        return body(mvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("gymId", gymId, "name", "Plan"))))
                .andReturn()).get("id").asText();
    }

    private String addDay(TestUsers.Registered user, String planId) throws Exception {
        return body(mvc.perform(post("/api/v1/plans/{id}/days", planId).header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "A"))))
                .andReturn()).at("/days/0/id").asText();
    }

    private ResultActions addItem(TestUsers.Registered user, String planId, String dayId, String exerciseId,
            UUID equipmentId) throws Exception {
        String day = dayId != null ? dayId : addDay(user, planId);
        return mvc.perform(post("/api/v1/plans/{p}/days/{d}/items", planId, day)
                .header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("exerciseId", exerciseId, "equipmentId", equipmentId, "sets", 3, "repsMin", 1,
                        "repsMax", 1, "restSeconds", 60))));
    }
}
