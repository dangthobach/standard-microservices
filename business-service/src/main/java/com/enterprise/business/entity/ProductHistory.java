package com.enterprise.business.entity;

import com.enterprise.business.entity.base.HistoryEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Product History Entity
 * <p>
 * Stores snapshots of Product state changes
 */
@Entity
@Table(name = "product_history")
@Getter
@Setter
@NoArgsConstructor
public class ProductHistory extends HistoryEntity {

    private static final long serialVersionUID = 1L;

    @Builder
    public ProductHistory(UUID entityId, String action, String snapshot, String diff,
            String changedBy, String correlationId, String previousStatus,
            String currentStatus, String ipAddress) {
        super(entityId, "PRODUCT", action, snapshot, diff, changedBy, correlationId, previousStatus, currentStatus,
                ipAddress);
    }
}
