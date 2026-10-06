package com.gymplanner.plan;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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

class PlanIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    EquipmentTypeRepository types;

    TestUsers.Registered owner;
    UUID gymId;
    UUID cableId;
    UUID pulldownId;
    UUID benchPressId;
    UUID pushUpId;

    @BeforeEach
    void setUp() throws Exception {
        owner = TestUsers.register(mvc, objectMapper);
        gymId = TestGyms.create(mvc, objectMapper, owner);
        cableId = TestEquipment.create(mvc, objectMapper, owner, gymId, "Wyciąg górny", "CABLE",
                types.findByCode("LAT_PULLDOWN").orElseThrow().getId());
        pulldownId = exerciseId("Ściąganie drążka wyciągu górnego do klatki");
        benchPressId = exerciseId("Wyciskanie sztangi na ławce płaskiej");
        pushUpId = exerciseId("Pompki");
    }

    @Test
    void buildPlanWithDaysAndItems() throws Exception {
        String planId = createPlan("Push Pull");
        String dayA = body(addDay(planId, "Dzień A – pull").andExpect(status().isCreated()).andReturn())
                .at("/days/0/id").asText();

        addItem(planId, dayA, pulldownId, cableId).andExpect(status().isCreated())
                .andExpect(jsonPath("$.days[0].items[0].exercise.id").value(pulldownId.toString()))
                .andExpect(jsonPath("$.days[0].items[0].equipment.name").value("Wyciąg górny"))
                .andExpect(jsonPath("$.days[0].items[0].equipmentUnavailable").value(false));
        addItem(planId, dayA, pushUpId, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.days[0].items", hasSize(2)))
                .andExpect(jsonPath("$.days[0].items[1].position").value(1));

        mvc.perform(get("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(jsonPath("$[0].name").value("Push Pull"))
                .andExpect(jsonPath("$[0].dayCount").value(1))
                .andExpect(jsonPath("$[0].exerciseCount").value(2));
    }

    @Test
    void onlyExercisesAvailableInGymCanBeAdded() throws Exception {
        String planId = createPlan("Plan");
        String day = body(addDay(planId, "A").andReturn()).at("/days/0/id").asText();

        // Brak sztangi w siłowni
        addItem(planId, day, benchPressId, null).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("exercise_not_available"));
        // Ćwiczenie wymaga sprzętu
        addItem(planId, day, pulldownId, null).andExpect(status().isUnprocessableEntity());
        // Sprzęt nie pasuje do ćwiczenia
        UUID bench = TestEquipment.create(mvc, objectMapper, owner, gymId, "Ławka", "BENCH", null);
        addItem(planId, day, pulldownId, bench).andExpect(status().isUnprocessableEntity());
        // Sprzęt z innej siłowni
        TestUsers.Registered other = TestUsers.register(mvc, objectMapper);
        UUID otherGym = TestGyms.create(mvc, objectMapper, other);
        UUID foreignCable = TestEquipment.create(mvc, objectMapper, other, otherGym, "Wyciąg", "CABLE",
                types.findByCode("LAT_PULLDOWN").orElseThrow().getId());
        addItem(planId, day, pulldownId, foreignCable).andExpect(status().isUnprocessableEntity());

        Map<String, Object> badReps = itemBody(pushUpId, null);
        badReps.put("repsMin", 12);
        badReps.put("repsMax", 8);
        mvc.perform(post("/api/v1/plans/{p}/days/{d}/items", planId, day)
                        .header(HttpHeaders.AUTHORIZATION, owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(badReps)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("invalid_reps_range"));
    }

    @Test
    void planIsVisibleOnlyToOwner() throws Exception {
        String planId = createPlan("Prywatny");
        TestUsers.Registered stranger = TestUsers.register(mvc, objectMapper);
        TestGyms.join(mvc, stranger, gymId);

        mvc.perform(get("/api/v1/plans/{id}", planId).header(HttpHeaders.AUTHORIZATION, stranger.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/plans/{id}", planId).header(HttpHeaders.AUTHORIZATION, stranger.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "Hack"))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/plans/{id}", planId).header(HttpHeaders.AUTHORIZATION, stranger.bearer()))
                .andExpect(status().isNotFound());
        addDay(planId, "X", stranger).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, stranger.bearer()))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void creatingPlanRequiresGymMembership() throws Exception {
        TestUsers.Registered stranger = TestUsers.register(mvc, objectMapper);
        mvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, stranger.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("gymId", gymId, "name", "Plan"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void reorderItemsAndDays() throws Exception {
        String planId = createPlan("Kolejność");
        String day = body(addDay(planId, "A").andReturn()).at("/days/0/id").asText();
        addItem(planId, day, pulldownId, cableId);
        JsonNode plan = body(addItem(planId, day, pushUpId, null).andReturn());
        String first = plan.at("/days/0/items/0/id").asText();
        String second = plan.at("/days/0/items/1/id").asText();

        mvc.perform(post("/api/v1/plans/{p}/items/{i}/move", planId, second).param("direction", "UP")
                        .header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].items[0].id").value(second))
                .andExpect(jsonPath("$.days[0].items[1].id").value(first))
                .andExpect(jsonPath("$.days[0].items[1].position").value(1));

        mvc.perform(put("/api/v1/plans/{p}/days/{d}/items/order", planId, day)
                        .header(HttpHeaders.AUTHORIZATION, owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("ids", List.of(first, second)))))
                .andExpect(jsonPath("$.days[0].items[0].id").value(first));

        mvc.perform(put("/api/v1/plans/{p}/days/{d}/items/order", planId, day)
                        .header(HttpHeaders.AUTHORIZATION, owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("ids", List.of(first)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("invalid_order"));

        String dayB = body(addDay(planId, "B").andReturn()).at("/days/1/id").asText();
        mvc.perform(post("/api/v1/plans/{p}/days/{d}/move", planId, dayB).param("direction", "UP")
                        .header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(jsonPath("$.days[0].name").value("B"))
                .andExpect(jsonPath("$.days[1].items", hasSize(2)));
    }

    @Test
    void copyArchiveAndDelete() throws Exception {
        String planId = createPlan("Bazowy");
        String day = body(addDay(planId, "A").andReturn()).at("/days/0/id").asText();
        addItem(planId, day, pushUpId, null);

        String copyId = body(mvc.perform(post("/api/v1/plans/{id}/copy", planId)
                        .header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bazowy (kopia)"))
                .andExpect(jsonPath("$.days[0].items[0].exercise.name").value("Pompki"))
                .andReturn()).get("id").asText();

        mvc.perform(post("/api/v1/plans/{id}/archive", planId).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(jsonPath("$.archived").value(true));
        mvc.perform(get("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(copyId));
        mvc.perform(get("/api/v1/plans").param("archived", "true").header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(jsonPath("$[0].id").value(planId));

        mvc.perform(delete("/api/v1/plans/{id}", copyId).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/plans/{id}", copyId).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void removedEquipmentMarksPlanItemWithWarning() throws Exception {
        String planId = createPlan("Z ostrzeżeniem");
        String day = body(addDay(planId, "A").andReturn()).at("/days/0/id").asText();
        addItem(planId, day, pulldownId, cableId);

        mvc.perform(put("/api/v1/equipment/{id}", cableId).header(HttpHeaders.AUTHORIZATION, owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Wyciąg górny", "category", "CABLE",
                                "equipmentTypeId", types.findByCode("LAT_PULLDOWN").orElseThrow().getId(),
                                "status", "REMOVED_FROM_GYM", "version", 0))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/plans/{id}", planId).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(jsonPath("$.warningCount").value(1))
                .andExpect(jsonPath("$.days[0].items[0].equipmentUnavailable").value(true))
                .andExpect(jsonPath("$.days[0].items[0].equipment.status").value("REMOVED_FROM_GYM"));

        // Usunięcie wpisu sprzętu – pozycja nadal widoczna, z ostrzeżeniem
        mvc.perform(delete("/api/v1/equipment/{id}", cableId).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/plans/{id}", planId).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(jsonPath("$.days[0].items[0].equipment.deleted").value(true))
                .andExpect(jsonPath("$.days[0].items[0].equipmentUnavailable").value(true));
    }

    private UUID exerciseId(String name) throws Exception {
        JsonNode list = body(mvc.perform(get("/api/v1/exercises").header(HttpHeaders.AUTHORIZATION, owner.bearer())
                .param("q", name)).andReturn());
        for (JsonNode node : list) {
            if (node.get("name").asText().equals(name)) {
                return UUID.fromString(node.get("id").asText());
            }
        }
        throw new IllegalStateException("Exercise not seeded: " + name);
    }

    private String createPlan(String name) throws Exception {
        return body(mvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("gymId", gymId, "name", name))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andReturn()).get("id").asText();
    }

    private ResultActions addDay(String planId, String name) throws Exception {
        return addDay(planId, name, owner);
    }

    private ResultActions addDay(String planId, String name, TestUsers.Registered user) throws Exception {
        return mvc.perform(post("/api/v1/plans/{id}/days", planId).header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", name))));
    }

    private ResultActions addItem(String planId, String dayId, UUID exerciseId, UUID equipmentId) throws Exception {
        return mvc.perform(post("/api/v1/plans/{p}/days/{d}/items", planId, dayId)
                .header(HttpHeaders.AUTHORIZATION, owner.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(itemBody(exerciseId, equipmentId))));
    }

    private static Map<String, Object> itemBody(UUID exerciseId, UUID equipmentId) {
        Map<String, Object> body = new HashMap<>();
        body.put("exerciseId", exerciseId);
        if (equipmentId != null) {
            body.put("equipmentId", equipmentId);
        }
        body.put("sets", 3);
        body.put("repsMin", 8);
        body.put("repsMax", 12);
        body.put("targetWeightKg", 40.5);
        body.put("restSeconds", 90);
        body.put("note", "Kontrolowany ruch");
        return body;
    }
}
