package com.enterprise.business.dto;

import lombok.Data;

import java.util.List;

/**
 * User Response DTO
 * <p>
 * Example DTO for Feign client response deserialization.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Data
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private boolean active;
}
