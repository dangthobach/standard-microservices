package com.enterprise.common.config;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.dto.ErrorDetails;
import com.enterprise.common.exception.BusinessException;
import com.enterprise.common.util.TracingUtil;
import io.micrometer.tracing.Tracer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 * <p>
 * Centralized exception handling for all microservices.
 * Converts exceptions to standardized ApiResponse format with:
 * - Appropriate HTTP status codes
 * - Error codes and messages
 * - Distributed tracing information
 * - Validation error details
 * <p>
 * Supported exceptions:
 * - BusinessException (custom business logic errors)
 * - Validation errors (@Valid, @Validated)
 * - Authentication/Authorization errors
 * - Generic exceptions
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Tracer tracer;

    /**
     * Handle BusinessException (custom business logic errors).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessException ex
    ) {
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
                        TracingUtil.getSpanIdWithFallback(tracer)
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle validation errors from @Valid annotation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
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
                errorDetails
        ).withTracing(
                TracingUtil.getTraceIdWithFallback(tracer),
                TracingUtil.getSpanIdWithFallback(tracer)
        );

        response.setData(errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        log.warn("Constraint violation exception: {}", ex.getMessage());

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        ErrorDetails errorDetails = ErrorDetails.builder()
                .code("CONSTRAINT_VIOLATION")
                .detail("Constraint violation")
                .metadata(Map.of("errors", errors))
                .build();

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>error(
                "Constraint violation",
                errorDetails
        ).withTracing(
                TracingUtil.getTraceIdWithFallback(tracer),
                TracingUtil.getSpanIdWithFallback(tracer)
        );

        response.setData(errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle authentication exceptions.
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            Exception ex
    ) {
        log.warn("Authentication exception: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                "Authentication failed",
                "AUTHENTICATION_FAILED"
        ).withTracing(
                TracingUtil.getTraceIdWithFallback(tracer),
                TracingUtil.getSpanIdWithFallback(tracer)
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    /**
     * Handle access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex
    ) {
        log.warn("Access denied: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                "Access denied",
                "ACCESS_DENIED"
        ).withTracing(
                TracingUtil.getTraceIdWithFallback(tracer),
                TracingUtil.getSpanIdWithFallback(tracer)
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex
    ) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                "ILLEGAL_ARGUMENT"
        ).withTracing(
                TracingUtil.getTraceIdWithFallback(tracer),
                TracingUtil.getSpanIdWithFallback(tracer)
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle method argument type mismatch.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        String typeName = ex.getRequiredType() != null ?
                ex.getRequiredType().getSimpleName() : "unknown";

        String error = String.format("Parameter '%s' should be of type %s",
                ex.getName(), typeName);

        log.warn("Type mismatch: {}", error);

        ApiResponse<Object> response = ApiResponse.error(
                error,
                "TYPE_MISMATCH"
        ).withTracing(
                TracingUtil.getTraceIdWithFallback(tracer),
                TracingUtil.getSpanIdWithFallback(tracer)
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex
    ) {
        log.error("Unexpected exception", ex);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .code("INTERNAL_SERVER_ERROR")
                .detail("An unexpected error occurred")
                .build();

        ApiResponse<Object> response = ApiResponse.error(
                "An unexpected error occurred",
                errorDetails
        ).withTracing(
                TracingUtil.getTraceIdWithFallback(tracer),
                TracingUtil.getSpanIdWithFallback(tracer)
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
