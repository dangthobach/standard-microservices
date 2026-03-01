package com.enterprise.business.repository.base;

import org.springframework.data.repository.NoRepositoryBean;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface SoftDeleteQueryRepository<T, ID> {
    Optional<T> findByIdIncludeDeleted(ID id);

    List<T> findAllDeleted();

    Page<T> findAllDeleted(Pageable pageable);

    List<T> findAllByIdIncludeDeleted(List<ID> ids);

    long countDeleted();

    long countActive();
}
