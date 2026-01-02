package com.enterprise.gateway.feign.auth;

import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * Feign interceptor for API Key authentication.
 * <p>
 * Adds a custom header containing an API key to all outgoing requests.
 * <p>
 * Usage Example:
 * <pre>
 * {@code @Configuration}
 * public class ExternalServiceFeignConfig {
 *
 *     {@code @Bean}
 *     public AuthenticationInterceptor apiKeyInterceptor(
 *         {@code @Value("${external.service.api-key}")} String apiKey
 *     ) {
 *         return new ApiKeyInterceptor("X-API-Key", apiKey);
 *     }
 * }
 *
 * {@code @FeignClient(
 *     name = "external-service",
 *     url = "${external.service.url}",
 *     configuration = ExternalServiceFeignConfig.class
 * )}
 * public interface ExternalServiceClient {
 *     {@code @GetMapping("/api/data")}
 *     DataResponse getData();
 * }
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
public class ApiKeyInterceptor implements AuthenticationInterceptor {

    private final String headerName;
    private final String apiKey;

    /**
     * Creates an API Key interceptor with custom header name.
     *
     * @param headerName the HTTP header name (e.g., "X-API-Key", "Authorization", "API-Token")
     * @param apiKey     the API key value
     * @throws IllegalArgumentException if headerName or apiKey is null/empty
     */
    public ApiKeyInterceptor(String headerName, String apiKey) {
        Assert.hasText(headerName, "Header name must not be null or empty");
        Assert.hasText(apiKey, "API key must not be null or empty");

        this.headerName = headerName;
        this.apiKey = apiKey;

        log.debug("ApiKeyInterceptor initialized with header: {}", headerName);
    }

    /**
     * Creates an API Key interceptor with default header name "X-API-Key".
     *
     * @param apiKey the API key value
     * @throws IllegalArgumentException if apiKey is null/empty
     */
    public ApiKeyInterceptor(String apiKey) {
        this("X-API-Key", apiKey);
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(headerName, apiKey);
        log.trace("Added API Key header '{}' to request: {} {}",
                  headerName, template.method(), template.url());
    }

    @Override
    public String getAuthenticationType() {
        return "API_KEY";
    }
}
