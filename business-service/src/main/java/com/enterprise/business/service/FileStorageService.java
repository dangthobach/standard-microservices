package com.enterprise.business.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;

    public void uploadFile(String bucketName, String objectName, MultipartFile file) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
            }
        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new RuntimeException("Error uploading file: " + e.getMessage());
        }
    }

    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Error deleting file from MinIO", e);
            throw new RuntimeException("Error deleting file: " + e.getMessage());
        }
    }
    
    /**
     * Check if a file exists (Lightweight check for verification if needed)
     */
    public boolean fileExists(String bucketName, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
