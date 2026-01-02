package com.enterprise.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Business Service Application
 * - Uses Virtual Threads (Java 21) for blocking I/O operations
 * - Integrates with IAM service and external APIs via Feign clients
 * - Implements CQRS pattern with command and query separation
 * - Uses Redis for distributed caching
 */
@SpringBootApplication(scanBasePackages = {"com.enterprise.business", "com.enterprise.common"})
@EnableFeignClients
public class BusinessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessServiceApplication.class, args);
    }
}
