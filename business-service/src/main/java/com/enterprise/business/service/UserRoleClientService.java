package com.enterprise.business.service;

import com.enterprise.business.client.IamClient;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserRoleClientService {

    private final IamClient iamClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<String, List<String>> localCache;

    private static final String ROLE_KEY_PREFIX = "business:authz:roles:";
    private static final Duration REDIS_TTL = Duration.ofHours(1);
    
    public UserRoleClientService(IamClient iamClient, RedisTemplate<String, Object> redisTemplate) {
        this.iamClient = iamClient;
        this.redisTemplate = redisTemplate;
        
        // L1 Cache: Caffeine (60s TTL, 10k entries)
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(10_000)
                .build();
    }

    public List<String> getUserRoles(String userId) {
        if (userId == null || userId.isBlank()) return Collections.emptyList();

        // 1. Check L1 (Caffeine)
        List<String> cachedRoles = localCache.getIfPresent(userId);
        if (cachedRoles != null) return cachedRoles;

        // 2. Check L2 (Redis)
        String redisKey = ROLE_KEY_PREFIX + userId;
        try {
            List<String> redisRoles = (List<String>) redisTemplate.opsForValue().get(redisKey);
            if (redisRoles != null) {
                localCache.put(userId, redisRoles);
                return redisRoles;
            }
        } catch (Exception e) {
            log.warn("Redis error fetching roles: {}", e.getMessage());
        }

        // 3. Fallback to IAM Service (Feign)
        try {
            log.debug("Cache MISS for userId: {}, calling IAM Service", userId);
            List<String> fetchedRoles = iamClient.getUserRoles(userId);
            
            // Populate Caches
            if (fetchedRoles != null) {
                localCache.put(userId, fetchedRoles);
                try {
                    redisTemplate.opsForValue().set(redisKey, fetchedRoles, REDIS_TTL);
                } catch (Exception e) {
                    log.warn("Redis error saving roles: {}", e.getMessage());
                }
                return fetchedRoles;
            }
        } catch (Exception e) {
            log.error("Failed to fetch roles from IAM for user: {}", userId, e);
        }

        return Collections.emptyList();
    }
}
