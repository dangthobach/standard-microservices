package com.enterprise.gateway.config;

/**
 * @deprecated This configuration has been merged into {@link WebClientConfiguration}.
 * <p>
 * The WebClient configuration is now centralized in {@link WebClientConfiguration}
 * which provides both LoadBalanced and Standard WebClient builders.
 * <p>
 * This class is kept for backward compatibility but is no longer used.
 * All WebClient configuration should use {@link WebClientConfiguration}.
 *
 * @see WebClientConfiguration
 */
@Deprecated
public class WebClientLoadBalancerConfiguration {
    // Configuration moved to WebClientConfiguration
}

