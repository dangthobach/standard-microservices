package com.enterprise.common.config;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.util.TracingUtil;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Global Response Body Advice
 * <p>
 * Automatically enriches all ApiResponse objects with distributed tracing information
 * (trace ID and span ID) before sending to the client.
 * <p>
 * Features:
 * - Automatically adds traceId and spanId to ApiResponse
 * - Works with Micrometer Tracing
 * - No need to manually add tracing in every controller
 * <p>
 * This advice only modifies ApiResponse objects. Other response types are unchanged.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final Tracer tracer;

    /**
     * Check if this advice should be applied to the response.
     * <p>
     * Only applies to ApiResponse objects.
     *
     * @param returnType    Method return type
     * @param converterType Message converter type
     * @return true if response is ApiResponse
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> returnClass = returnType.getParameterType();
        return ApiResponse.class.isAssignableFrom(returnClass);
    }

    /**
     * Enrich the response body with tracing information.
     *
     * @param body                  Response body
     * @param returnType            Method return type
     * @param selectedContentType   Selected content type
     * @param selectedConverterType Selected converter type
     * @param request               Server HTTP request
     * @param response              Server HTTP response
     * @return Enriched response body
     */
    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (body instanceof ApiResponse<?> apiResponse) {
            enrichWithTracing(apiResponse);
        }

        return body;
    }

    /**
     * Add tracing information to ApiResponse.
     *
     * @param apiResponse ApiResponse to enrich
     */
    private void enrichWithTracing(ApiResponse<?> apiResponse) {
        try {
            // Get traceId and spanId from current span
            String traceId = TracingUtil.getTraceIdWithFallback(tracer);
            String spanId = TracingUtil.getSpanIdWithFallback(tracer);

            // Only set if not already set by controller
            if (apiResponse.getTraceId() == null && traceId != null) {
                apiResponse.setTraceId(traceId);
            }

            if (apiResponse.getSpanId() == null && spanId != null) {
                apiResponse.setSpanId(spanId);
            }

            log.trace("Enriched response with tracing: traceId={}, spanId={}", traceId, spanId);

        } catch (Exception e) {
            log.warn("Failed to enrich response with tracing information", e);
            // Don't fail the request if tracing fails
        }
    }
}
