package com.gymplanner.common.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Bazowy wyjątek domenowy mapowany na odpowiedź ProblemDetail (RFC 7807).
 * {@code code} jest stabilnym identyfikatorem błędu, którego frontend używa do tłumaczeń.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> properties;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public ApiException(HttpStatus status, String code, String message, Map<String, Object> properties) {
        super(message);
        this.status = status;
        this.code = code;
        this.properties = properties;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
