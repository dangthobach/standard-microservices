package com.enterprise.gateway.filter;

import brave.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global Filter to start distributed tracing
 * Injects TraceId into request headers and MDC for logging
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceIdFilter implements GlobalFilter, Ordered {

    private final Tracer tracer;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = getOrCreateTraceId();

        // Add traceId to request headers for downstream services
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-Trace-Id", traceId)
                .build();

        // Add traceId to response headers for clients
        exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);

        log.debug("Request {} {} with traceId: {}",
                request.getMethod(),
                request.getPath(),
                traceId);

        return chain.filter(exchange.mutate().request(request).build());
    }

    private String getOrCreateTraceId() {
        var currentSpan = tracer.currentSpan();
        if (currentSpan != null && currentSpan.context() != null) {
            return currentSpan.context().traceIdString();
        }
        return java.util.UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
