package com.enterprise.business.command;

import com.enterprise.business.entity.Product;
import com.enterprise.business.repository.ProductRepository;
import com.enterprise.common.cqrs.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Update Product Command Handler
 *
 * Handles the UpdateProductCommand by:
 * 1. Validating that product exists
 * 2. Updating product fields (only non-null fields)
 * 3. Updating cache (write-through pattern)
 * 4. Returning the updated product's ID
 *
 * Transaction Management:
 * - @Transactional ensures all database operations are atomic
 *
 * Cache Management:
 * - @CachePut updates cache with new product data
 * - @CacheEvict invalidates list cache
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateProductCommandHandler implements CommandHandler<UpdateProductCommand, UUID> {

    private final ProductRepository productRepository;

    /**
     * Handle UpdateProductCommand
     *
     * @param command The command containing update data
     * @return The ID of the updated product
     * @throws IllegalArgumentException if product not found
     */
    @Override
    @Transactional
    @CachePut(value = "products", key = "#command.productId()")
    @CacheEvict(value = "products", allEntries = true)
    public UUID handle(UpdateProductCommand command) {
        log.debug("Handling UpdateProductCommand: productId={}", command.productId());

        // Step 1: Find existing product
        Product product = productRepository.findById(command.productId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found: " + command.productId()
                ));

        // Step 2: Update fields (only non-null fields)
        if (command.name() != null) {
            product.setName(command.name());
        }
        if (command.description() != null) {
            product.setDescription(command.description());
        }
        if (command.price() != null) {
            product.setPrice(command.price());
        }
        if (command.category() != null) {
            product.setCategory(command.category());
        }
        if (command.stockQuantity() != null) {
            product.setStockQuantity(command.stockQuantity());
        }
        if (command.active() != null) {
            product.setActive(command.active());
        }

        // Step 3: Persist changes
        product = productRepository.save(product);

        log.info("✅ Product updated: id={}, sku={}", product.getId(), product.getSku());

        return product.getId();
    }
}
