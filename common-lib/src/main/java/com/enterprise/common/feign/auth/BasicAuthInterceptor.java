package com.enterprise.common.feign.auth;

import feign.RequestTemplate;
import feign.auth.BasicAuthRequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Feign interceptor for HTTP Basic Authentication.
 * <p>
 * Adds an "Authorization: Basic {base64(username:password)}" header to all outgoing requests.
 * <p>
 * Usage Example:
 * <pre>
 * {@code @Configuration}
 * public class LegacyServiceFeignConfig {
 *
 *     {@code @Bean}
 *     public AuthenticationInterceptor basicAuthInterceptor(
 *         {@code @Value("${legacy.service.username}")} String username,
 *         {@code @Value("${legacy.service.password}")} String password
 *     ) {
 *         return new BasicAuthInterceptor(username, password);
 *     }
 * }
 *
 * {@code @FeignClient(
 *     name = "legacy-service",
 *     url = "${legacy.service.url}",
 *     configuration = LegacyServiceFeignConfig.class
 * )}
 * public interface LegacyServiceClient {
 *     {@code @GetMapping("/api/data")}
 *     DataResponse getData();
 * }
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
public class BasicAuthInterceptor implements AuthenticationInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";

    private final String encodedCredentials;
    private final String username;

    /**
     * Creates a Basic Auth interceptor.
     *
     * @param username the username for authentication
     * @param password the password for authentication
     * @throws IllegalArgumentException if username or password is null/empty
     */
    public BasicAuthInterceptor(String username, String password) {
        Assert.hasText(username, "Username must not be null or empty");
        Assert.hasText(password, "Password must not be null or empty");

        this.username = username;
        String credentials = username + ":" + password;
        this.encodedCredentials = BASIC_PREFIX +
                Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        log.debug("BasicAuthInterceptor initialized for user: {}", username);
    }

    @Override
    public void apply(RequestTemplate template) {
        // Only add header if not already present (allow per-request override)
        if (!template.headers().containsKey(AUTHORIZATION_HEADER)) {
            template.header(AUTHORIZATION_HEADER, encodedCredentials);
            log.trace("Added Basic Auth header for user '{}' to request: {} {}",
                      username, template.method(), template.url());
        } else {
            log.trace("Authorization header already present, skipping Basic Auth for: {} {}",
                      template.method(), template.url());
        }
    }

    @Override
    public String getAuthenticationType() {
        return "BASIC_AUTH";
    }

    /**
     * Alternative: Delegate to Spring Cloud OpenFeign's built-in BasicAuthRequestInterceptor.
     * <p>
     * This is a wrapper for compatibility with our AuthenticationInterceptor interface.
     *
     * @param username the username
     * @param password the password
     * @return BasicAuthRequestInterceptor wrapped as AuthenticationInterceptor
     */
    public static AuthenticationInterceptor fromSpringFeign(String username, String password) {
        BasicAuthRequestInterceptor delegate = new BasicAuthRequestInterceptor(username, password);

        return new AuthenticationInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                delegate.apply(template);
            }

            @Override
            public String getAuthenticationType() {
                return "BASIC_AUTH_SPRING";
            }
        };
    }
}
