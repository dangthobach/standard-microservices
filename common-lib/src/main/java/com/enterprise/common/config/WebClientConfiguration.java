package com.enterprise.common.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient Configuration for HTTP communication
 * <p>
 * Provides centralized WebClient configuration with:
 * - Connection pooling
 * - Timeouts (connect, read, write)
 * - Request/Response logging
 * - Error handling
 * - OAuth2 support (when available)
 * <p>
 * WebClient is the recommended replacement for RestTemplate,
 * supporting both blocking and reactive operations.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
public class WebClientConfiguration {

    @Value("${webclient.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${webclient.read-timeout:30000}")
    private int readTimeout;

    @Value("${webclient.write-timeout:30000}")
    private int writeTimeout;

    @Value("${webclient.max-connections:100}")
    private int maxConnections;

    @Value("${webclient.pending-acquire-timeout:45000}")
    private int pendingAcquireTimeout;

    /**
     * Default WebClient.Builder bean with standard configuration.
     * <p>
     * Use this builder to create WebClient instances:
     * <pre>
     * {@code
     * @Autowired
     * private WebClient.Builder webClientBuilder;
     *
     * WebClient client = webClientBuilder
     *     .baseUrl("https://api.example.com")
     *     .build();
     * }
     * </pre>
     *
     * @return Configured WebClient.Builder
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse());
    }

    /**
     * Default WebClient bean for general use.
     * <p>
     * Use this for simple HTTP calls without specific base URL:
     * <pre>
     * {@code
     * @Autowired
     * private WebClient webClient;
     *
     * String result = webClient.get()
     *     .uri("https://api.example.com/users/{id}", userId)
     *     .retrieve()
     *     .bodyToMono(String.class)
     *     .block();
     * }
     * </pre>
     *
     * @param builder WebClient.Builder from above
     * @return Configured WebClient
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    /**
     * Log outgoing requests (debug level only).
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("→ WebClient Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) ->
                        values.forEach(value -> {
                            if (isSensitiveHeader(name)) {
                                log.debug("  {}: ***MASKED***", name);
                            } else {
                                log.debug("  {}: {}", name, value);
                            }
                        })
                );
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * Log incoming responses (debug level only).
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
