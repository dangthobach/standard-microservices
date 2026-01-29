package com.enterprise.iam.controller;

import com.enterprise.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ClientAuthController {

    /**
     * Exchange Client ID & Secret for an Access Token.
     * In a real scenario, this would call Keycloak's /token endpoint.
     */
    @PostMapping("/client-token")
    public ResponseEntity<Map<String, Object>> getClientToken(
            @RequestParam String clientId,
            @RequestParam String clientSecret) {

        log.info("Received token request for client: {}", clientId);

        // Simple mock validation
        if ("app-client".equals(clientId) && "secret".equals(clientSecret)) {
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", "ey.mock.jwt.token." + UUID.randomUUID());
            response.put("token_type", "Bearer");
            response.put("expires_in", 3600);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Expose Public Key for signature verification.
     */
    @GetMapping("/public-key")
    public ResponseEntity<String> getPublicKey() {
        log.info("Received public key request");
        // Mock RSA Public Key
        String publicKey = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...\n-----END PUBLIC KEY-----";
        return ResponseEntity.ok(publicKey);
    }
}
