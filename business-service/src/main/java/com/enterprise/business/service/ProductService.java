package com.enterprise.business.service;

import com.enterprise.business.dto.ProductDTO;
import com.enterprise.business.query.GetProductByIdQuery;
import com.enterprise.common.cqrs.QueryBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Product Service
 * <p>
 * This service handles cross-cutting concerns and file operations for products.
 * <p>
 * Note: Product CRUD operations are handled via CQRS pattern:
 * - Commands: CreateProductCommand, UpdateProductCommand, DeleteProductCommand
 * - Queries: GetProductByIdQuery, GetProductBySkuQuery, ListProductsQuery
 * <p>
 * This service is kept for:
 * - File upload/download operations
 * - Permission checks for file access
 * - Cross-cutting concerns that don't fit CQRS pattern
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final com.enterprise.business.repository.ProductAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final UserRoleClientService userRoleClientService;
    private final PurchaseVerificationService purchaseVerificationService;

    @org.springframework.beans.factory.annotation.Value("${minio.bucket}")
    private String productFilesBucket;

    private final QueryBus queryBus;

    // --- File Management ---

    /**
     * Upload a file for a product
     * <p>
     * Verifies product exists using CQRS query before uploading file.
     *
     * @param productId Product ID
     * @param file      File to upload
     * @return ProductAttachment entity
     */
    @Transactional
    @CacheEvict(value = "products", key = "#productId") // Invalidate product cache
    public com.enterprise.business.entity.ProductAttachment uploadProductFile(
        UUID productId,
        org.springframework.web.multipart.MultipartFile file
    ) {
        // Verify product exists using CQRS query
        GetProductByIdQuery query = new GetProductByIdQuery(productId);
        ProductDTO product = queryBus.dispatch(query);
        
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

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

    /**
     * Get file redirect path with permission checks
     * <p>
     * Verifies product exists and user has permission to access the file.
     *
     * @param productId  Product ID
     * @param fileId     File ID
     * @param isDownload Whether this is a download request
     * @param userId     User ID
     * @param width      Optional image width for resizing
     * @param height     Optional image height for resizing
     * @return Redirect path for file access
     */
    public String getFileRedirectPath(UUID productId, UUID fileId, boolean isDownload, String userId, Integer width, Integer height) {
        // Verify product exists using CQRS query
        GetProductByIdQuery query = new GetProductByIdQuery(productId);
        ProductDTO product = queryBus.dispatch(query);
        
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
        
        com.enterprise.business.entity.ProductAttachment attachment =
            attachmentRepository
                .findById(fileId)
                .orElseThrow(() ->
                    new IllegalArgumentException("File not found: " + fileId)
                );

        if (!attachment.getProductId().equals(productId)) {
            throw new SecurityException("File does not belong to this product");
        }

        // 1. Fetch User Roles (Cached)
        java.util.List<String> roles = userRoleClientService.getUserRoles(userId);
        
        boolean isAdmin = roles.contains("ROLE_ADMIN") || roles.contains("ROLE_MANAGER");

        // 2. Permission Logic
        if (isAdmin) {
            log.info("Admin access granted for user: {}", userId);
        } else {
            if (isDownload) {
                // Download: Must have purchased OR be Admin
                boolean hasPurchased = purchaseVerificationService.hasPurchased(userId, productId);
                if (!hasPurchased) {
                    throw new org.springframework.security.access.AccessDeniedException("User has not purchased this product.");
                }
            } else {
                // View: Allow if purchased OR (maybe allow preview for all?)
                // For now, strict: View also requires purchase or maybe a lighter check
                boolean hasPurchased = purchaseVerificationService.hasPurchased(userId, productId);
                 if (!hasPurchased) {
                    // throw new org.springframework.security.access.AccessDeniedException("User cannot view this file.");
                    // Allow view for demo purposes if strict check is too blocking, or enforce it.
                    // User request said: "User bought product can view file"
                    throw new org.springframework.security.access.AccessDeniedException("User has not purchased this product.");
                }
            }
        }

        // 3. Construct Redirect Path
        // Format: /internal-files/{bucket}/{key}?width={w}&height={h}&mode=fit
        StringBuilder path = new StringBuilder("/internal-files/" + productFilesBucket + "/" + attachment.getStorageKey());
        
        // Append Resize Params if present and it's an image
        if (!isDownload && attachment.getFileType().startsWith("image/") && (width != null || height != null)) {
            path.append("?");
            if (width != null) path.append("width=").append(width).append("&");
            if (height != null) path.append("height=").append(height).append("&");
            path.append("mode=fit");
        }

        return path.toString();
    }
    
    public com.enterprise.business.entity.ProductAttachment getFileMetadata(UUID fileId) {
         return attachmentRepository.findById(fileId).orElseThrow();
    }
}
