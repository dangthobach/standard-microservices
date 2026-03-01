package com.enterprise.business.repository.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Base repository interface with soft delete support
 * For entities extending SoftDeleteEntity
 *
 * IMPORTANT: @SQLRestriction on SoftDeleteEntity automatically filters deleted
 * records
 * - findAll(), findById() etc. will ONLY return deleted = false
 * - Use xxxIncludeDeleted() methods for admin/audit access to deleted records
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T, ID>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T>, SoftDeleteQueryRepository<T, ID> {

    // NOTE: findAll(), findById() automatically filter deleted=false via
    // @SQLRestriction
    // No need to override them here

    @Modifying(clearAutomatically = true)
    @Query("UPDATE #{#entityName} e SET e.deleted = true, e.deletedBy = :deletedBy, " +
            "e.deletedAt = CURRENT_TIMESTAMP, e.updatedAt = CURRENT_TIMESTAMP, e.updatedBy = :deletedBy " +
            "WHERE e.id = :id AND e.deleted = false")
    int softDeleteById(@Param("id") ID id, @Param("deletedBy") String deletedBy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE #{#entityName} e SET e.deleted = true, e.deletedBy = :deletedBy, " +
            "e.deletedAt = CURRENT_TIMESTAMP, e.updatedAt = CURRENT_TIMESTAMP, e.updatedBy = :deletedBy " +
            "WHERE e.id IN :ids AND e.deleted = false")
    int softDeleteAllByIds(@Param("ids") List<ID> ids, @Param("deletedBy") String deletedBy);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE #{#entityName} e SET e.deleted = true, e.deletedBy = :deletedBy, " +
            "e.deletedAt = CURRENT_TIMESTAMP, e.updatedAt = CURRENT_TIMESTAMP, e.updatedBy = :deletedBy, " +
            "e.version = e.version + 1 " +
            "WHERE e.id = :id AND e.version = :version AND e.deleted = false")
    int softDeleteByIdWithVersion(@Param("id") ID id,
            @Param("deletedBy") String deletedBy,
            @Param("version") Long version);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE #{#entityName} e SET e.deleted = false, e.deletedBy = null, " +
            "e.deletedAt = null, e.updatedAt = CURRENT_TIMESTAMP, e.updatedBy = :restoredBy, " +
            "e.version = e.version + 1 " +
            "WHERE e.id = :id AND e.deleted = true")
    int restoreById(@Param("id") ID id, @Param("restoredBy") String restoredBy);
}
