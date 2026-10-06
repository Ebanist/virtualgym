package com.gymplanner.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "GymPlanner API", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    /**
     * Pola schematów odpowiedzi (*Dto, *Response) są domyślnie wymagane – typy TS na froncie nie mają wtedy
     * zbędnych {@code | undefined}. Pole opcjonalne oznaczamy {@code @Schema(nullable = true)}; jest wtedy
     * pomijane w JSON-ie, gdy ma wartość null (Jackson: non_null), więc w TS staje się {@code field?: T}.
     */
    @Bean
    @SuppressWarnings("unchecked")
    OpenApiCustomizer requiredResponseFieldsCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().forEach((name, schema) -> {
                if (!(name.endsWith("Dto") || name.endsWith("Response")) || schema.getProperties() == null) {
                    return;
                }
                List<String> required = new ArrayList<>();
                for (Map.Entry<String, Schema> property : ((Map<String, Schema>) schema.getProperties()).entrySet()) {
                    if (Boolean.TRUE.equals(property.getValue().getNullable())) {
                        property.getValue().setNullable(null);
                    } else {
                        required.add(property.getKey());
                    }
                }
                schema.setRequired(required);
            });
        };
    }
}
