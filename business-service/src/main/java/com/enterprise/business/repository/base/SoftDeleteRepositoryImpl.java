package com.enterprise.business.repository.base;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.hibernate.Session;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SoftDeleteRepositoryImpl<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID>
        implements SoftDeleteQueryRepository<T, ID> {

    private final EntityManager em;
    private final Class<T> domainClass;

    public SoftDeleteRepositoryImpl(JpaEntityInformation<T, ?> entityInfo, EntityManager em) {
        super(entityInfo, em);
        this.em = em;
        this.domainClass = entityInfo.getJavaType();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findByIdIncludeDeleted(ID id) {
        String tableName = getTableName();
        List<T> result = em.createNativeQuery(
                "SELECT * FROM " + tableName + " WHERE id = :id", domainClass)
                .setParameter("id", id)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAllDeleted() {
        String tableName = getTableName();
        return em.createNativeQuery(
                "SELECT * FROM " + tableName + " WHERE deleted = true", domainClass)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAllDeleted(Pageable pageable) {
        String tableName = getTableName();
        long total = countDeleted();

        String orderClause = buildOrderClause(pageable);

        List<T> content = em.createNativeQuery(
                "SELECT * FROM " + tableName + " WHERE deleted = true " + orderClause + " LIMIT :limit OFFSET :offset",
                domainClass)
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", pageable.getOffset())
                .getResultList();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Subclasses có thể override để expand whitelist cho entity-specific fields.
     * SECURITY: Chỉ whitelist static field names từ code, KHÔNG nhận từ user input.
     */
    protected Set<String> getAllowedSortFields() {
        return Set.of("created_at", "updated_at", "deleted_at");
    }

    private String buildOrderClause(Pageable pageable) {
        if (!pageable.getSort().isSorted())
            return "ORDER BY deleted_at DESC NULLS LAST";

        String orderClause = pageable.getSort().stream()
                .filter(order -> getAllowedSortFields().contains(order.getProperty()))
                .map(order -> order.getProperty() + " " + order.getDirection().name())
                .collect(Collectors.joining(", "));

        return orderClause.isBlank() ? "ORDER BY deleted_at DESC NULLS LAST" : "ORDER BY " + orderClause;
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAllByIdIncludeDeleted(List<ID> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();
        String tableName = getTableName();
        return em.createNativeQuery(
                "SELECT * FROM " + tableName + " WHERE id IN :ids", domainClass)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDeleted() {
        String tableName = getTableName();
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM " + tableName + " WHERE deleted = true")
                .getSingleResult()).longValue();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive() {
        String tableName = getTableName();
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM " + tableName + " WHERE deleted = false")
                .getSingleResult()).longValue();
    }

    private String getTableName() {
        Class<?> clazz = domainClass;
        while (clazz != null) {
            Table tableAnnotation = clazz.getAnnotation(Table.class);
            if (tableAnnotation != null && !tableAnnotation.name().isBlank()) {
                String schema = tableAnnotation.schema();
                return schema.isBlank()
                        ? tableAnnotation.name()
                        : schema + "." + tableAnnotation.name();
            }
            clazz = clazz.getSuperclass();
        }
        return toSnakeCase(domainClass.getSimpleName()) + "s";
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
