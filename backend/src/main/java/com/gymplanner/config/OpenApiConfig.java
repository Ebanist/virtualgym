package com.gymplanner.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "GymPlanner API", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    private static final String BASE_PACKAGE = "com.gymplanner";

    /**
     * Pola schematów odpowiedzi (*Dto, *Response) są domyślnie wymagane – typy TS na froncie nie mają wtedy
     * zbędnych {@code | undefined}. Pole opcjonalne oznaczamy w rekordzie {@code @Schema(nullable = true)};
     * przy wartości null jest pomijane w JSON-ie (Jackson: non_null), więc w TS staje się {@code field?: T}.
     * Informację bierzemy z adnotacji rekordu, bo springdoc przenosi „nullable” z pola-referencji na cały
     * schemat komponentu.
     */
    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    OpenApiCustomizer requiredResponseFieldsCustomizer() {
        Map<String, Set<String>> optionalFields = scanOptionalRecordFields();
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            Map<String, io.swagger.v3.oas.models.media.Schema> schemas = openApi.getComponents().getSchemas();
            schemas.values().forEach(schema -> {
                schema.setNullable(null);
                if (schema.getProperties() != null) {
                    ((Map<String, io.swagger.v3.oas.models.media.Schema>) schema.getProperties()).values()
                            .forEach(p -> p.setNullable(null));
                }
            });
            schemas.forEach((name, schema) -> {
                if (!(name.endsWith("Dto") || name.endsWith("Response")) || schema.getProperties() == null) {
                    return;
                }
                Set<String> optional = optionalFields.getOrDefault(name, Set.of());
                List<String> required = new ArrayList<>(((Map<String, ?>) schema.getProperties()).keySet());
                required.removeAll(optional);
                schema.setRequired(required);
            });
        };
    }

    /** Nazwa rekordu → pola oznaczone {@code @Schema(nullable = true)}. */
    private static Map<String, Set<String>> scanOptionalRecordFields() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Record.class));
        Map<String, Set<String>> result = new HashMap<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
            Class<?> type = ClassUtils.resolveClassName(candidate.getBeanClassName(), OpenApiConfig.class.getClassLoader());
            RecordComponent[] components = type.getRecordComponents();
            if (components == null) {
                continue;
            }
            Set<String> optional = java.util.Arrays.stream(components)
                    .filter(c -> {
                        // @Schema nie ma celu RECORD_COMPONENT – kompilator przenosi ją na akcesor/pole.
                        Schema schema = c.getAccessor().getAnnotation(Schema.class);
                        return schema != null && schema.nullable();
                    })
                    .map(RecordComponent::getName)
                    .collect(Collectors.toSet());
            result.put(type.getSimpleName(), optional);
        }
        return result;
    }
}
