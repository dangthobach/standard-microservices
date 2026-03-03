package com.enterprise.business.repository;

import com.enterprise.business.entity.ProductHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Product History Repository
 */
@Repository
public interface ProductHistoryRepository extends JpaRepository<ProductHistory, UUID> {
}
