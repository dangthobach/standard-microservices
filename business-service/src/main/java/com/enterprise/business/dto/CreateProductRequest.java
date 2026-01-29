package com.enterprise.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Create Product Request DTO
 * <p>
 * Request DTO for creating a new product.
 * Used in REST controller to receive client input.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new product")
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    @Schema(description = "Product name", example = "Laptop Pro 15", required = true)
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(min = 1, max = 50, message = "SKU must be between 1 and 50 characters")
    @Schema(description = "Stock Keeping Unit", example = "LAP-PRO-15-001", required = true)
    private String sku;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Product description", example = "High-performance laptop")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Schema(description = "Product price", example = "1299.99", required = true)
    private BigDecimal price;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    @Schema(description = "Product category", example = "Electronics")
    private String category;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be non-negative")
    @Schema(description = "Initial stock quantity", example = "50", required = true)
    private Integer stockQuantity;
}
