package com.enterprise.integration.service;

import com.enterprise.integration.client.IamServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdPartyIntegrationService {

    private final IamServiceClient iamServiceClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${integration.providers.provider-a.base-url:}")
    private String providerABaseUrl;

    @Value("${integration.providers.provider-a.auth.client-id:}")
    private String providerAClientId;

    @Value("${integration.providers.provider-a.auth.client-secret:}")
    private String providerAClientSecret;

    /**
     * Call Provider A with L1 (Caffeine, 5s) and L2 (Redis, 1h) Caching
     * Uses composite cache or chaining.
     * Here we demo L2 Redis Cache (long lived).
     * L1 is automatically handled if we added @Cacheable(cacheManager = "caffeineCacheManager") on a separate method,
     * or we rely on OS page cache / network speed for L1 efficiency in this demo.
     */
    @Cacheable(value = "thirdPartyData", key = "'providerA-data'", cacheManager = "redisCacheManager")
    public String getProviderAData() {
        log.info("Fetching data from Provider A (Cache Miss)...");

        // 1. Get Token from IAM Service
        log.info("Authenticating with IAM Service...");
        Map<String, Object> tokenResponse = iamServiceClient.getClientToken(providerAClientId, providerAClientSecret);
        String accessToken = (String) tokenResponse.get("access_token");

        // 2. Call Third Party
        String url = providerABaseUrl + "/data";
        // Header setup omitted for brevity in template, effectively: Authorization: Bearer accessToken
        
        // Mock response for now as we don't have real 3rd party
        return "Data from Provider A using Token: " + accessToken.substring(0, 5) + "...";
    }

    /**
     * Demo method for Public Key validation flow
     */
    public String verifyPublicKeyFlow() {
        return "Public Key: " + iamServiceClient.getPublicKey();
    }
}
