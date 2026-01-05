package com.enterprise.business.service;

import com.enterprise.business.entity.Product;
import com.enterprise.business.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Product Service
 * <p>
 * Demonstrates Multi-Level Caching (L2 Redis via @Cacheable).
 * Caffeine L1 is configured via Spring CacheManager in application.yml or
 * CacheConfig.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "products")
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Get Product by ID
     * <p>
     * 
     * @Cacheable logic:
     *            1. Check Cache (Redis)
     *            2. If Miss -> DB Query -> Put to Cache
     */
    @Cacheable(key = "#id", unless = "#result == null")
    public Product getProduct(UUID id) {
        log.info("Fetching product from DB: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    /**
     * Get Product by SKU
     */
    @Cacheable(key = "#sku", unless = "#result == null")
    public Product getProductBySku(String sku) {
        log.info("Fetching product from DB by SKU: {}", sku);
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));
    }

    @Transactional
    @CacheEvict(allEntries = true) // Simple invalidation for listing
    public Product createProduct(Product product) {
        log.info("Creating product: {}", product.getSku());
        return productRepository.save(product);
    }

    /**
     * Update Product
     * <p>
     * Updates both DB and Cache (Write-Through pattern via @CachePut)
     */
    @Transactional
    @CachePut(key = "#id")
    public Product updateProduct(UUID id, Product details) {
        log.info("Updating product: {}", id);
        Product product = getProduct(id);

        product.setName(details.getName());
        product.setPrice(details.getPrice());
        product.setStockQuantity(details.getStockQuantity());
        product.setDescription(details.getDescription());

        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(key = "#id")
    public void deleteProduct(UUID id) {
        log.info("Deleting product: {}", id);
        productRepository.deleteById(id);
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        // Warning: Caching pagination is tricky (Cache Stampede/Invalidation
        // complexity)
        // Usually, we cache individual items, but list queries hit DB (or specialized
        // search index like Elasticsearch)
        return productRepository.findByActiveTrue(pageable);
    }
}
