package com.enterprise.business.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "iam-service")
public interface IamClient {

    @GetMapping("/api/internal/roles/keycloak/{keycloakId}")
    List<String> getUserRoles(@PathVariable("keycloakId") String keycloakId);
}
