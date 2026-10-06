package com.gymplanner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymplanner.support.AbstractIntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Zapisuje specyfikację OpenAPI do target/openapi.json – źródło do generowania typów TS na froncie
 * ({@code npm run gen:api}) bez uruchamiania backendu.
 */
class OpenApiSpecExportTest extends AbstractIntegrationTest {

    @Test
    void exportSpec() throws Exception {
        String spec = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(spec);
        Path target = Path.of("target", "openapi.json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
    }
}
