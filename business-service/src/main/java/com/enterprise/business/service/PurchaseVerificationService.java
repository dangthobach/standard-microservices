package com.enterprise.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class PurchaseVerificationService {

    /**
     * Check if a user has purchased a product.
     * Mock implementation for now.
     *
     * @param userId Keycloak User ID
     * @param productId Product ID
     * @return true if purchased, false otherwise
     */
    public boolean hasPurchased(String userId, UUID productId) {
        // Mock Logic:
        // - Allow if userId contains "vip" (e.g. "user-vip-123")
        // - Allow if productId matches a specific UUID (for testing)
        // - Otherwise deny
        log.info("Checking purchase for user: {} and product: {}", userId, productId);
        
        if (userId.contains("vip")) {
             return true; 
        }
        
        // For testing purposes, we can uncomment this or change logic
        // return true; 
        
        return false;
    }
}
