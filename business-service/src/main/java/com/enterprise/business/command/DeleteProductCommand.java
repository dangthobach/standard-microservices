package com.enterprise.business.command;

import com.enterprise.common.cqrs.Command;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Delete Product Command
 *
 * Represents a request to delete (soft delete) a product in the system.
 * This is a WRITE operation that changes system state.
 *
 * Uses soft delete pattern - product is marked as deleted but not removed from database.
 *
 * @param productId Product ID to delete (required)
 * @author Enterprise Team
 * @since 1.0.0
 */
public record DeleteProductCommand(
    @NotNull(message = "Product ID is required")
    UUID productId
) implements Command<Void> {
}
