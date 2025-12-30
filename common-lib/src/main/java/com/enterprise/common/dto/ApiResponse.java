package com.enterprise.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standardized API Response Wrapper for all microservices
 * <p>
 * Features:
 * - Consistent response structure across all services
 * - Distributed tracing support (traceId, spanId)
 * - Error details with error codes
 * - Timestamp for audit and debugging
 * - Factory methods for common use cases
 * <p>
 * Usage Examples:
 * <pre>
 * // Success with data
 * return ApiResponse.success(userData);
 *
 * // Success with message
 * return ApiResponse.success("User created successfully", userData);
 *
 * // Error with code
 * return ApiResponse.error("User not found", "USER_NOT_FOUND");
 *
 * // Error with details
 * return ApiResponse.error("Validation failed", "VALIDATION_ERROR", validationErrors);
 * </pre>
 *
 * @param <T> Type of the response data
 * @author Enterprise Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Indicates if the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response message or error description", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response data payload")
    private T data;

    @Schema(description = "Error code for failed requests", example = "USER_NOT_FOUND")
    private String errorCode;

    @Schema(description = "Detailed error information for failed requests")
    private ErrorDetails error;

    @Schema(description = "Distributed trace ID for request correlation", example = "5f9c8a7b6d4e3f2a1b0c9d8e")
    private String traceId;

    @Schema(description = "Distributed span ID for request tracing", example = "1a2b3c4d5e6f7g8h")
    private String spanId;

    @Schema(description = "Response timestamp", example = "2025-12-31T00:00:00Z")
    @Builder.Default
    private Instant timestamp = Instant.now();

    // ========== Success Response Factory Methods ==========

    /**
     * Create a success response with data.
     *
     * @param data Response data
     * @return ApiResponse with success=true and data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a success response with custom message and data.
     *
     * @param message Success message
     * @param data    Response data
     * @return ApiResponse with success=true, message, and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a success response with only message (no data).
     *
     * @param message Success message
     * @return ApiResponse with success=true and message
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    // ========== Error Response Factory Methods ==========

    /**
     * Create an error response with message only.
     *
     * @param message Error message
     * @return ApiResponse with success=false and message
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response with message and error code.
     *
     * @param message   Error message
     * @param errorCode Error code
     * @return ApiResponse with success=false, message, and errorCode
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response with message, error code, and additional data.
     *
     * @param message   Error message
     * @param errorCode Error code
     * @param data      Additional error data (e.g., validation errors)
     * @return ApiResponse with success=false, message, errorCode, and data
     */
    public static <T> ApiResponse<T> error(String message, String errorCode, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response with message and ErrorDetails.
     *
     * @param message Error message
     * @param error   Detailed error information
     * @return ApiResponse with success=false, message, and error details
     */
    public static <T> ApiResponse<T> error(String message, ErrorDetails error) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .errorCode(error != null ? error.getCode() : null)
                .timestamp(Instant.now())
                .build();
    }

    // ========== Tracing Methods ==========

    /**
     * Add tracing information to the response.
     *
     * @param traceId Trace ID
     * @param spanId  Span ID
     * @return This ApiResponse with tracing info
     */
    public ApiResponse<T> withTracing(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
        return this;
    }

    /**
     * Add trace ID to the response.
     *
     * @param traceId Trace ID
     * @return This ApiResponse with trace ID
     */
    public ApiResponse<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
}
