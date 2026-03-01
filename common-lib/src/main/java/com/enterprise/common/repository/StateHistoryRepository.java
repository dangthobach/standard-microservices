package com.enterprise.common.repository;

import com.enterprise.common.entity.StateHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for State History records.
 *
 * Provides methods to query state change audit trail
 * for any entity type across the system.
 */
@Repository
public interface StateHistoryRepository extends JpaRepository<StateHistory, UUID> {

    /**
     * Get full state history for a specific entity, ordered by time descending
     */
    List<StateHistory> findByEntityTypeAndEntityIdOrderByChangedAtDesc(String entityType, String entityId);

    /**
     * Get paginated state history for a specific entity
     */
    Page<StateHistory> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    /**
     * Get state history for a specific entity type (e.g., all Product changes)
     */
    Page<StateHistory> findByEntityType(String entityType, Pageable pageable);

    /**
     * Get state changes made by a specific user
     */
    List<StateHistory> findByChangedByOrderByChangedAtDesc(String changedBy);

    /**
     * Get state changes within a time range
     */
    List<StateHistory> findByEntityTypeAndEntityIdAndChangedAtBetween(
            String entityType, String entityId, Instant from, Instant to);

    /**
     * Get the most recent state change for an entity
     */
    StateHistory findFirstByEntityTypeAndEntityIdOrderByChangedAtDesc(String entityType, String entityId);
}
