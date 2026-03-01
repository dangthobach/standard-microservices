package com.enterprise.integration.connector;

/**
 * Authorization type for connector
 */
public enum AuthorizationType {
    NONE,
    BASIC_AUTH,
    BEARER_TOKEN,
    API_KEY,
    OAUTH2,
    AWS_SIGV4
}
