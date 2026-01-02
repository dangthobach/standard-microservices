package com.enterprise.gateway.feign.error;

import com.enterprise.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Global Feign Error Decoder for unified error handling.
 * <p>
 * Converts HTTP 4xx and 5xx responses into appropriate exceptions:
 * - 400 Bad Request -> BusinessException with BAD_REQUEST
 * - 401 Unauthorized -> FeignUnauthorizedException
 * - 403 Forbidden -> FeignForbiddenException
 * - 404 Not Found -> FeignNotFoundException
 * - 409 Conflict -> BusinessException with CONFLICT
 * - 422 Unprocessable Entity -> BusinessException with VALIDATION_ERROR
 * - 429 Too Many Requests -> FeignRateLimitException
 * - 5xx Server Error -> FeignServerException
 * - Other -> Default Feign exception
 * <p>
 * This decoder attempts to parse error response bodies to extract meaningful error messages.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
public class GlobalFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String reasonPhrase = response.reason();
        String errorMessage = extractErrorMessage(response);

        log.error("Feign client error: {} {} - Status: {}, Message: {}",
                  methodKey, response.request().url(), status, errorMessage);

        return switch (status) {
            case 400 -> new BusinessException("BAD_REQUEST",
                    errorMessage != null ? errorMessage : "Bad request to downstream service");

            case 401 -> new FeignUnauthorizedException(
                    errorMessage != null ? errorMessage : "Unauthorized access to downstream service");

            case 403 -> new FeignForbiddenException(
                    errorMessage != null ? errorMessage : "Forbidden access to downstream service");

            case 404 -> new FeignNotFoundException(
                    errorMessage != null ? errorMessage : "Resource not found in downstream service");

            case 409 -> new BusinessException("CONFLICT",
                    errorMessage != null ? errorMessage : "Conflict in downstream service");

            case 422 -> new BusinessException("VALIDATION_ERROR",
                    errorMessage != null ? errorMessage : "Validation error in downstream service");

            case 429 -> new FeignRateLimitException(
                    errorMessage != null ? errorMessage : "Rate limit exceeded in downstream service");

            case 500, 501, 502, 503, 504 -> new FeignServerException(status,
                    errorMessage != null ? errorMessage : "Server error in downstream service: " + reasonPhrase);

            default -> {
                if (status >= 400 && status < 500) {
                    yield new FeignClientException(status,
                            errorMessage != null ? errorMessage : "Client error: " + reasonPhrase);
                }
                // Fallback to default Feign error handling for other cases
                yield defaultDecoder.decode(methodKey, response);
            }
        };
    }

    /**
     * Attempts to extract a meaningful error message from the response body.
     * <p>
     * Supports:
     * - JSON responses with "message", "error", "errorMessage", or "detail" fields
     * - Plain text responses
     *
     * @param response the Feign response
     * @return extracted error message, or null if unable to extract
     */
    private String extractErrorMessage(Response response) {
        if (response.body() == null) {
            return null;
        }

        try (InputStream bodyStream = response.body().asInputStream()) {
            String body = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);

            if (body.isEmpty()) {
                return null;
            }

            // Try to parse as JSON
            if (isJsonResponse(response)) {
                return extractJsonErrorMessage(body);
            }

            // Return plain text (truncate if too long)
            return body.length() > 500 ? body.substring(0, 500) + "..." : body;

        } catch (IOException e) {
            log.warn("Failed to read error response body", e);
            return null;
        }
    }

    /**
     * Checks if the response is JSON based on Content-Type header.
     */
    private boolean isJsonResponse(Response response) {
        return response.headers().containsKey("Content-Type") &&
               response.headers().get("Content-Type").stream()
                       .anyMatch(type -> type.contains("application/json"));
    }

    /**
     * Extracts error message from JSON response.
     * <p>
     * Checks common error message fields: message, error, errorMessage, detail.
     */
    private String extractJsonErrorMessage(String jsonBody) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);

            // Try common error message fields
            for (String field : new String[]{"message", "error", "errorMessage", "detail", "title"}) {
                if (root.has(field)) {
                    JsonNode node = root.get(field);
                    if (node.isTextual()) {
                        return node.asText();
                    }
                }
            }

            // If no standard field found, return the whole JSON (truncated)
            String jsonStr = root.toString();
            return jsonStr.length() > 500 ? jsonStr.substring(0, 500) + "..." : jsonStr;

        } catch (IOException e) {
            log.debug("Failed to parse JSON error response, returning raw body", e);
            return jsonBody.length() > 500 ? jsonBody.substring(0, 500) + "..." : jsonBody;
        }
    }
}
