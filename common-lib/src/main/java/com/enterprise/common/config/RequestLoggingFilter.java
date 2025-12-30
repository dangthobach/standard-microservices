package com.enterprise.common.config;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Request/Response Logging Filter with Distributed Tracing
 * <p>
 * Logs all incoming HTTP requests and outgoing responses with:
 * - Request method, URI, headers
 * - Response status code
 * - Execution time
 * - Trace ID and Span ID for correlation
 * <p>
 * This filter is essential for:
 * - Debugging and troubleshooting
 * - Performance monitoring
 * - Audit trail
 * - Distributed tracing correlation
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestLoggingFilter implements Filter {

    private final Tracer tracer;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Wrap request and response for logging
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();

        try {
            // Log request
            logRequest(requestWrapper);

            // Process request
            chain.doFilter(requestWrapper, responseWrapper);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log response
            logResponse(responseWrapper, duration);

            // Copy response body to actual response
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * Log incoming HTTP request with tracing context.
     */
    private void logRequest(ContentCachingRequestWrapper request) {
        String traceId = getTraceId();
        String spanId = getSpanId();

        String queryString = request.getQueryString();
        String requestUri = queryString != null ?
                request.getRequestURI() + "?" + queryString :
                request.getRequestURI();

        log.info("→ Incoming Request | Method: {} | URI: {} | TraceId: {} | SpanId: {} | RemoteAddr: {}",
                request.getMethod(),
                requestUri,
                traceId,
                spanId,
                request.getRemoteAddr()
        );

        // Log headers in debug mode
        if (log.isDebugEnabled()) {
            logHeaders(request);
        }
    }

    /**
     * Log outgoing HTTP response with tracing context.
     */
    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        String traceId = getTraceId();
        String spanId = getSpanId();

        int status = response.getStatus();
        String statusCategory = getStatusCategory(status);

        log.info("← Outgoing Response | Status: {} ({}) | Duration: {}ms | TraceId: {} | SpanId: {}",
                status,
                statusCategory,
                duration,
                traceId,
                spanId
        );

        // Log response body for errors (in debug mode)
        if (log.isDebugEnabled() && status >= 400) {
            logResponseBody(response);
        }
    }

    /**
     * Log request headers.
     */
    private void logHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder("Request Headers: ");

        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            String headerValue = request.getHeader(headerName);
            // Mask sensitive headers
            if (isSensitiveHeader(headerName)) {
                headerValue = "***MASKED***";
            }
            headers.append(headerName).append("=").append(headerValue).append("; ");
        });

        log.debug(headers.toString());
    }

    /**
     * Log response body for debugging.
     */
    private void logResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            // Truncate if too long
            if (body.length() > 1000) {
                body = body.substring(0, 1000) + "... (truncated)";
            }
            log.debug("Response Body: {}", body);
        }
    }

    /**
     * Check if header is sensitive (should be masked in logs).
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.contains("authorization") ||
               lowerName.contains("cookie") ||
               lowerName.contains("token") ||
               lowerName.contains("password") ||
               lowerName.contains("secret");
    }

    /**
     * Get HTTP status category description.
     */
    private String getStatusCategory(int status) {
        if (status >= 200 && status < 300) return "Success";
        if (status >= 300 && status < 400) return "Redirect";
        if (status >= 400 && status < 500) return "Client Error";
        if (status >= 500) return "Server Error";
        return "Unknown";
    }

    /**
     * Get current trace ID from tracer.
     */
    private String getTraceId() {
        try {
            if (tracer.currentSpan() != null) {
                return tracer.currentSpan().context().traceId();
            }
        } catch (Exception e) {
            log.trace("Failed to get trace ID", e);
        }
        return "N/A";
    }

    /**
     * Get current span ID from tracer.
     */
    private String getSpanId() {
        try {
            if (tracer.currentSpan() != null) {
                return tracer.currentSpan().context().spanId();
            }
        } catch (Exception e) {
            log.trace("Failed to get span ID", e);
        }
        return "N/A";
    }
}
