package com.gymplanner.exercise;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ExerciseIntegrationTest extends AbstractIntegrationTest {

    static final String PULLDOWN = "Ściąganie drążka wyciągu górnego do klatki";

    @Autowired
    EquipmentTypeRepository types;

    TestUsers.Registered member;
    UUID gymId;

    @BeforeEach
    void setUp() throws Exception {
        member = TestUsers.register(mvc, objectMapper);
        gymId = TestGyms.create(mvc, objectMapper, member);
    }

    @Test
    void libraryContainsSeededExercises() throws Exception {
        mvc.perform(get("/api/v1/exercises").header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(50)));
        mvc.perform(get("/api/v1/exercises").header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .param("q", "sciaganie drazka").param("muscle", "BACK"))
                .andExpect(jsonPath("$[*].name", hasItem(PULLDOWN)))
                .andExpect(jsonPath("$[0].equipmentTypes[0].code").value("LAT_PULLDOWN"));
    }

    @Test
    void emptyGymOffersOnlyBodyweightExercises() throws Exception {
        mvc.perform(get("/api/v1/gyms/{gymId}/exercises/available", gymId)
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].exercise.bodyweight", everyItem(org.hamcrest.Matchers.is(true))))
                .andExpect(jsonPath("$[*].exercise.name", hasItem("Pompki")));
    }

    @Test
    void equipmentTypeUnlocksExercisesAndRemovalLocksThemAgain() throws Exception {
        UUID cable = TestEquipment.create(mvc, objectMapper, member, gymId, "Wyciąg górny", "CABLE",
                types.findByCode("LAT_PULLDOWN").orElseThrow().getId());

        JsonNode available = body(mvc.perform(get("/api/v1/gyms/{gymId}/exercises/available", gymId)
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()).param("q", "drazka wyciagu"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].exercise.name").value(PULLDOWN))
                .andExpect(jsonPath("$[0].equipment[0].id").value(cable.toString()))
                .andReturn());
        org.assertj.core.api.Assertions.assertThat(available.get(0).at("/equipment/0/name").asText())
                .isEqualTo("Wyciąg górny");

        mvc.perform(get("/api/v1/equipment/{id}/exercises", cable).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(jsonPath("$[*].exercise.name", hasItem(PULLDOWN)))
                .andExpect(jsonPath("$[0].byType").value(true));

        mvc.perform(put("/api/v1/equipment/{id}", cable).header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Wyciąg górny", "category", "CABLE", "status", "REMOVED_FROM_GYM",
                                "version", 0))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/gyms/{gymId}/exercises/available", gymId)
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()).param("q", "drazka wyciagu"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void explicitLinkAndCustomExercise() throws Exception {
        UUID machine = TestEquipment.create(mvc, objectMapper, member, gymId, "Maszyna wielofunkcyjna",
                "STRENGTH_MACHINE", null);
        JsonNode pulldown = body(mvc.perform(get("/api/v1/exercises").header(HttpHeaders.AUTHORIZATION, member.bearer())
                .param("q", "sciaganie drazka wyciagu")).andReturn()).get(0);

        mvc.perform(put("/api/v1/equipment/{e}/exercises/{x}", machine, pulldown.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/equipment/{id}/exercises", machine).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].linked").value(true))
                .andExpect(jsonPath("$[0].byType").value(false));

        // Własne ćwiczenie przypisane do sprzętu
        mvc.perform(post("/api/v1/gyms/{gymId}/exercises", gymId).header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Wyciskanie na maszynie wielofunkcyjnej",
                                "primaryMuscle", "CHEST", "secondaryMuscles", List.of("TRICEPS", "CHEST"),
                                "equipmentIds", List.of(machine)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("CUSTOM"))
                .andExpect(jsonPath("$.gymId").value(gymId.toString()))
                .andExpect(jsonPath("$.secondaryMuscles", hasSize(1)));

        mvc.perform(get("/api/v1/gyms/{gymId}/exercises/available", gymId)
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()).param("q", "wielofunkcyjnej"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].equipment[0].id").value(machine.toString()));

        // Własne ćwiczenie nie jest widoczne w bibliotece osoby spoza siłowni
        TestUsers.Registered outsider = TestUsers.register(mvc, objectMapper);
        mvc.perform(get("/api/v1/exercises").header(HttpHeaders.AUTHORIZATION, outsider.bearer())
                        .param("q", "wielofunkcyjnej"))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(delete("/api/v1/equipment/{e}/exercises/{x}", machine, pulldown.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/equipment/{id}/exercises", machine).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(jsonPath("$[*].exercise.name", not(hasItem(PULLDOWN))));
    }

    @Test
    void customExerciseRequiresEquipmentFromSameGymUnlessBodyweight() throws Exception {
        mvc.perform(post("/api/v1/gyms/{gymId}/exercises", gymId).header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Coś", "primaryMuscle", "ABS"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("exercise_equipment_required"));

        TestUsers.Registered other = TestUsers.register(mvc, objectMapper);
        UUID otherGym = TestGyms.create(mvc, objectMapper, other);
        UUID foreign = TestEquipment.create(mvc, objectMapper, other, otherGym, "Ławka", "BENCH", null);
        mvc.perform(post("/api/v1/gyms/{gymId}/exercises", gymId).header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Coś", "primaryMuscle", "ABS", "equipmentIds", List.of(foreign)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("equipment_not_in_gym"));

        mvc.perform(post("/api/v1/gyms/{gymId}/exercises", gymId).header(HttpHeaders.AUTHORIZATION, other.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Coś", "primaryMuscle", "ABS", "bodyweight", true))))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/gyms/{gymId}/exercises", gymId).header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Pajacyki", "primaryMuscle", "CARDIO", "bodyweight", true))))
                .andExpect(status().isCreated());
    }
}
