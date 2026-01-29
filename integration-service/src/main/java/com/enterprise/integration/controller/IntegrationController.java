package com.enterprise.integration.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.integration.service.ThirdPartyIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final ThirdPartyIntegrationService integrationService;

    @GetMapping("/provider-a")
    public ResponseEntity<ApiResponse<String>> getProviderAData() {
        return ResponseEntity.ok(ApiResponse.success(
            "Data retrieved from Provider A",
            integrationService.getProviderAData()
        ));
    }

    @GetMapping("/public-key")
    public ResponseEntity<ApiResponse<String>> getPublicKey() {
        return ResponseEntity.ok(ApiResponse.success(
            "Public Key retrieved from IAM",
            integrationService.verifyPublicKeyFlow()
        ));
    }
}
