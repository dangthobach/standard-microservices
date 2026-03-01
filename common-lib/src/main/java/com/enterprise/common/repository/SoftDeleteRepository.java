package com.enterprise.common.repository;

import com.enterprise.common.entity.SoftDeletableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Base Repository for Soft-Deletable Entities
 *
 * Provides:
 * - Standard CRUD operations (auto-filtered by @SQLRestriction for non-deleted
 * only)
 * - Explicit methods to query deleted records (bypasses @SQLRestriction via
 * native query)
 * - Bulk soft-delete operations
 *
 * Note: Since SoftDeletableEntity has @SQLRestriction("deleted = false"):
 * - findAll(), findById() etc. automatically exclude soft-deleted records
 * - Use findIncludingDeleted*() methods to access soft-deleted records
 *
 * @param <T>  Entity type extending SoftDeletableEntity
 * @param <ID> Entity ID type
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T extends SoftDeletableEntity<ID>, ID extends Serializable>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /**
     * Find entity by ID including soft-deleted records
     * Uses native query to bypass @SQLRestriction
     */
    @Query(value = "SELECT * FROM #{#entityName} WHERE id = :id", nativeQuery = true)
    Optional<T> findByIdIncludingDeleted(@Param("id") ID id);

    /**
     * Find all entities including soft-deleted records
     */
    @Query(value = "SELECT * FROM #{#entityName}", nativeQuery = true)
    List<T> findAllIncludingDeleted();

    /**
     * Count only non-deleted entities
     * (default count already filters via @SQLRestriction, but explicit for clarity)
     */
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.deleted = false")
    long countActive();

    /**
     * Count only soft-deleted entities
     */
    @Query(value = "SELECT COUNT(*) FROM #{#entityName} WHERE deleted = true", nativeQuery = true)
    long countDeleted();
}
