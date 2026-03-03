package com.enterprise.common.config.reactive;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.dto.ErrorDetails;
import com.enterprise.common.exception.BusinessException;
import com.enterprise.common.exception.DuplicateResourceException;
import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.common.exception.StateTransitionException;
import com.enterprise.common.util.TracingUtil;
import io.micrometer.tracing.Tracer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for WebFlux environments
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ReactiveGlobalExceptionHandler {

    private final Tracer tracer;

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        Map<String, Object> metadata = new HashMap<>();
        if (ex.getMetadata() != null) {
            metadata.put("details", ex.getMetadata());
        }

        ErrorDetails errorDetails = ErrorDetails.builder()
                .code(ex.getErrorCode())
                .detail(ex.getMessage())
                .metadata(metadata)
                .build();

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), errorDetails)
                .withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> handleValidationException(
            WebExchangeBindException ex) {
        log.warn("Validation exception: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorDetails errorDetails = ErrorDetails.builder()
                .code("VALIDATION_ERROR")
                .detail("Validation failed")
                .metadata(Map.of("errors", errors))
                .build();

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>error(
                "Validation failed",
                errorDetails).withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        response.setData(errors);

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleServerWebInputException(ServerWebInputException ex) {
        log.warn("Type mismatch or invalid input: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                ex.getReason() != null ? ex.getReason() : "Invalid request input",
                "INVALID_INPUT").withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        log.warn("Constraint violation exception: {}", ex.getMessage());

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing));

        ErrorDetails errorDetails = ErrorDetails.builder()
                .code("CONSTRAINT_VIOLATION")
                .detail("Constraint violation")
                .metadata(Map.of("errors", errors))
                .build();

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>error(
                "Constraint violation",
                errorDetails).withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        response.setData(errors);

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleAuthenticationException(Exception ex) {
        log.warn("Authentication exception: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                "Authentication failed",
                "AUTHENTICATION_FAILED").withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                "Access denied",
                "ACCESS_DENIED").withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(response));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                "ILLEGAL_ARGUMENT").withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .code("RESOURCE_NOT_FOUND")
                .detail(ex.getMessage())
                .metadata(Map.of(
                        "resourceType", ex.getResourceType(),
                        "field", ex.getFieldName(),
                        "value", String.valueOf(ex.getFieldValue())))
                .build();

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), errorDetails)
                .withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .code("DUPLICATE_RESOURCE")
                .detail(ex.getMessage())
                .metadata(Map.of(
                        "resourceType", ex.getResourceType(),
                        "field", ex.getFieldName(),
                        "value", String.valueOf(ex.getFieldValue())))
                .build();

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), errorDetails)
                .withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(response));
    }

    @ExceptionHandler(StateTransitionException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleStateTransitionException(StateTransitionException ex) {
        log.warn("Invalid state transition: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .code("INVALID_STATE_TRANSITION")
                .detail(ex.getMessage())
                .metadata(Map.of(
                        "entityType", ex.getEntityType(),
                        "currentState", ex.getCurrentState(),
                        "targetState", ex.getTargetState()))
                .build();

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), errorDetails)
                .withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                "ILLEGAL_STATE").withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(response));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleGlobalException(Exception ex) {
        log.error("Unexpected exception", ex);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .code("INTERNAL_SERVER_ERROR")
                .detail("An unexpected error occurred")
                .build();

        ApiResponse<Object> response = ApiResponse.error(
                "An unexpected error occurred",
                errorDetails).withTracing(
                        TracingUtil.getTraceIdWithFallback(tracer),
                        TracingUtil.getSpanIdWithFallback(tracer));

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }
}
