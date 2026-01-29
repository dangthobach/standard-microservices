package com.enterprise.business.query;

import com.enterprise.business.dto.ProductDTO;
import com.enterprise.common.cqrs.Query;
import jakarta.validation.constraints.NotBlank;

/**
 * Get Product By SKU Query
 *
 * Represents a request to retrieve a product by its SKU.
 * This is a READ operation with no side effects.
 *
 * @param sku Stock Keeping Unit (required)
 * @author Enterprise Team
 * @since 1.0.0
 */
public record GetProductBySkuQuery(
    @NotBlank(message = "SKU is required")
    String sku
) implements Query<ProductDTO> {
}
