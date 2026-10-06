package com.gymplanner.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
        ProblemDetail problem = ProblemDetails.of(ex.getStatus(), ex.getCode(), ex.getMessage());
        ex.getProperties().forEach(problem::setProperty);
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorDto> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorDto(v.getPropertyPath().toString(), "invalid", v.getMessage()))
                .toList();
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "validation_failed", "Validation failed");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ProblemDetails.of(HttpStatus.CONFLICT,
                "concurrent_modification", "The resource was modified by someone else. Reload and try again."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ProblemDetails.of(HttpStatus.CONFLICT, "data_conflict", "Data conflict"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ProblemDetails.of(HttpStatus.FORBIDDEN, "forbidden", "Access denied"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError().body(ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR,
                "internal_error", "Unexpected server error"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        List<FieldErrorDto> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(), fe.getCode(), fe.getDefaultMessage()))
                .toList();
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "validation_failed", "Validation failed");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode, @NonNull WebRequest request) {
        // Ujednolicamy błędy frameworka (np. 400 przy złym JSON, 405) – zawsze z polem "code".
        if (body instanceof ProblemDetail problem && problem.getProperties() == null) {
            HttpStatus resolved = HttpStatus.resolve(statusCode.value());
            String code = resolved != null ? resolved.name().toLowerCase() : "error";
            problem.setProperty(ProblemDetails.CODE, code);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }
}
