package com.enterprise.business.controller;

import com.enterprise.business.command.CreateProductCommand;
import com.enterprise.business.command.DeleteProductCommand;
import com.enterprise.business.command.UpdateProductCommand;
import com.enterprise.business.dto.CreateProductRequest;
import com.enterprise.business.dto.ProductDTO;
import com.enterprise.business.dto.UpdateProductRequest;
import com.enterprise.business.query.GetProductByIdQuery;
import com.enterprise.business.query.ListProductsQuery;
import com.enterprise.common.cqrs.CommandBus;
import com.enterprise.common.cqrs.QueryBus;
import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Product Management Controller
 * <p>
 * Demonstrates CQRS pattern implementation:
 * - Commands (Write): CreateProductCommand, UpdateProductCommand, DeleteProductCommand
 * - Queries (Read): GetProductByIdQuery, ListProductsQuery
 * - Uses CommandBus and QueryBus for decoupled business logic
 * <p>
 * All endpoints return standardized ApiResponse wrapper.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Product CRUD operations using CQRS pattern")
public class ProductController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    /**
     * Get all products with pagination
     * <p>
     * Example: GET /api/products?page=0&size=20
     *
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20)
     * @return Paginated list of products wrapped in ApiResponse
     */
    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve paginated list of active products")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST request to get Products: page={}, size={}", page, size);

        ListProductsQuery query = new ListProductsQuery(page, size);
        List<ProductDTO> products = queryBus.dispatch(query);

        return ResponseEntity.ok(
                ApiResponse.success("Products retrieved successfully", products)
        );
    }

    /**
     * Get product by ID
     * <p>
     * Example: GET /api/products/550e8400-e29b-41d4-a716-446655440000
     *
     * @param id Product ID
     * @return Product details wrapped in ApiResponse
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve product details by ID")
    public ResponseEntity<ApiResponse<ProductDTO>> getProduct(@PathVariable UUID id) {
        log.info("REST request to get Product: {}", id);

        GetProductByIdQuery query = new GetProductByIdQuery(id);
        ProductDTO product = queryBus.dispatch(query);

        return ResponseEntity.ok(
                ApiResponse.success("Product retrieved successfully", product)
        );
    }

    /**
     * Create a new product
     * <p>
     * Example: POST /api/products
     * <pre>
     * {
     *   "name": "Laptop Pro 15",
     *   "sku": "LAP-PRO-15-001",
     *   "price": 1299.99,
     *   "stockQuantity": 50
     * }
     * </pre>
     *
     * @param request Create product request
     * @return Created product ID wrapped in ApiResponse
     */
    @PostMapping
    @PreAuthorize("hasAuthority('product:create')")
    @Operation(summary = "Create product", description = "Create a new product")
    public ResponseEntity<ApiResponse<UUID>> createProduct(@RequestBody CreateProductRequest request) {
        log.info("REST request to create Product: sku={}", request.getSku());

        CreateProductCommand command = CreateProductCommand.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .stockQuantity(request.getStockQuantity())
                .build();

        UUID productId = commandBus.dispatch(command);

        return ResponseEntity.ok(
                ApiResponse.success("Product created successfully", productId)
        );
    }

    /**
     * Update an existing product
     * <p>
     * Example: PUT /api/products/550e8400-e29b-41d4-a716-446655440000
     * <pre>
     * {
     *   "name": "Laptop Pro 15 Updated",
     *   "price": 1199.99,
     *   "stockQuantity": 45
     * }
     * </pre>
     *
     * @param id      Product ID
     * @param request Update product request (all fields optional)
     * @return Updated product ID wrapped in ApiResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:write')")
    @Operation(summary = "Update product", description = "Update an existing product")
    public ResponseEntity<ApiResponse<UUID>> updateProduct(
            @PathVariable UUID id,
            @RequestBody UpdateProductRequest request
    ) {
        log.info("REST request to update Product: {}", id);

        UpdateProductCommand command = UpdateProductCommand.builder()
                .productId(id)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .stockQuantity(request.getStockQuantity())
                .active(request.getActive())
                .build();

        UUID productId = commandBus.dispatch(command);

        return ResponseEntity.ok(
                ApiResponse.success("Product updated successfully", productId)
        );
    }

    /**
     * Delete a product (soft delete)
     * <p>
     * Example: DELETE /api/products/550e8400-e29b-41d4-a716-446655440000
     *
     * @param id Product ID
     * @return Success response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    @Operation(summary = "Delete product", description = "Soft delete a product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        log.info("REST request to delete Product: {}", id);

        DeleteProductCommand command = new DeleteProductCommand(id);
        commandBus.dispatch(command);

        return ResponseEntity.ok(
                ApiResponse.success("Product deleted successfully")
        );
    }
}
