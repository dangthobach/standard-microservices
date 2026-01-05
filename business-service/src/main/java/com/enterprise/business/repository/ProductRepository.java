package com.enterprise.business.repository;

import com.enterprise.business.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    Page<Product> findByCategoryAndActiveTrue(String category, Pageable pageable);

    Page<Product> findByActiveTrue(Pageable pageable);
}
