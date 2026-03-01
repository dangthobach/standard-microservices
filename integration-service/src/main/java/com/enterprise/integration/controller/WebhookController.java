package com.enterprise.integration.controller;

import com.enterprise.common.constant.ApiConstants;
import com.enterprise.common.dto.ApiResponse;
import com.enterprise.integration.entity.WebhookEndpoint;
import com.enterprise.integration.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhook Management", description = "Manage inbound webhooks and process events")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/receive/{path}")
    @Operation(summary = "Receive webhook", description = "Public endpoint for receiving external webhooks")
    public ResponseEntity<ApiResponse<Void>> receiveWebhook(
            @PathVariable String path,
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers) {

        webhookService.processWebhook(path, payload, headers);
        return ResponseEntity.ok(ApiResponse.success("Webhook received"));
    }

    @PostMapping("/config")
    @PreAuthorize("hasAuthority('webhook:write')")
    @Operation(summary = "Create webhook endpoint", description = "Register a new webhook listener")
    public ResponseEntity<ApiResponse<WebhookEndpoint>> createEndpoint(@RequestBody WebhookEndpoint endpoint) {
        WebhookEndpoint created = webhookService.createEndpoint(endpoint);
        return ResponseEntity.ok(ApiResponse.success("Webhook endpoint created", created));
    }
}
