# API Response Patterns & Distributed Tracing

Hướng dẫn sử dụng cấu trúc response chuẩn hóa và distributed tracing cho toàn bộ hệ thống microservices.

## Mục lục
1. [Tổng quan](#tổng-quan)
2. [ApiResponse - Cấu trúc Response Chuẩn](#apiresponse---cấu-trúc-response-chuẩn)
3. [PageResponse - Pagination](#pageresponse---pagination)
4. [Distributed Tracing](#distributed-tracing)
5. [Exception Handling](#exception-handling)
6. [Best Practices](#best-practices)
7. [Ví dụ thực tế](#ví-dụ-thực-tế)

---

## Tổng quan

### Các thành phần chính

```
┌─────────────────────────────────────────────────────────────────┐
│                     Request Flow with Tracing                   │
└─────────────────────────────────────────────────────────────────┘

Client Request
    ↓
[RequestLoggingFilter] ← Tự động log request với traceId/spanId
    ↓
[Controller] ← Sử dụng ApiResponse, PageResponse
    ↓
[GlobalResponseBodyAdvice] ← Tự động thêm traceId/spanId vào response
    ↓
[GlobalExceptionHandler] ← Tự động convert exceptions → ApiResponse
    ↓
Client Response (với traceId, spanId)
```

### Tính năng

- ✅ **Chuẩn hóa response**: Tất cả API đều trả về cấu trúc giống nhau
- ✅ **Distributed tracing**: Tự động thêm `traceId`, `spanId` vào mọi response
- ✅ **Error handling**: Tự động convert exceptions thành ApiResponse
- ✅ **Pagination**: Cấu trúc chuẩn cho kết quả phân trang
- ✅ **Logging**: Tự động log request/response với trace context
- ✅ **Swagger**: Tích hợp sẵn với OpenAPI documentation

---

## ApiResponse - Cấu trúc Response Chuẩn

### Cấu trúc

```java
public class ApiResponse<T> {
    private boolean success;        // true nếu thành công
    private String message;         // Thông báo cho user
    private T data;                 // Dữ liệu trả về
    private String errorCode;       // Mã lỗi (nếu có)
    private ErrorDetails error;     // Chi tiết lỗi (nếu có)
    private String traceId;         // Trace ID (tự động)
    private String spanId;          // Span ID (tự động)
    private Instant timestamp;      // Thời điểm response
}
```

### Success Response

```json
{
  "success": true,
  "message": "User retrieved successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.doe@enterprise.com",
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe",
    "enabled": true
  },
  "traceId": "5f9c8a7b6d4e3f2a1b0c9d8e",
  "spanId": "1a2b3c4d5e6f7g8h",
  "timestamp": "2025-12-31T00:15:30.123Z"
}
```

### Error Response

```json
{
  "success": false,
  "message": "User not found",
  "errorCode": "USER_NOT_FOUND",
  "error": {
    "code": "USER_NOT_FOUND",
    "detail": "No user found with ID: 550e8400-e29b-41d4-a716-446655440000"
  },
  "traceId": "5f9c8a7b6d4e3f2a1b0c9d8e",
  "spanId": "1a2b3c4d5e6f7g8h",
  "timestamp": "2025-12-31T00:15:30.123Z"
}
```

### Validation Error Response

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "data": {
    "email": "Email is required",
    "firstName": "First name must be between 2 and 100 characters"
  },
  "error": {
    "code": "VALIDATION_ERROR",
    "detail": "Validation failed",
    "metadata": {
      "errors": {
        "email": "Email is required",
        "firstName": "First name must be between 2 and 100 characters"
      }
    }
  },
  "traceId": "5f9c8a7b6d4e3f2a1b0c9d8e",
  "spanId": "1a2b3c4d5e6f7g8h",
  "timestamp": "2025-12-31T00:15:30.123Z"
}
```

---

## PageResponse - Pagination

### Cấu trúc

```java
public class PageResponse<T> {
    private List<T> content;           // Danh sách items
    private int pageIndex;             // Trang hiện tại (0-based)
    private int pageSize;              // Số items mỗi trang
    private long totalElements;        // Tổng số items
    private int totalPages;            // Tổng số trang
    private boolean first;             // Có phải trang đầu?
    private boolean last;              // Có phải trang cuối?
    private boolean hasNext;           // Có trang tiếp theo?
    private boolean hasPrevious;       // Có trang trước?
    private boolean empty;             // Trang rỗng?
}
```

### Ví dụ Response

```json
{
  "success": true,
  "message": "Users retrieved successfully",
  "data": {
    "content": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "email": "john.doe@enterprise.com",
        "fullName": "John Doe"
      },
      {
        "id": "660e8400-e29b-41d4-a716-446655440001",
        "email": "jane.smith@enterprise.com",
        "fullName": "Jane Smith"
      }
    ],
    "pageIndex": 0,
    "pageSize": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false,
    "empty": false
  },
  "traceId": "5f9c8a7b6d4e3f2a1b0c9d8e",
  "spanId": "1a2b3c4d5e6f7g8h",
  "timestamp": "2025-12-31T00:15:30.123Z"
}
```

---

## Distributed Tracing

### Trace ID & Span ID

- **Trace ID**: ID duy nhất cho toàn bộ request journey qua nhiều services
- **Span ID**: ID duy nhất cho mỗi operation trong service

```
Client → Gateway → IAM Service → Database
         [Trace: 5f9c8a7b]
           [Span: 1a2b]  [Span: 3c4d]  [Span: 5e6f]
```

### Tự động thêm vào Response

**GlobalResponseBodyAdvice** tự động thêm `traceId` và `spanId` vào mọi `ApiResponse`:

```java
// Controller code
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable UUID id) {
    UserDTO user = userService.getUser(id);

    // Không cần thêm traceId/spanId thủ công!
    // GlobalResponseBodyAdvice sẽ tự động thêm
    return ResponseEntity.ok(ApiResponse.success(user));
}
```

### Manual Tracing (nếu cần)

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable UUID id) {
    UserDTO user = userService.getUser(id);

    // Thêm tracing thủ công (optional)
    return ResponseEntity.ok(
        ApiResponse.success(user)
            .withTracing(
                TracingUtil.getTraceIdWithFallback(tracer),
                TracingUtil.getSpanIdWithFallback(tracer)
            )
    );
}
```

### Request/Response Logging

**RequestLoggingFilter** tự động log tất cả requests/responses với trace context:

```
2025-12-31 00:15:30 INFO  → Incoming Request | Method: GET | URI: /api/users/550e8400 | TraceId: 5f9c8a7b | SpanId: 1a2b | RemoteAddr: 192.168.1.100
2025-12-31 00:15:30 INFO  ← Outgoing Response | Status: 200 (Success) | Duration: 45ms | TraceId: 5f9c8a7b | SpanId: 1a2b
```

---

## Exception Handling

### Tự động xử lý Exceptions

**GlobalExceptionHandler** tự động convert tất cả exceptions thành `ApiResponse`:

| Exception | HTTP Status | Error Code | Mô tả |
|-----------|-------------|------------|-------|
| `BusinessException` | 400 | Custom | Business logic errors |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` | @Valid validation errors |
| `ConstraintViolationException` | 400 | `CONSTRAINT_VIOLATION` | JPA constraint violations |
| `IllegalArgumentException` | 400 | `ILLEGAL_ARGUMENT` | Invalid arguments |
| `AuthenticationException` | 401 | `AUTHENTICATION_FAILED` | Authentication errors |
| `AccessDeniedException` | 403 | `ACCESS_DENIED` | Authorization errors |
| `Exception` | 500 | `INTERNAL_SERVER_ERROR` | Unexpected errors |

### Ví dụ

```java
// Controller
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable UUID id) {
    // Không cần try-catch!
    // GlobalExceptionHandler sẽ tự động xử lý exception
    User user = userRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    return ResponseEntity.ok(ApiResponse.success(convertToDTO(user)));
}

// Nếu throw exception, response tự động:
{
  "success": false,
  "message": "User not found",
  "errorCode": "ILLEGAL_ARGUMENT",
  "traceId": "5f9c8a7b",
  "spanId": "1a2b",
  "timestamp": "2025-12-31T00:15:30.123Z"
}
```

---

## Best Practices

### 1. Luôn sử dụng ApiResponse

✅ **Đúng**:
```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable UUID id) {
    return ResponseEntity.ok(
        ApiResponse.success("User retrieved successfully", userDTO)
    );
}
```

❌ **Sai**:
```java
@GetMapping("/{id}")
public ResponseEntity<UserDTO> getUser(@PathVariable UUID id) {
    return ResponseEntity.ok(userDTO);  // Không có traceId, timestamp, etc.
}
```

### 2. Sử dụng Factory Methods

```java
// Success responses
ApiResponse.success(data)
ApiResponse.success(message, data)
ApiResponse.success(message)

// Error responses
ApiResponse.error(message)
ApiResponse.error(message, errorCode)
ApiResponse.error(message, errorCode, data)
ApiResponse.error(message, errorDetails)
```

### 3. Pagination với PageResponse

✅ **Đúng**:
```java
@GetMapping
public ResponseEntity<ApiResponse<PageResponse<UserDTO>>> getUsers(Pageable pageable) {
    Page<User> userPage = userRepository.findAll(pageable);
    List<UserDTO> dtos = userPage.map(this::toDTO).getContent();
    PageResponse<UserDTO> pageResponse = PageResponse.from(userPage, dtos);

    return ResponseEntity.ok(
        ApiResponse.success("Users retrieved successfully", pageResponse)
    );
}
```

### 4. Không cần try-catch cho validation

```java
// GlobalExceptionHandler tự động xử lý
@PostMapping
public ResponseEntity<ApiResponse<UserDTO>> createUser(
    @Valid @RequestBody CreateUserRequest request  // @Valid sẽ trigger validation
) {
    UserDTO user = userService.createUser(request);
    return ResponseEntity.ok(ApiResponse.success("User created", user));
}
```

### 5. Logging với Trace Context

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable UUID id) {
    // Log sẽ tự động có traceId trong MDC
    log.info("Fetching user by ID: {}", id);
    // Output: Fetching user by ID: 550e8400 | traceId: 5f9c8a7b

    UserDTO user = userService.getUser(id);
    return ResponseEntity.ok(ApiResponse.success(user));
}
```

---

## Ví dụ thực tế

### Example 1: Simple CRUD

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // GET single item
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProduct(@PathVariable UUID id) {
        ProductDTO product = productService.getById(id);
        return ResponseEntity.ok(
            ApiResponse.success("Product retrieved successfully", product)
        );
    }

    // GET list with pagination
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductDTO>>> getProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productService.findAll(pageable);

        List<ProductDTO> dtos = productPage.map(this::toDTO).getContent();
        PageResponse<ProductDTO> pageResponse = PageResponse.from(productPage, dtos);

        return ResponseEntity.ok(
            ApiResponse.success("Products retrieved successfully", pageResponse)
        );
    }

    // POST create
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
        @Valid @RequestBody CreateProductRequest request
    ) {
        ProductDTO product = productService.create(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Product created successfully", product));
    }

    // PUT update
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductDTO product = productService.update(id, request);
        return ResponseEntity.ok(
            ApiResponse.success("Product updated successfully", product)
        );
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.ok(
            ApiResponse.success("Product deleted successfully")
        );
    }
}
```

### Example 2: Custom Error Handling

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderDTO placeOrder(PlaceOrderRequest request) {
        // Business validation
        if (request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                "INVALID_AMOUNT",
                "Order amount must be greater than zero",
                Map.of("amount", request.getTotalAmount())
            );
        }

        // Inventory check
        if (!inventoryService.hasStock(request.getProductId(), request.getQuantity())) {
            throw new BusinessException(
                "OUT_OF_STOCK",
                "Product is out of stock",
                Map.of("productId", request.getProductId(), "requested", request.getQuantity())
            );
        }

        Order order = createOrder(request);
        return toDTO(order);
    }
}

// Response khi throw BusinessException:
{
  "success": false,
  "message": "Product is out of stock",
  "errorCode": "OUT_OF_STOCK",
  "error": {
    "code": "OUT_OF_STOCK",
    "detail": "Product is out of stock",
    "metadata": {
      "details": {
        "productId": "abc-123",
        "requested": 10
      }
    }
  },
  "traceId": "5f9c8a7b",
  "spanId": "1a2b",
  "timestamp": "2025-12-31T00:15:30.123Z"
}
```

### Example 3: Search với Custom Response

```java
@GetMapping("/search")
public ResponseEntity<ApiResponse<SearchResult>> searchProducts(
    @RequestParam String keyword,
    @RequestParam(required = false) String category,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Product> results = productService.search(keyword, category, pageable);

    SearchResult searchResult = SearchResult.builder()
        .keyword(keyword)
        .category(category)
        .results(PageResponse.from(results.map(this::toDTO)))
        .totalFound(results.getTotalElements())
        .searchTime(System.currentTimeMillis() - startTime)
        .build();

    return ResponseEntity.ok(
        ApiResponse.success("Search completed successfully", searchResult)
    );
}
```

---

## Monitoring & Debugging

### 1. Sử dụng Trace ID để debug

```bash
# Tìm tất cả logs cho một request
grep "5f9c8a7b" application.log

# Kết quả:
2025-12-31 00:15:30 INFO  → Incoming Request | TraceId: 5f9c8a7b
2025-12-31 00:15:30 INFO  Fetching user by ID: 550e8400 | TraceId: 5f9c8a7b
2025-12-31 00:15:30 INFO  Database query executed in 25ms | TraceId: 5f9c8a7b
2025-12-31 00:15:30 INFO  ← Outgoing Response | Duration: 45ms | TraceId: 5f9c8a7b
```

### 2. Zipkin Integration

Tất cả traces tự động được gửi đến Zipkin để visualize:

```
http://localhost:9411
```

### 3. Prometheus Metrics

```
# Request duration by endpoint
http_server_requests_seconds_sum{uri="/api/users/{id}"}

# Error rate
http_server_requests_seconds_count{status="500"}
```

---

## Tổng kết

### Checklist cho mỗi Controller

- ✅ Tất cả endpoints trả về `ApiResponse<T>`
- ✅ Pagination endpoints sử dụng `PageResponse<T>`
- ✅ Sử dụng `@Valid` cho request validation
- ✅ Sử dụng `log.info()` với business context
- ✅ Không cần thêm `traceId`/`spanId` thủ công
- ✅ Không cần try-catch cho exception handling
- ✅ Sử dụng factory methods của `ApiResponse`

### Lợi ích

1. **Nhất quán**: Tất cả APIs có cấu trúc giống nhau
2. **Traceability**: Mọi request đều có trace ID để debug
3. **Monitoring**: Tự động metrics và logging
4. **Developer Experience**: Ít boilerplate code hơn
5. **Client Experience**: Response chuẩn, dễ parse
6. **Production Ready**: Built-in error handling và tracing

🎉 **Hệ thống đã sẵn sàng cho production!**
