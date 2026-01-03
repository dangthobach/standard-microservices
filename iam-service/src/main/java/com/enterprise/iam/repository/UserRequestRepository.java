package com.enterprise.iam.repository;

import com.enterprise.iam.entity.UserRequest;
import com.enterprise.iam.entity.UserRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Request Repository
 * <p>
 * JPA repository for UserRequest entity operations.
 * Optimized queries for high-performance dashboard filtering at 1M user scale.
 */
@Repository
public interface UserRequestRepository extends JpaRepository<UserRequest, UUID> {

    /**
     * Find user request by ID (excluding soft-deleted)
     */
    @Query("SELECT ur FROM UserRequest ur WHERE ur.id = :id AND ur.deleted = false")
    Optional<UserRequest> findByIdAndNotDeleted(@Param("id") UUID id);

    /**
     * Find all requests by status (excluding soft-deleted)
     */
    Page<UserRequest> findByStatusAndDeletedFalse(UserRequestStatus status, Pageable pageable);

    /**
     * Find all requests by creator ID (excluding soft-deleted)
     */
    Page<UserRequest> findByRequestCreatorIdAndDeletedFalse(String requestCreatorId, Pageable pageable);

    /**
     * Find all requests by status and creator ID (excluding soft-deleted)
     */
    Page<UserRequest> findByStatusAndRequestCreatorIdAndDeletedFalse(
            UserRequestStatus status,
            String requestCreatorId,
            Pageable pageable
    );

    /**
     * Find all requests waiting for approval (excluding soft-deleted)
     * Used by Checker dashboard
     */
    @Query("SELECT ur FROM UserRequest ur WHERE ur.status = :status AND ur.deleted = false ORDER BY ur.statusChangedAt ASC")
    List<UserRequest> findWaitingForApproval(@Param("status") UserRequestStatus status);

    /**
     * Count requests by status (excluding soft-deleted)
     */
    long countByStatusAndDeletedFalse(UserRequestStatus status);

    /**
     * Check if email already exists in any request (excluding soft-deleted)
     */
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * Find request by email (excluding soft-deleted)
     */
    Optional<UserRequest> findByEmailAndDeletedFalse(String email);
}

