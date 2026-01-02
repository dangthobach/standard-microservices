package com.enterprise.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * IAM Service Application - Identity and Access Management
 * - Uses Virtual Threads (Java 21) for blocking I/O operations
 * - Integrates with Keycloak for OAuth2/OIDC via Feign clients
 * - Manages users, roles, permissions, and authorization
 * - Implements Redis L2 cache for authorization decisions
 */
@SpringBootApplication(scanBasePackages = {"com.enterprise.iam", "com.enterprise.common"})
@EnableCaching
@EnableFeignClients
public class IamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamServiceApplication.class, args);
    }
}
