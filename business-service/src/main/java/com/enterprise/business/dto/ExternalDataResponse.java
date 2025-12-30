package com.enterprise.business.dto;

import lombok.Data;

/**
 * External Data Response DTO
 * <p>
 * Example DTO for external API response deserialization.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Data
public class ExternalDataResponse {
    private String id;
    private String data;
    private String status;
}
