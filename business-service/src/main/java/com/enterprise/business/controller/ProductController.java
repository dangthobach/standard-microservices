package com.enterprise.business.controller;

import com.enterprise.business.entity.Product;
import com.enterprise.business.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<Product>> getProducts(@PageableDefault(size = 20) Pageable pageable) {
        log.info("REST request to get Products");
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable UUID id) {
        log.info("REST request to get Product: {}", id);
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:create')") // or based on IAM role
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        log.info("REST request to create Product: {}", product.getSku());
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, @RequestBody Product product) {
        log.info("REST request to update Product: {}", id);
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        log.info("REST request to delete Product: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
