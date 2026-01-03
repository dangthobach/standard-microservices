package com.enterprise.iam.repository;

import com.enterprise.iam.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Role Repository
 * <p>
 * JPA repository for Role entity operations.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find role by name
     */
    Optional<Role> findByName(String name);
}

