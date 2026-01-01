package com.enterprise.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard Security Configuration Properties
 *
 * Configures which roles have access to Dashboard APIs.
 * Supports dynamic role configuration via application.yml or Spring Cloud Config.
 *
 * Example configuration:
 * <pre>
 * dashboard:
 *   security:
 *     allowed-roles:
 *       - ADMIN
 *       - DEVELOPER
 *       - SUPPORT
 * </pre>
 *
 * Default: Only ADMIN role has access if not configured.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "dashboard.security")
public class DashboardSecurityProperties {

    /**
     * List of roles allowed to access Dashboard APIs
     * Default: ["ADMIN"]
     *
     * User needs ANY of these roles (OR logic) to access dashboard.
     */
    private List<String> allowedRoles = new ArrayList<>(List.of("ADMIN"));
}
