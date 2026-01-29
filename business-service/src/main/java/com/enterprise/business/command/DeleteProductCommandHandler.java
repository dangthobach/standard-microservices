package com.enterprise.business.command;

import com.enterprise.business.entity.Product;
import com.enterprise.business.repository.ProductRepository;
import com.enterprise.common.cqrs.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delete Product Command Handler
 *
 * Handles the DeleteProductCommand by:
 * 1. Validating that product exists
 * 2. Performing soft delete (marking as deleted)
 * 3. Invalidating cache
 *
 * Transaction Management:
 * - @Transactional ensures all database operations are atomic
 *
 * Cache Management:
 * - @CacheEvict removes product from cache
 * - Also invalidates list cache
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteProductCommandHandler implements CommandHandler<DeleteProductCommand, Void> {

    private final ProductRepository productRepository;

    /**
     * Handle DeleteProductCommand
     *
     * @param command The command containing product ID
     * @return null (void operation)
     * @throws IllegalArgumentException if product not found
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#command.productId()", allEntries = true)
    public Void handle(DeleteProductCommand command) {
        log.debug("Handling DeleteProductCommand: productId={}", command.productId());

        // Step 1: Find existing product
        Product product = productRepository.findById(command.productId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found: " + command.productId()
                ));

        // Step 2: Soft delete
        product.setDeleted(true);
        productRepository.save(product);

        log.info("✅ Product deleted (soft): id={}, sku={}", product.getId(), product.getSku());

        return null;
    }
}
