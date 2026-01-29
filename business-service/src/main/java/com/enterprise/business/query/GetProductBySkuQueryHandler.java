package com.enterprise.business.query;

import com.enterprise.business.dto.ProductDTO;
import com.enterprise.business.entity.Product;
import com.enterprise.business.repository.ProductRepository;
import com.enterprise.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Get Product By SKU Query Handler
 *
 * Handles the GetProductBySkuQuery by:
 * 1. Fetching product from database (or cache)
 * 2. Converting entity to DTO
 * 3. Returning product DTO
 *
 * Transaction Management:
 * - @Transactional(readOnly = true) optimizes for read-only operations
 *
 * Cache Management:
 * - @Cacheable caches product by SKU
 * - Cache key: sku
 * - Cache name: "products"
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetProductBySkuQueryHandler implements QueryHandler<GetProductBySkuQuery, ProductDTO> {

    private final ProductRepository productRepository;

    /**
     * Handle GetProductBySkuQuery
     *
     * @param query The query containing SKU
     * @return ProductDTO
     * @throws IllegalArgumentException if product not found
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#query.sku()", unless = "#result == null")
    public ProductDTO handle(GetProductBySkuQuery query) {
        log.debug("Handling GetProductBySkuQuery: sku={}", query.sku());

        Product product = productRepository.findBySku(query.sku())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found with SKU: " + query.sku()
                ));

        return ProductDTO.from(product);
    }
}
