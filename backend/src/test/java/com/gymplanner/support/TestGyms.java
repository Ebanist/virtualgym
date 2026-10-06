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

/** Tworzenie siłowni w testach – unikalne miasto, więc bez ostrzeżeń o duplikatach. */
public final class TestGyms {

    private TestGyms() {
    }

    public static UUID create(MockMvc mvc, ObjectMapper mapper, TestUsers.Registered owner) throws Exception {
        return create(mvc, mapper, owner, "Siłownia " + UUID.randomUUID(), "Miasto-" + UUID.randomUUID());
    }

    public static UUID create(MockMvc mvc, ObjectMapper mapper, TestUsers.Registered owner, String name,
            String city) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("city", city);
        body.put("address", "ul. Testowa 1");
        body.put("confirmDuplicate", true);
        String response = mvc.perform(post("/api/v1/gyms")
                        .header(HttpHeaders.AUTHORIZATION, owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(response).get("id").asText());
    }

    public static void join(MockMvc mvc, TestUsers.Registered user, UUID gymId) throws Exception {
        mvc.perform(post("/api/v1/gyms/{id}/membership", gymId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isNoContent());
    }
}
