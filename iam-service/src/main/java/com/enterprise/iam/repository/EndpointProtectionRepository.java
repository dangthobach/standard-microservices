package com.enterprise.iam.repository;

import com.enterprise.iam.entity.EndpointProtection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EndpointProtectionRepository extends JpaRepository<EndpointProtection, UUID> {

    /**
     * Find all active rules sorted by priority (High to Low).
     * Gateway uses this list to build the Matcher Chain.
     */
    List<EndpointProtection> findByActiveTrueAndDeletedFalseOrderByPriorityDesc();
}
