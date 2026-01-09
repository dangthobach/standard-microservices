package com.enterprise.business.controller;

import com.enterprise.business.entity.ProductAttachment;
import com.enterprise.business.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/products/{productId}/files")
@RequiredArgsConstructor
public class ProductFileController {

    private final ProductService productService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ProductAttachment> uploadFile(@PathVariable UUID productId,
                                                        @RequestParam("file") MultipartFile file) {
        log.info("Uploading file for product: {}", productId);
        return ResponseEntity.ok(productService.uploadProductFile(productId, file));
    }

    @GetMapping
    public ResponseEntity<List<ProductAttachment>> getFiles(@PathVariable UUID productId) {
        return ResponseEntity.ok(productService.getProductFiles(productId));
    }

    @GetMapping("/{fileId}/view")
    public ResponseEntity<Void> viewFile(@PathVariable UUID productId, @PathVariable UUID fileId) {
        log.info("Requesting VIEW for file: {} of product: {}", fileId, productId);
        
        // 1. Get Redirect Path (Checks permissions)
        String redirectPath = productService.getFileRedirectPath(productId, fileId, false);
        
        // 2. Get Metadata for Headers
        com.enterprise.business.entity.ProductAttachment meta = productService.getFileMetadata(fileId);

        // 3. Return X-Accel-Redirect with Inline Disposition
        return ResponseEntity.ok()
                .header("X-Accel-Redirect", redirectPath)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + meta.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, meta.getFileType())
                .build();
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Void> downloadFile(@PathVariable UUID productId, @PathVariable UUID fileId) {
        log.info("Requesting DOWNLOAD for file: {} of product: {}", fileId, productId);

        // 1. Get Redirect Path (Checks DOWNLOAD permissions)
        String redirectPath = productService.getFileRedirectPath(productId, fileId, true);
        
        // 2. Get Metadata for Headers
        com.enterprise.business.entity.ProductAttachment meta = productService.getFileMetadata(fileId);

        // 3. Return X-Accel-Redirect with Attachment Disposition
        return ResponseEntity.ok()
                .header("X-Accel-Redirect", redirectPath)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, meta.getFileType())
                .build();
    }
}
