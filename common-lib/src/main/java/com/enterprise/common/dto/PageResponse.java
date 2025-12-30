package com.enterprise.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated Response Wrapper
 * <p>
 * Standardized pagination response for all list endpoints.
 * Compatible with Spring Data Page interface.
 * <p>
 * Usage Examples:
 * <pre>
 * // From Spring Data Page
 * Page&lt;User&gt; userPage = userRepository.findAll(pageable);
 * PageResponse&lt;UserDTO&gt; response = PageResponse.from(userPage.map(userMapper::toDTO));
 *
 * // Manual construction
 * PageResponse&lt;UserDTO&gt; response = PageResponse.&lt;UserDTO&gt;builder()
 *     .content(users)
 *     .pageIndex(0)
 *     .pageSize(20)
 *     .totalElements(100)
 *     .build();
 * </pre>
 *
 * @param <T> Type of the content items
 * @author Enterprise Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response wrapper")
public class PageResponse<T> {

    @Schema(description = "List of items in current page")
    private List<T> content;

    @Schema(description = "Current page index (0-based)", example = "0")
    private int pageIndex;

    @Schema(description = "Number of items per page", example = "20")
    private int pageSize;

    @Schema(description = "Total number of items across all pages", example = "100")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;

    @Schema(description = "Is this the first page", example = "true")
    private boolean first;

    @Schema(description = "Is this the last page", example = "false")
    private boolean last;

    @Schema(description = "Does it have next page", example = "true")
    private boolean hasNext;

    @Schema(description = "Does it have previous page", example = "false")
    private boolean hasPrevious;

    @Schema(description = "Is the page empty", example = "false")
    private boolean empty;

    /**
     * Create PageResponse from Spring Data Page.
     *
     * @param page Spring Data Page object
     * @param <T>  Type of content
     * @return PageResponse with all pagination info
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageIndex(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .empty(page.isEmpty())
                .build();
    }

    /**
     * Create PageResponse with custom content (e.g., after DTO mapping).
     *
     * @param page        Spring Data Page (for pagination info)
     * @param mappedContent Transformed content list
     * @param <T>         Type of original content
     * @param <R>         Type of mapped content
     * @return PageResponse with mapped content
     */
    public static <T, R> PageResponse<R> from(Page<T> page, List<R> mappedContent) {
        return PageResponse.<R>builder()
                .content(mappedContent)
                .pageIndex(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .empty(mappedContent.isEmpty())
                .build();
    }

    /**
     * Create an empty PageResponse.
     *
     * @param <T> Type of content
     * @return Empty PageResponse
     */
    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .content(List.of())
                .pageIndex(0)
                .pageSize(0)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .empty(true)
                .build();
    }
}
