package com.enterprise.business.dto;

import com.enterprise.business.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Product Data Transfer Object
 * <p>
 * DTO for Product entity to expose to API clients.
 * Excludes internal fields and provides a clean API contract.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product information")
public class ProductDTO {

    @Schema(description = "Product ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Product name", example = "Laptop Pro 15")
    private String name;

    @Schema(description = "Stock Keeping Unit (SKU)", example = "LAP-PRO-15-001")
    private String sku;

    @Schema(description = "Product description", example = "High-performance laptop")
    private String description;

    @Schema(description = "Product price", example = "1299.99")
    private BigDecimal price;

    @Schema(description = "Product category", example = "Electronics")
    private String category;

    @Schema(description = "Stock quantity", example = "50")
    private Integer stockQuantity;

    @Schema(description = "Product is active", example = "true")
    private Boolean active;

    @Schema(description = "Product creation timestamp", example = "2025-01-01T00:00:00Z")
    private Instant createdAt;

    @Schema(description = "Product last update timestamp", example = "2025-01-15T00:00:00Z")
    private Instant updatedAt;

    /**
     * Convert Product entity to ProductDTO
     *
     * @param product Product entity
     * @return ProductDTO instance
     */
    public static ProductDTO from(Product product) {
        if (product == null) {
            return null;
        }

        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .stockQuantity(product.getStockQuantity())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
