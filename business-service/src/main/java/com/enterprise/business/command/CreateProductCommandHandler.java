package com.enterprise.business.command;

import com.enterprise.business.entity.Product;
import com.enterprise.business.repository.ProductRepository;
import com.enterprise.common.cqrs.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Create Product Command Handler
 *
 * Handles the CreateProductCommand by:
 * 1. Validating that SKU is unique
 * 2. Creating and persisting the Product entity
 * 3. Invalidating product list cache
 * 4. Returning the new product's ID
 *
 * Transaction Management:
 * - @Transactional ensures all database operations are atomic
 * - If any exception occurs, entire transaction is rolled back
 *
 * Cache Management:
 * - @CacheEvict invalidates product list cache on creation
 * - Individual product cache will be populated on first read
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateProductCommandHandler implements CommandHandler<CreateProductCommand, UUID> {

    private final ProductRepository productRepository;

    /**
     * Handle CreateProductCommand
     *
     * @param command The command containing product data
     * @return The ID of the newly created product
     * @throws IllegalArgumentException if SKU already exists
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public UUID handle(CreateProductCommand command) {
        log.debug("Handling CreateProductCommand: sku={}", command.sku());

        // Step 1: Validate uniqueness
        validateUniqueness(command);

        // Step 2: Create product entity
        Product product = Product.builder()
                .name(command.name())
                .sku(command.sku())
                .description(command.description())
                .price(command.price())
                .category(command.category())
                .stockQuantity(command.stockQuantity())
                .active(true)
                .build();

        // Step 3: Persist to database
        product = productRepository.save(product);

        log.info("✅ Product created: id={}, sku={}", product.getId(), product.getSku());

        return product.getId();
    }

    /**
     * Validate that SKU is unique
     *
     * @param command The command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateUniqueness(CreateProductCommand command) {
        if (productRepository.existsBySkuAndDeletedFalse(command.sku())) {
            throw new IllegalArgumentException(
                    "Product with SKU '" + command.sku() + "' already exists"
            );
        }
    }
}
