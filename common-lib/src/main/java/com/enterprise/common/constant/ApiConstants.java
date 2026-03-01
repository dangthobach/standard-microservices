package com.enterprise.common.constant;

/**
 * API Constants for versioning and common path prefixes.
 * All controllers should use these constants for consistent API versioning.
 */
public final class ApiConstants {

    private ApiConstants() {
        // Utility class — no instantiation
    }

    /**
     * API version 1 prefix. All public endpoints should use this prefix.
     * Example: @RequestMapping(ApiConstants.API_V1 + "/users")
     */
    public static final String API_V1 = "/api/v1";

    /**
     * Internal API prefix. For inter-service communication only.
     * Not exposed through the API Gateway.
     */
    public static final String INTERNAL = "/api/internal";

    /**
     * Default page size for paginated endpoints.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum page size to prevent over-fetching.
     */
    public static final int MAX_PAGE_SIZE = 100;
}
