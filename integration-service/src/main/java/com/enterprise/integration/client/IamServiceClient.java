package com.enterprise.integration.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "iam-service")
public interface IamServiceClient {

    @PostMapping("/api/auth/client-token")
    Map<String, Object> getClientToken(@RequestParam("clientId") String clientId, 
                                      @RequestParam("clientSecret") String clientSecret);

    @GetMapping("/api/auth/public-key")
    String getPublicKey();
}
