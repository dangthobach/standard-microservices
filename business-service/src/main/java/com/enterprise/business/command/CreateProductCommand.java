package com.enterprise.business.command;

import com.enterprise.common.cqrs.Command;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Create Product Command
 *
 * Represents a request to create a new product in the system.
 * This is a WRITE operation that changes system state.
 *
 * @param name          Product name (2-100 characters, required)
 * @param sku           Stock Keeping Unit (unique, 1-50 characters, required)
 * @param description   Product description (max 1000 characters, optional)
 * @param price         Product price (must be positive, required)
 * @param category      Product category (max 50 characters, optional)
 * @param stockQuantity Initial stock quantity (must be non-negative, required)
 * @author Enterprise Team
 * @since 1.0.0
 */
@Builder
public record CreateProductCommand(
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    String name,

    @NotBlank(message = "SKU is required")
    @Size(min = 1, max = 50, message = "SKU must be between 1 and 50 characters")
    String sku,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    BigDecimal price,

    @Size(max = 50, message = "Category must not exceed 50 characters")
    String category,

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be non-negative")
    Integer stockQuantity
) implements Command<UUID> {
}
