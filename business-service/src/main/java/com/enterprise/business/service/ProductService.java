package com.enterprise.business.service;

import com.enterprise.business.entity.Product;
import com.enterprise.business.repository.ProductRepository;
import java.util.UUID;
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
    private final com.enterprise.business.repository.ProductAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;

    @org.springframework.beans.factory.annotation.Value("${minio.bucket}")
    private String productFilesBucket;

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
        return productRepository
            .findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException("Product not found: " + id)
            );
    }

    /**
     * Get Product by SKU
     */
    @Cacheable(key = "#sku", unless = "#result == null")
    public Product getProductBySku(String sku) {
        log.info("Fetching product from DB by SKU: {}", sku);
        return productRepository
            .findBySku(sku)
            .orElseThrow(() ->
                new IllegalArgumentException("Product not found: " + sku)
            );
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

    // --- File Management ---

    @Transactional
    @CacheEvict(key = "#productId") // Invalidate product cache if needed, or separate cache for files
    public com.enterprise.business.entity.ProductAttachment uploadProductFile(
        UUID productId,
        org.springframework.web.multipart.MultipartFile file
    ) {
        // Verify product exists
        Product product = getProduct(productId);

        String filename =
            java.util.UUID.randomUUID() + "-" + file.getOriginalFilename();
        String storageKey = "products/" + productId + "/" + filename;

        // Upload to S3
        fileStorageService.uploadFile(productFilesBucket, storageKey, file);

        // Save Metadata
        com.enterprise.business.entity.ProductAttachment attachment =
            com.enterprise.business.entity.ProductAttachment.builder()
                .productId(productId)
                .fileName(file.getOriginalFilename()) // Keep original name
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .storageKey(storageKey)
                .build();

        return attachmentRepository.save(attachment);
    }

    public java.util.List<
        com.enterprise.business.entity.ProductAttachment
    > getProductFiles(UUID productId) {
        return attachmentRepository.findByProductId(productId);
    }

    public String getFileRedirectPath(UUID productId, UUID fileId, boolean isDownload) {
        com.enterprise.business.entity.ProductAttachment attachment =
            attachmentRepository
                .findById(fileId)
                .orElseThrow(() ->
                    new IllegalArgumentException("File not found: " + fileId)
                );

        if (!attachment.getProductId().equals(productId)) {
            throw new SecurityException("File does not belong to this product");
        }

        // TODO: Implement Real Permissions Check
        // e.g. if (isDownload && !userHasPremium) throw new AccessDeniedException()
        if (isDownload) {
            log.info("Processing DOWNLOAD request for file: {}", attachment.getFileName());
        } else {
            log.info("Processing VIEW request for file: {}", attachment.getFileName());
        }

        // Return path for Nginx X-Accel-Redirect
        // Format: /internal-files/{bucket}/{key}
        return (
            "/internal-files/" +
            productFilesBucket +
            "/" +
            attachment.getStorageKey()
        );
    }
    
    public com.enterprise.business.entity.ProductAttachment getFileMetadata(UUID fileId) {
         return attachmentRepository.findById(fileId).orElseThrow();
    }
}
