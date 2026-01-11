package com.enterprise.common.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient Configuration for HTTP communication
 * <p>
 * Provides centralized WebClient configuration with:
 * - Connection pooling (configurable via WebClientProperties)
 * - Timeouts (connect, read, write)
 * - Request/Response logging (DEBUG level)
 * - Error handling
 * <p>
 * This configuration uses {@code @ConditionalOnMissingBean} to allow
 * services to override these beans when needed (e.g., Gateway with
 * LoadBalanced).
 * <p>
 * Architecture:
 * 
 * <pre>
 * WebClientProperties (externalized config)
 *         ↓
 * HttpClient Bean (shared: timeout, pool, etc.)
 *         ↓
 * WebClient.Builder (@ConditionalOnMissingBean)
 *         ↓
 * WebClient (@ConditionalOnMissingBean)
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 * @see WebClientProperties
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
@EnableConfigurationProperties(WebClientProperties.class)
public class WebClientConfiguration {

    private final WebClientProperties properties;

    /**
     * Shared HttpClient bean with connection pooling and timeouts.
     * <p>
     * This bean is designed to be reused by other configurations.
     * Gateway-service can inject this bean and add LoadBalancer filter.
     * <p>
     * Features:
     * - Connection pooling with configurable max connections
     * - Connect, read, write timeouts
     * - Pending acquire timeout for pool exhaustion
     *
     * @return Configured HttpClient for reactive HTTP calls
     */
    @Bean
    @ConditionalOnMissingBean(HttpClient.class)
    public HttpClient baseHttpClient() {
        log.info("✅ Configuring shared HttpClient with timeouts: connect={}ms, read={}ms, write={}ms",
                properties.getConnectTimeout(),
                properties.getReadTimeout(),
                properties.getWriteTimeout());

        // Configure connection pool
        ConnectionProvider connectionProvider = ConnectionProvider.builder("webclient-pool")
                .maxConnections(properties.getMaxConnections())
                .pendingAcquireTimeout(Duration.ofMillis(properties.getPendingAcquireTimeout()))
                .build();

        return HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(properties.getReadTimeout()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeout(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(properties.getWriteTimeout(), TimeUnit.MILLISECONDS)));
    }

    /**
     * Default WebClient.Builder bean with standard configuration.
     * <p>
     * This bean uses {@code @ConditionalOnMissingBean} to allow services
     * to override it when needed. For example, Gateway-service provides
     * its own LoadBalanced WebClient.Builder.
     * <p>
     * Usage:
     * 
     * <pre>
     * {@code
     * @Autowired
     * private WebClient.Builder webClientBuilder;
     *
     * WebClient client = webClientBuilder
     *         .baseUrl("https://api.example.com")
     *         .build();
     * }
     * </pre>
     *
     * @param httpClient Shared HttpClient with timeouts and pooling
     * @return Configured WebClient.Builder
     */
    @Bean
    @ConditionalOnMissingBean(WebClient.Builder.class)
    public WebClient.Builder webClientBuilder(HttpClient httpClient) {
        log.info("✅ Configuring default WebClient.Builder with shared HttpClient");

        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(properties.getMaxInMemorySize()));

        // Add logging filters if enabled
        if (properties.isLoggingEnabled()) {
            builder.filter(logRequest())
                    .filter(logResponse());
        }

        return builder;
    }

    /**
     * Default WebClient bean for general use.
     * <p>
     * This bean uses {@code @ConditionalOnMissingBean} to allow services
     * to provide their own WebClient instance.
     * <p>
     * Usage:
     * 
     * <pre>
     * {@code
     * @Autowired
     * private WebClient webClient;
     *
     * String result = webClient.get()
     *         .uri("https://api.example.com/users/{id}", userId)
     *         .retrieve()
     *         .bodyToMono(String.class)
     *         .block();
     * }
     * </pre>
     *
     * @param builder WebClient.Builder from above
     * @return Configured WebClient
     */
    @Bean
    @ConditionalOnMissingBean(WebClient.class)
    public WebClient webClient(WebClient.Builder builder) {
        log.info("✅ Creating default WebClient instance");
        return builder.build();
    }

    /**
     * Log outgoing requests (DEBUG level only).
     * Masks sensitive headers like Authorization, Cookie, etc.
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("→ WebClient Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) -> values.forEach(value -> {
                    if (isSensitiveHeader(name)) {
                        log.debug("  {}: ***MASKED***", name);
                    } else {
                        log.debug("  {}: {}", name, value);
                    }
                }));
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * Log incoming responses (DEBUG level only).
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("← WebClient Response: Status {}", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }

    /**
     * Check if header contains sensitive information.
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.contains("authorization") ||
                lowerName.contains("cookie") ||
                lowerName.contains("token") ||
                lowerName.contains("password") ||
                lowerName.contains("secret") ||
                lowerName.contains("api-key");
    }
}
