package com.enterprise.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebClient Configuration Properties
 * <p>
 * Externalized configuration for WebClient across all services.
 * This follows the 12-Factor App methodology for configuration management.
 * <p>
 * Usage in application.yml:
 * 
 * <pre>
 * webclient:
 *   connect-timeout: 5000
 *   read-timeout: 30000
 *   write-timeout: 30000
 *   max-connections: 100
 *   pending-acquire-timeout: 45000
 *   max-in-memory-size: 262144
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "webclient")
public class WebClientProperties {

    /**
     * Connection timeout in milliseconds.
     * Time to wait for a connection to be established.
     * Default: 5000ms (5 seconds)
     */
    private int connectTimeout = 5000;

    /**
     * Read timeout in milliseconds.
     * Time to wait for response data after connection is established.
     * Default: 30000ms (30 seconds)
     */
    private int readTimeout = 30000;

    /**
     * Write timeout in milliseconds.
     * Time to wait for request data to be sent.
     * Default: 30000ms (30 seconds)
     */
    private int writeTimeout = 30000;

    /**
     * Maximum number of connections in the connection pool.
     * Default: 100
     */
    private int maxConnections = 100;

    /**
     * Pending acquire timeout in milliseconds.
     * Time to wait to acquire a connection from the pool.
     * Default: 45000ms (45 seconds)
     */
    private int pendingAcquireTimeout = 45000;

    /**
     * Maximum in-memory buffer size in bytes.
     * Used for buffering response body.
     * Default: 256KB (262144 bytes)
     */
    private int maxInMemorySize = 256 * 1024;

    /**
     * Whether to enable request/response logging at DEBUG level.
     * Default: true
     */
    private boolean loggingEnabled = true;
}
