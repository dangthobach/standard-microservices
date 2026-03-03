package com.enterprise.common.service;

import com.enterprise.common.entity.StateHistory;
import com.enterprise.common.repository.StateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * Service for recording and querying state transition history.
 *
 * This service is used automatically by StatefulEntity.changeStatus()
 * when a StateHistoryService bean is available in the Spring context.
 * It can also be called explicitly for custom state tracking.
 *
 * Usage:
 * 
 * <pre>
 * // Query history:
 * List<StateHistory> history = stateHistoryService.getHistory("Product", productId.toString());
 *
 * // Manual recording:
 * stateHistoryService.recordTransition("Order", orderId.toString(), "NEW", "PROCESSING", "admin", "Batch processing");
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@org.springframework.boot.autoconfigure.condition.ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "enterprise.state-history.enabled", havingValue = "true", matchIfMissing = false)
public class StateHistoryService {

        private final StateHistoryRepository stateHistoryRepository;

        /**
         * Record a state transition
         *
         * @param entityType Simple name of the entity class
         * @param entityId   ID of the entity (as String)
         * @param fromStatus Previous status (null for initial state)
         * @param toStatus   New status after transition
         * @param changedBy  User who triggered the change
         * @param reason     Optional reason for the change
         */
        public StateHistory recordTransition(String entityType, String entityId,
                        String fromStatus, String toStatus,
                        String changedBy, String reason) {
                StateHistory history = StateHistory.builder()
                                .entityType(entityType)
                                .entityId(entityId)
                                .fromStatus(fromStatus)
                                .toStatus(toStatus)
                                .changedBy(changedBy)
                                .reason(reason)
                                .build();

                StateHistory saved = stateHistoryRepository.save(history);
                log.debug("Recorded state transition: {} [{}] {} -> {} by {}",
                                entityType, entityId, fromStatus, toStatus, changedBy);
                return saved;
        }

        /**
         * Record a state transition with metadata
         */
        public StateHistory recordTransition(String entityType, String entityId,
                        String fromStatus, String toStatus,
                        String changedBy, String reason, String metadata) {
                StateHistory history = StateHistory.builder()
                                .entityType(entityType)
                                .entityId(entityId)
                                .fromStatus(fromStatus)
                                .toStatus(toStatus)
                                .changedBy(changedBy)
                                .reason(reason)
                                .metadata(metadata)
                                .build();

                return stateHistoryRepository.save(history);
        }

        /**
         * Record a state transition from a StatefulEntity
         *
         * @param entity     The StatefulEntity that changed state
         * @param fromStatus Previous status name
         * @param toStatus   New status name
         */
        public <ID extends Serializable, S extends Enum<S>> void recordFromEntity(
                        com.enterprise.common.entity.StatefulEntity<ID, S> entity,
                        String fromStatus, String toStatus) {
                recordTransition(
                                entity.getClass().getSimpleName(),
                                entity.getId().toString(),
                                fromStatus,
                                toStatus,
                                entity.getStatusChangedBy(),
                                entity.getStatusChangeReason());
        }

        /**
         * Get full state history for an entity, ordered newest first
         */
        @Transactional(readOnly = true)
        public List<StateHistory> getHistory(String entityType, String entityId) {
                return stateHistoryRepository
                                .findByEntityTypeAndEntityIdOrderByChangedAtDesc(entityType, entityId);
        }

        /**
         * Get paginated state history for an entity
         */
        @Transactional(readOnly = true)
        public Page<StateHistory> getHistory(String entityType, String entityId, Pageable pageable) {
                return stateHistoryRepository
                                .findByEntityTypeAndEntityId(entityType, entityId, pageable);
        }

        /**
         * Get the most recent state change for an entity
         */
        @Transactional(readOnly = true)
        public StateHistory getLatestChange(String entityType, String entityId) {
                return stateHistoryRepository
                                .findFirstByEntityTypeAndEntityIdOrderByChangedAtDesc(entityType, entityId);
        }

        /**
         * Get all state changes for a given entity type (e.g., all Product transitions)
         */
        @Transactional(readOnly = true)
        public Page<StateHistory> getHistoryByType(String entityType, Pageable pageable) {
                return stateHistoryRepository.findByEntityType(entityType, pageable);
        }
}
