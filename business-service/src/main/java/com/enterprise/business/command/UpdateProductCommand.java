package com.enterprise.business.command;

import com.enterprise.common.cqrs.Command;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Update Product Command
 *
 * Represents a request to update an existing product in the system.
 * This is a WRITE operation that changes system state.
 *
 * @param productId     Product ID to update (required)
 * @param name          Product name (2-100 characters, optional)
 * @param description   Product description (max 1000 characters, optional)
 * @param price         Product price (must be positive, optional)
 * @param category      Product category (max 50 characters, optional)
 * @param stockQuantity Stock quantity (must be non-negative, optional)
 * @param active        Product active status (optional)
 * @author Enterprise Team
 * @since 1.0.0
 */
@Builder
public record UpdateProductCommand(
    @NotNull(message = "Product ID is required")
    UUID productId,

    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    BigDecimal price,

    @Size(max = 50, message = "Category must not exceed 50 characters")
    String category,

    @Min(value = 0, message = "Stock quantity must be non-negative")
    Integer stockQuantity,

    Boolean active
) implements Command<UUID> {
}
