package com.enterprise.iam.repository;

import com.enterprise.iam.entity.UserRequest;
import com.enterprise.iam.entity.UserRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * User Request History Repository
 * <p>
 * JPA repository for UserRequestHistory entity operations.
 * Optimized for fast history reads at 1M user scale.
 */
@Repository
public interface UserRequestHistoryRepository extends JpaRepository<UserRequestHistory, UUID> {

    /**
     * Find all history entries for a specific request
     * Ordered by creation time (oldest first)
     */
    @Query("SELECT h FROM UserRequestHistory h WHERE h.request.id = :requestId ORDER BY h.createdAt ASC")
    List<UserRequestHistory> findByRequestIdOrderByCreatedAtAsc(@Param("requestId") UUID requestId);

    /**
     * Find all history entries for a specific request
     * Ordered by creation time (newest first)
     */
    @Query("SELECT h FROM UserRequestHistory h WHERE h.request.id = :requestId ORDER BY h.createdAt DESC")
    List<UserRequestHistory> findByRequestIdOrderByCreatedAtDesc(@Param("requestId") UUID requestId);

    /**
     * Find history entries by request and action
     */
    List<UserRequestHistory> findByRequestAndActionOrderByCreatedAtDesc(UserRequest request, com.enterprise.iam.entity.UserRequestAction action);
}

