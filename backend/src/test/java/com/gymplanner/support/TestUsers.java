package com.gymplanner.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Pomocnik do rejestrowania użytkowników w testach integracyjnych. */
public final class TestUsers {

    public static final String PASSWORD = "Secret123";

    private TestUsers() {
    }

    public record Registered(UUID id, String email, String accessToken, String refreshCookie) {

        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    public static Registered register(MockMvc mvc, ObjectMapper mapper) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", email, "password", PASSWORD, "displayName", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        String cookie = result.getResponse().getCookie("gp_refresh").getValue();
        return new Registered(UUID.fromString(body.at("/user/id").asText()), email,
                body.get("accessToken").asText(), cookie);
    }
}
