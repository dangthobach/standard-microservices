package com.enterprise.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Update Product Request DTO
 * <p>
 * Request DTO for updating an existing product.
 * Used in REST controller to receive client input.
 * All fields are optional - only provided fields will be updated.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing product")
public class UpdateProductRequest {

    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    @Schema(description = "Product name", example = "Laptop Pro 15")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Product description", example = "High-performance laptop")
    private String description;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Schema(description = "Product price", example = "1299.99")
    private BigDecimal price;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    @Schema(description = "Product category", example = "Electronics")
    private String category;

    @Min(value = 0, message = "Stock quantity must be non-negative")
    @Schema(description = "Stock quantity", example = "50")
    private Integer stockQuantity;

    @Schema(description = "Product active status", example = "true")
    private Boolean active;
}
