package com.enterprise.business.service;

import com.enterprise.business.entity.Product;
import com.enterprise.business.entity.ProductHistory;
import com.enterprise.business.entity.ProductStatus;
import com.enterprise.business.repository.ProductHistoryRepository;
import com.enterprise.business.repository.ProductRepository;
import com.enterprise.business.service.base.BaseStatefulService;
import com.enterprise.business.util.JsonDiffUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class ProductStateService
        extends BaseStatefulService<Product, ProductHistory, ProductStatus, ProductRepository> {

    public ProductStateService(ProductRepository repository,
            ProductHistoryRepository historyRepository,
            ObjectMapper objectMapper,
            JsonDiffUtil jsonDiffUtil) {
        super(repository, historyRepository, objectMapper, jsonDiffUtil);
    }

    @Override
    protected ProductStatus getInitialStatus() {
        return ProductStatus.DRAFT;
    }
}
