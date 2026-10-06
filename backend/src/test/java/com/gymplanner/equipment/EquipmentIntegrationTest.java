package com.gymplanner.equipment;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymplanner.support.AbstractIntegrationTest;
import com.gymplanner.support.TestEquipment;
import com.gymplanner.support.TestGyms;
import com.gymplanner.support.TestUsers;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class EquipmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    EquipmentTypeRepository types;

    TestUsers.Registered member;
    TestUsers.Registered outsider;
    UUID gymId;

    @BeforeEach
    void setUp() throws Exception {
        member = TestUsers.register(mvc, objectMapper);
        outsider = TestUsers.register(mvc, objectMapper);
        gymId = TestGyms.create(mvc, objectMapper, member);
    }

    @Test
    void onlyMembersCanAddEquipment() throws Exception {
        Map<String, Object> body = Map.of("name", "Wyciąg górny", "category", "CABLE");
        mvc.perform(post("/api/v1/gyms/{gymId}/equipment", gymId).header(HttpHeaders.AUTHORIZATION, outsider.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("gym_membership_required"));

        UUID typeId = types.findByCode("LAT_PULLDOWN").orElseThrow().getId();
        Map<String, Object> full = new HashMap<>(body);
        full.put("equipmentTypeId", typeId);
        full.put("quantity", 2);
        full.put("description", "Przy oknie");
        mvc.perform(post("/api/v1/gyms/{gymId}/equipment", gymId).header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(full)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Wyciąg górny"))
                .andExpect(jsonPath("$.equipmentType.code").value("LAT_PULLDOWN"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.source").value("COMMUNITY"))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.member").value(true));
    }

    @Test
    void listFiltersByCategoryAndNameAndSuggestsSimilar() throws Exception {
        TestEquipment.create(mvc, objectMapper, member, gymId, "Wyciąg górny", "CABLE", null);
        TestEquipment.create(mvc, objectMapper, member, gymId, "Ławka płaska", "BENCH", null);
        TestEquipment.create(mvc, objectMapper, member, gymId, "Ławka skośna", "BENCH", null);

        mvc.perform(get("/api/v1/gyms/{gymId}/equipment", gymId).header(HttpHeaders.AUTHORIZATION, outsider.bearer())
                        .param("category", "BENCH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Ławka płaska"));

        mvc.perform(get("/api/v1/gyms/{gymId}/equipment", gymId).header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .param("q", "lawka sko"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(get("/api/v1/gyms/{gymId}/equipment/similar", gymId)
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()).param("name", "wyciag gorny linka"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Wyciąg górny"));
    }

    @Test
    void updateRecordsHistoryAndChecksVersion() throws Exception {
        UUID id = TestEquipment.create(mvc, objectMapper, member, gymId, "Hantle", "FREE_WEIGHTS", null);
        TestGyms.join(mvc, outsider, gymId);
        TestUsers.Registered other = outsider;

        Map<String, Object> update = new HashMap<>();
        update.put("name", "Hantle 2-40 kg");
        update.put("category", "FREE_WEIGHTS");
        update.put("quantity", 20);
        update.put("status", "ACTIVE");
        update.put("version", 0);
        mvc.perform(put("/api/v1/equipment/{id}", id).header(HttpHeaders.AUTHORIZATION, other.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hantle 2-40 kg"))
                .andExpect(jsonPath("$.version").value(1));

        // Nieaktualna wersja => 409
        mvc.perform(put("/api/v1/equipment/{id}", id).header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("concurrent_modification"));

        update.put("status", "REMOVED_FROM_GYM");
        update.put("version", 1);
        mvc.perform(put("/api/v1/equipment/{id}", id).header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMOVED_FROM_GYM"));

        mvc.perform(get("/api/v1/equipment/{id}/history", id).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].changeType").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$.content[0].changes.status.oldValue").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].changes.status.newValue").value("REMOVED_FROM_GYM"))
                .andExpect(jsonPath("$.content[1].changeType").value("UPDATED"))
                .andExpect(jsonPath("$.content[1].user.id").value(other.id().toString()))
                .andExpect(jsonPath("$.content[1].changes.name.oldValue").value("Hantle"))
                .andExpect(jsonPath("$.content[1].changes.quantity.newValue").value("20"))
                .andExpect(jsonPath("$.content[2].changeType").value("CREATED"));
    }

    @Test
    void outsiderCannotEditOrDelete() throws Exception {
        UUID id = TestEquipment.create(mvc, objectMapper, member, gymId, "Bieżnia", "CARDIO", null);
        Map<String, Object> update = Map.of("name", "Bieżnia 2", "category", "CARDIO", "status", "ACTIVE", "version", 0);
        mvc.perform(put("/api/v1/equipment/{id}", id).header(HttpHeaders.AUTHORIZATION, outsider.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/equipment/{id}", id).header(HttpHeaders.AUTHORIZATION, outsider.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void softDeleteHidesEquipment() throws Exception {
        UUID id = TestEquipment.create(mvc, objectMapper, member, gymId, "Orbitrek", "CARDIO", null);
        mvc.perform(delete("/api/v1/equipment/{id}", id).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/equipment/{id}", id).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/gyms/{gymId}/equipment", gymId).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void uploadPhotoCreatesThumbnail() throws Exception {
        UUID id = TestEquipment.create(mvc, objectMapper, member, gymId, "Suwnica", "STRENGTH_MACHINE", null);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", png(800, 600));

        JsonNode body = body(mvc.perform(multipart("/api/v1/equipment/{id}/photo", id).file(file)
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl", startsWith("/api/v1/files/")))
                .andReturn());

        // Pliki publiczne (bez tokenu) – ładowane przez <img>.
        mvc.perform(get(body.get("photoUrl").asText()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        byte[] thumb = mvc.perform(get(body.get("thumbnailUrl").asText()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage thumbnail = ImageIO.read(new java.io.ByteArrayInputStream(thumb));
        org.assertj.core.api.Assertions.assertThat(thumbnail.getWidth()).isEqualTo(320);

        mvc.perform(get("/api/v1/equipment/{id}/history", id).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(jsonPath("$.content[0].changeType").value("PHOTO_CHANGED"));
    }

    @Test
    void uploadRejectsNonImage() throws Exception {
        UUID id = TestEquipment.create(mvc, objectMapper, member, gymId, "Drążek", "FUNCTIONAL", null);
        MockMultipartFile fake = new MockMultipartFile("file", "photo.png", "image/png", "not an image".getBytes());
        mvc.perform(multipart("/api/v1/equipment/{id}/photo", id).file(fake)
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("unsupported_file_type"));
    }

    @Test
    void reportsLifecycle() throws Exception {
        UUID original = TestEquipment.create(mvc, objectMapper, member, gymId, "Ławka płaska", "BENCH", null);
        UUID dup = TestEquipment.create(mvc, objectMapper, member, gymId, "Ławka prosta", "BENCH", null);

        Map<String, Object> report = Map.of("type", "DUPLICATE", "comment", "To ta sama ławka",
                "duplicateOfId", original);
        mvc.perform(post("/api/v1/equipment/{id}/reports", dup).header(HttpHeaders.AUTHORIZATION, outsider.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(report)))
                .andExpect(status().isForbidden());

        JsonNode created = body(mvc.perform(post("/api/v1/equipment/{id}/reports", dup)
                        .header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(report)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duplicateOf.name").value("Ławka płaska"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn());

        mvc.perform(post("/api/v1/equipment/{id}/reports", dup).header(HttpHeaders.AUTHORIZATION, member.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(report)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("report_already_open"));

        mvc.perform(get("/api/v1/equipment/{id}", dup).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(jsonPath("$.openReportCount").value(1));

        mvc.perform(post("/api/v1/reports/{id}/resolve", created.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedBy.id").value(member.id().toString()));

        mvc.perform(get("/api/v1/equipment/{id}/reports", dup).header(HttpHeaders.AUTHORIZATION, outsider.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void equipmentTypesAreSeeded() throws Exception {
        mvc.perform(get("/api/v1/equipment-types").header(HttpHeaders.AUTHORIZATION, member.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(37));
    }

    private static byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
