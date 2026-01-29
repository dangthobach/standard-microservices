package com.enterprise.business.query;

import com.enterprise.business.dto.ProductDTO;
import com.enterprise.business.entity.Product;
import com.enterprise.business.repository.ProductRepository;
import com.enterprise.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * List Products Query Handler
 *
 * Handles the ListProductsQuery by:
 * 1. Fetching paginated products from database
 * 2. Converting entities to DTOs
 * 3. Returning list of product DTOs
 *
 * Transaction Management:
 * - @Transactional(readOnly = true) optimizes for read-only operations
 *
 * Cache Management:
 * - No caching for paginated results (cache stampede complexity)
 * - Individual products are cached via GetProductByIdQuery
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListProductsQueryHandler implements QueryHandler<ListProductsQuery, List<ProductDTO>> {

    private final ProductRepository productRepository;

    /**
     * Handle ListProductsQuery
     *
     * @param query The query containing pagination parameters
     * @return List of ProductDTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> handle(ListProductsQuery query) {
        log.debug("Handling ListProductsQuery: page={}, size={}", query.page(), query.size());

        Pageable pageable = PageRequest.of(query.page(), query.size());
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);

        return productPage.getContent().stream()
                .map(ProductDTO::from)
                .collect(Collectors.toList());
    }
}
