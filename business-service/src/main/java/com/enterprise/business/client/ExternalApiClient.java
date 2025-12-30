package com.enterprise.business.client;

import com.enterprise.business.dto.ExternalDataResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign Client for External API
 * <p>
 * Example of calling an external third-party API using API Key authentication.
 * <p>
 * Configuration:
 * - Uses FeignClientConfiguration for standard timeouts and error handling
 * - Uses ApiKeyInterceptor to add API key to requests
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@FeignClient(
    name = "external-api",
    url = "${app.services.external.url:https://api.example.com}",
    configuration = ExternalApiFeignConfiguration.class
)
public interface ExternalApiClient {

    /**
     * Fetch data from external API.
     *
     * @param query Search query
     * @return External data
     */
    @GetMapping("/data")
    ExternalDataResponse fetchData(@RequestParam("q") String query);
}
