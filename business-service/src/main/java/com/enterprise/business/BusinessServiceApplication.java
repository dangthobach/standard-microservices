package com.enterprise.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Business Service Application
 * <p>
 * Features:
 * - Uses Virtual Threads (Java 21) for blocking I/O operations
 * - Integrates with IAM service and external APIs via Feign clients
 * - Implements CQRS pattern with command and query separation:
 *   * Commands: CreateProductCommand, UpdateProductCommand, DeleteProductCommand
 *   * Queries: GetProductByIdQuery, GetProductBySkuQuery, ListProductsQuery
 * - Uses Redis for distributed caching (L2 cache)
 * - Uses Caffeine for local caching (L1 cache)
 * <p>
 * Architecture:
 * - Controllers use CommandBus and QueryBus (not direct service injection)
 * - Business logic in CommandHandlers and QueryHandlers
 * - ProductService handles file operations and cross-cutting concerns
 */
@SpringBootApplication(scanBasePackages = {"com.enterprise.business", "com.enterprise.common"})
@EnableFeignClients
public class BusinessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessServiceApplication.class, args);
    }
}
