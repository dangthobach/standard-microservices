package com.enterprise.business.service.base;

import com.enterprise.business.entity.base.HistoryEntity;
import com.enterprise.business.entity.base.StatefulEntity;
import com.enterprise.business.repository.base.SoftDeleteRepository;
import com.enterprise.business.util.JsonDiffUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Base service cho stateful entities
 * Đảm bảo entity + history luôn được save trong cùng transaction
 */
@Slf4j
@Transactional
public abstract class BaseStatefulService<E extends StatefulEntity<S, H>, H extends HistoryEntity, S extends Enum<S>, R extends SoftDeleteRepository<E, UUID>> {

    protected final R repository;
    protected final JpaRepository<H, UUID> historyRepository;
    protected final ObjectMapper objectMapper;
    protected final JsonDiffUtil jsonDiffUtil;

    protected BaseStatefulService(R repository,
            JpaRepository<H, UUID> historyRepository,
            ObjectMapper objectMapper,
            JsonDiffUtil jsonDiffUtil) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
        this.jsonDiffUtil = jsonDiffUtil;
    }

    protected abstract S getInitialStatus();

    protected String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            log.warn(
                    "[BaseStatefulService] No authenticated user in SecurityContext, defaulting to 'system'. Check SecurityContext propagation.");
            return "system";
        }
        return auth.getName();
    }

    public E create(E entity, String correlationId) {
        return create(entity, getCurrentUser(), correlationId);
    }

    public E create(E entity, String createdBy, String correlationId) {
        entity.setStatus(getInitialStatus());

        E saved = repository.save(entity);
        String currentSnapshot = toJson(saved);

        H history = saved.createHistorySnapshot(
                "CREATE", null, createdBy, currentSnapshot, null, correlationId);
        historyRepository.save(history);
        return saved;
    }

    public E transitionTo(E entity, S newStatus, String correlationId) {
        return transitionTo(entity, newStatus, getCurrentUser(), correlationId);
    }

    public E transitionTo(E entity, S newStatus, String changedBy, String correlationId) {
        String previousSnapshot = toJson(entity);
        S previousStatus = entity.getStatus();

        entity.transitionTo(newStatus);

        E saved = repository.save(entity);
        String currentSnapshot = toJson(saved);
        String diff = jsonDiffUtil.computeDiffJsonString(previousSnapshot, currentSnapshot);

        H history = saved.createHistorySnapshot(
                "UPDATE", previousStatus, changedBy, currentSnapshot, diff, correlationId);
        historyRepository.save(history);

        return saved;
    }

    public E softDelete(E entity, String correlationId) {
        return softDelete(entity, getCurrentUser(), correlationId);
    }

    public E softDelete(E entity, String deletedBy, String correlationId) {
        String previousSnapshot = toJson(entity);
        S previousStatus = entity.getStatus();

        entity.softDelete(deletedBy);

        E saved = repository.save(entity);
        String currentSnapshot = toJson(saved);
        String diff = jsonDiffUtil.computeDiffJsonString(previousSnapshot, currentSnapshot);

        H history = saved.createHistorySnapshot(
                "DELETE", previousStatus, deletedBy, currentSnapshot, diff, correlationId);
        historyRepository.save(history);

        return saved;
    }

    public E restore(E entity, String correlationId) {
        return restore(entity, getCurrentUser(), correlationId);
    }

    public E restore(E entity, String restoredBy, String correlationId) {
        String previousSnapshot = toJson(entity);
        S previousStatus = entity.getStatus();

        entity.restore();

        E saved = repository.save(entity);
        String currentSnapshot = toJson(saved);
        String diff = jsonDiffUtil.computeDiffJsonString(previousSnapshot, currentSnapshot);

        H history = saved.createHistorySnapshot(
                "RESTORE", previousStatus, restoredBy, currentSnapshot, diff, correlationId);
        historyRepository.save(history);

        return saved;
    }

    public E saveWithHistory(E entity, String action, String correlationId) {
        return saveWithHistory(entity, action, getCurrentUser(), correlationId);
    }

    /**
     * Generic save với history — dùng cho các UPDATE không liên quan đến state
     * transition.
     * 
     * IMPORTANT: Snapshot được chụp tại thời điểm gọi method này.
     * Caller phải đảm bảo entity đã được modify TRƯỚC khi gọi method này.
     * Không dùng method này cho: CREATE (dùng create()), state transition (dùng
     * transitionTo()),
     * soft delete (dùng softDelete()), restore (dùng restore()).
     */
    public E saveWithHistory(E entity, String action, String changedBy, String correlationId) {
        String previousSnapshot = entity.getId() != null ? toJson(entity) : null;
        S previousStatus = entity.getStatus();

        E saved = repository.save(entity);
        String currentSnapshot = toJson(saved);
        String diff = (previousSnapshot != null)
                ? jsonDiffUtil.computeDiffJsonString(previousSnapshot, currentSnapshot)
                : null;

        H history = saved.createHistorySnapshot(
                action, previousStatus, changedBy, currentSnapshot, diff, correlationId);
        historyRepository.save(history);

        return saved;
    }

    protected String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot serialize entity for history snapshot: " + obj.getClass().getSimpleName(), e);
        }
    }
}
