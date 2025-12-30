package com.enterprise.common.util;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for distributed tracing operations.
 * <p>
 * Provides helper methods to extract trace ID and span ID from:
 * - Micrometer Tracing (current span)
 * - HTTP Request headers (X-B3-TraceId, X-B3-SpanId)
 * - MDC context
 * <p>
 * Usage:
 * <pre>
 * String traceId = TracingUtil.getCurrentTraceId();
 * String spanId = TracingUtil.getCurrentSpanId();
 *
 * ApiResponse&lt;User&gt; response = ApiResponse.success(user)
 *     .withTracing(traceId, spanId);
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@UtilityClass
public class TracingUtil {

    // Standard B3 propagation headers
    private static final String TRACE_ID_HEADER = "X-B3-TraceId";
    private static final String SPAN_ID_HEADER = "X-B3-SpanId";

    // Alternative headers
    private static final String TRACE_ID_HEADER_ALT = "X-Trace-Id";
    private static final String SPAN_ID_HEADER_ALT = "X-Span-Id";

    /**
     * Get current trace ID from Micrometer Tracer.
     *
     * @param tracer Micrometer Tracer instance
     * @return Trace ID or null if not available
     */
    public static String getCurrentTraceId(Tracer tracer) {
        if (tracer == null) {
            return null;
        }

        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                return currentSpan.context().traceId();
            }
        } catch (Exception e) {
            log.trace("Failed to get trace ID from tracer", e);
        }

        return null;
    }

    /**
     * Get current span ID from Micrometer Tracer.
     *
     * @param tracer Micrometer Tracer instance
     * @return Span ID or null if not available
     */
    public static String getCurrentSpanId(Tracer tracer) {
        if (tracer == null) {
            return null;
        }

        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                return currentSpan.context().spanId();
            }
        } catch (Exception e) {
            log.trace("Failed to get span ID from tracer", e);
        }

        return null;
    }

    /**
     * Get trace ID from current HTTP request headers.
     *
     * @return Trace ID from headers or null
     */
    public static String getTraceIdFromRequest() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        // Try standard B3 header first
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }

        // Try alternative header
        traceId = request.getHeader(TRACE_ID_HEADER_ALT);
        return (traceId != null && !traceId.isEmpty()) ? traceId : null;
    }

    /**
     * Get span ID from current HTTP request headers.
     *
     * @return Span ID from headers or null
     */
    public static String getSpanIdFromRequest() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        // Try standard B3 header first
        String spanId = request.getHeader(SPAN_ID_HEADER);
        if (spanId != null && !spanId.isEmpty()) {
            return spanId;
        }

        // Try alternative header
        spanId = request.getHeader(SPAN_ID_HEADER_ALT);
        return (spanId != null && !spanId.isEmpty()) ? spanId : null;
    }

    /**
     * Get current HTTP request from RequestContextHolder.
     *
     * @return Current HttpServletRequest or null
     */
    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.trace("Failed to get current request", e);
        }

        return null;
    }

    /**
     * Get trace ID with fallback strategy.
     * <p>
     * Tries in order:
     * 1. Micrometer Tracer current span
     * 2. HTTP request headers
     * 3. Returns null if not found
     *
     * @param tracer Micrometer Tracer instance (can be null)
     * @return Trace ID or null
     */
    public static String getTraceIdWithFallback(Tracer tracer) {
        // Try tracer first
        String traceId = getCurrentTraceId(tracer);
        if (traceId != null) {
            return traceId;
        }

        // Fallback to request headers
        return getTraceIdFromRequest();
    }

    /**
     * Get span ID with fallback strategy.
     * <p>
     * Tries in order:
     * 1. Micrometer Tracer current span
     * 2. HTTP request headers
     * 3. Returns null if not found
     *
     * @param tracer Micrometer Tracer instance (can be null)
     * @return Span ID or null
     */
    public static String getSpanIdWithFallback(Tracer tracer) {
        // Try tracer first
        String spanId = getCurrentSpanId(tracer);
        if (spanId != null) {
            return spanId;
        }

        // Fallback to request headers
        return getSpanIdFromRequest();
    }
}
