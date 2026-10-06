package com.gymplanner.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public final class TestEquipment {

    private TestEquipment() {
    }

    public static UUID create(MockMvc mvc, ObjectMapper mapper, TestUsers.Registered user, UUID gymId, String name,
            String category, UUID typeId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("category", category);
        if (typeId != null) {
            body.put("equipmentTypeId", typeId);
        }
        String response = mvc.perform(post("/api/v1/gyms/{gymId}/equipment", gymId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(response).get("id").asText());
    }
}
