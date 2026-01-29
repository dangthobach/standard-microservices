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

import java.util.UUID;

/**
 * Get Product By ID Query Handler
 *
 * Handles the GetProductByIdQuery by:
 * 1. Fetching product from database (or cache)
 * 2. Converting entity to DTO
 * 3. Returning product DTO
 *
 * Transaction Management:
 * - @Transactional(readOnly = true) optimizes for read-only operations
 *
 * Cache Management:
 * - @Cacheable caches product by ID
 * - Cache key: productId
 * - Cache name: "products"
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetProductByIdQueryHandler implements QueryHandler<GetProductByIdQuery, ProductDTO> {

    private final ProductRepository productRepository;

    /**
     * Handle GetProductByIdQuery
     *
     * @param query The query containing product ID
     * @return ProductDTO
     * @throws IllegalArgumentException if product not found
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#query.productId()", unless = "#result == null")
    public ProductDTO handle(GetProductByIdQuery query) {
        log.debug("Handling GetProductByIdQuery: productId={}", query.productId());

        Product product = productRepository.findById(query.productId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found: " + query.productId()
                ));

        return ProductDTO.from(product);
    }
}
