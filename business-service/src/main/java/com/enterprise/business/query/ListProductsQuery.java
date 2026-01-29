package com.enterprise.business.query;

import com.enterprise.business.dto.ProductDTO;
import com.enterprise.common.cqrs.Query;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * List Products Query
 *
 * Represents a request to retrieve a paginated list of active products.
 * This is a READ operation with no side effects.
 *
 * Note: Pagination caching is complex and usually avoided.
 * Individual products are cached, but list queries hit the database.
 *
 * @param page Page number (0-based, required)
 * @param size Page size (required, min 1)
 * @author Enterprise Team
 * @since 1.0.0
 */
public record ListProductsQuery(
    @NotNull(message = "Page number is required")
    @Min(value = 0, message = "Page number must be non-negative")
    Integer page,

    @NotNull(message = "Page size is required")
    @Min(value = 1, message = "Page size must be at least 1")
    Integer size
) implements Query<List<ProductDTO>> {
}
