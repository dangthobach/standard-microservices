package com.enterprise.business.query;

import com.enterprise.business.dto.ProductDTO;
import com.enterprise.common.cqrs.Query;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Get Product By ID Query
 *
 * Represents a request to retrieve a product by its ID.
 * This is a READ operation with no side effects.
 *
 * @param productId Product ID (required)
 * @author Enterprise Team
 * @since 1.0.0
 */
public record GetProductByIdQuery(
    @NotNull(message = "Product ID is required")
    UUID productId
) implements Query<ProductDTO> {
}
