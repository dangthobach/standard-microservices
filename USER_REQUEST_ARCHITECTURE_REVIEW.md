# Đánh Giá Kiến Trúc UserRequest

## Tổng Quan

Báo cáo này đánh giá xem các chức năng UserRequest có đáp ứng đúng kiến trúc backend, pattern, và history pattern của base backend hay không.

---

## ✅ ĐÁT YÊU CẦU

### 1. Entity Base Classes

#### UserRequest Entity
- ✅ **Đúng**: Extends `StatefulEntity<UUID, UserRequestStatus>`
- ✅ **Đúng**: `StatefulEntity` → `SoftDeletableEntity` → `AuditableEntity` → `BaseEntity`
- ✅ **Đúng**: Có đầy đủ các fields từ base classes:
  - `id`, `version` (từ BaseEntity)
  - `createdBy`, `createdAt`, `updatedBy`, `updatedAt` (từ AuditableEntity)
  - `deleted`, `deletedBy`, `deletedAt` (từ SoftDeletableEntity)
  - `status`, `previousStatus`, `statusChangedAt`, `statusChangedBy`, `statusChangeReason` (từ StatefulEntity)

#### UserRequestHistory Entity
- ✅ **Đúng**: Extends `AuditableEntity<UUID>`
- ✅ **Đúng**: Có đầy đủ audit fields từ base class

### 2. History Pattern

#### UserRequestHistory Implementation
- ✅ **Đúng**: Separate entity (không dùng Hibernate Envers)
- ✅ **Đúng**: Reference ID (`request_id` foreign key)
- ✅ **Đúng**: Action enum (`UserRequestAction`: CREATE, UPDATE, SUBMIT, APPROVE, REJECT)
- ✅ **Đúng**: State transitions (`oldStatus`, `newStatus`)
- ✅ **Đúng**: Metadata JSONB column cho flexible snapshots
- ✅ **Đúng**: Actor tracking (`actorId`)
- ✅ **Đúng**: Comment field cho context
- ✅ **Đúng**: Indexes tối ưu:
  - `idx_history_request_id` trên `request_id`
  - `idx_history_action` trên `action`
  - `idx_history_actor_id` trên `actor_id`
  - `idx_history_created_at` trên `created_at`

---

## ❌ CHƯA ĐÁT YÊU CẦU

### 1. CQRS Pattern - **VI PHẠM NGHIÊM TRỌNG**

#### Vấn Đề Hiện Tại

**Controller đang inject Service trực tiếp:**
```java
@RestController
public class UserRequestController {
    private final UserRequestService userRequestService; // ❌ SAI
    // ...
}
```

**Theo user rules, Controller PHẢI inject CommandBus và QueryBus:**
```java
@RestController
public class UserRequestController {
    private final CommandBus commandBus; // ✅ ĐÚNG
    private final QueryBus queryBus;     // ✅ ĐÚNG
    // ...
}
```

#### Thiếu Các Thành Phần CQRS

1. **Commands (Write Operations)** - CHƯA CÓ:
   - `CreateUserRequestCommand`
   - `UpdateUserRequestCommand`
   - `SubmitUserRequestCommand`
   - `ApproveUserRequestCommand`
   - `RejectUserRequestCommand`
   - `BatchApproveUserRequestCommand`
   - `BatchRejectUserRequestCommand`

2. **Queries (Read Operations)** - CHƯA CÓ:
   - `GetUserRequestByIdQuery`
   - `GetAllUserRequestsQuery`
   - `GetUserRequestsByStatusQuery`
   - `GetUserRequestHistoryQuery`

3. **Command Handlers** - CHƯA CÓ:
   - `CreateUserRequestCommandHandler`
   - `UpdateUserRequestCommandHandler`
   - `SubmitUserRequestCommandHandler`
   - `ApproveUserRequestCommandHandler`
   - `RejectUserRequestCommandHandler`
   - `BatchApproveUserRequestCommandHandler`
   - `BatchRejectUserRequestCommandHandler`

4. **Query Handlers** - CHƯA CÓ:
   - `GetUserRequestByIdQueryHandler`
   - `GetAllUserRequestsQueryHandler`
   - `GetUserRequestsByStatusQueryHandler`
   - `GetUserRequestHistoryQueryHandler`

#### So Sánh Với Implementation Đúng

**Ví dụ đúng từ codebase (CreateUserCommand):**
```java
// Command
public record CreateUserCommand(...) implements Command<UUID> {}

// Handler
@Service
@Transactional
public class CreateUserCommandHandler implements CommandHandler<CreateUserCommand, UUID> {
    @Override
    public UUID handle(CreateUserCommand command) { ... }
}

// Controller
@RestController
public class UserController {
    private final CommandBus commandBus;
    
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UUID>> createUser(...) {
        CreateUserCommand command = new CreateUserCommand(...);
        UUID userId = commandBus.dispatch(command);
        return ResponseEntity.ok(ApiResponse.success(userId));
    }
}
```

**UserRequest hiện tại (SAI):**
```java
// Controller
@RestController
public class UserRequestController {
    private final UserRequestService userRequestService; // ❌ Inject Service
    
    @PostMapping
    public ResponseEntity<ApiResponse<UserRequestDTO>> createRequest(...) {
        UserRequest request = userRequestService.createRequest(...); // ❌ Gọi Service trực tiếp
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(request)));
    }
}
```

---

## 📋 TÓM TẮT ĐÁNH GIÁ

| Tiêu Chí | Trạng Thái | Ghi Chú |
|----------|-----------|---------|
| **Entity Base Classes** | ✅ ĐẠT | UserRequest và UserRequestHistory đều extend đúng base classes |
| **History Pattern** | ✅ ĐẠT | UserRequestHistory tuân thủ đúng pattern với separate entity, indexes, metadata |
| **CQRS Pattern** | ❌ **KHÔNG ĐẠT** | Controller inject Service thay vì CommandBus/QueryBus, thiếu Commands/Queries/Handlers |

---

## 🔧 KHUYẾN NGHỊ

### Ưu Tiên Cao (Bắt Buộc)

1. **Refactor sang CQRS Pattern:**
   - Tạo tất cả Commands cho write operations
   - Tạo tất cả Queries cho read operations
   - Tạo tất cả CommandHandlers và QueryHandlers
   - Refactor Controller để inject CommandBus và QueryBus
   - Di chuyển logic từ UserRequestService vào các Handlers

2. **Xóa UserRequestService:**
   - Sau khi refactor sang CQRS, Service layer không còn cần thiết
   - Logic business sẽ nằm trong các Handlers

### Cấu Trúc Đề Xuất

```
iam-service/src/main/java/com/enterprise/iam/
├── command/
│   ├── CreateUserRequestCommand.java
│   ├── CreateUserRequestCommandHandler.java
│   ├── UpdateUserRequestCommand.java
│   ├── UpdateUserRequestCommandHandler.java
│   ├── SubmitUserRequestCommand.java
│   ├── SubmitUserRequestCommandHandler.java
│   ├── ApproveUserRequestCommand.java
│   ├── ApproveUserRequestCommandHandler.java
│   ├── RejectUserRequestCommand.java
│   ├── RejectUserRequestCommandHandler.java
│   ├── BatchApproveUserRequestCommand.java
│   ├── BatchApproveUserRequestCommandHandler.java
│   ├── BatchRejectUserRequestCommand.java
│   └── BatchRejectUserRequestCommandHandler.java
├── query/
│   ├── GetUserRequestByIdQuery.java
│   ├── GetUserRequestByIdQueryHandler.java
│   ├── GetAllUserRequestsQuery.java
│   ├── GetAllUserRequestsQueryHandler.java
│   ├── GetUserRequestsByStatusQuery.java
│   ├── GetUserRequestsByStatusQueryHandler.java
│   ├── GetUserRequestHistoryQuery.java
│   └── GetUserRequestHistoryQueryHandler.java
└── controller/
    └── UserRequestController.java (refactored để dùng CommandBus/QueryBus)
```

---

## 📝 KẾT LUẬN

**UserRequest hiện tại:**
- ✅ Đáp ứng đúng Entity Base Classes pattern
- ✅ Đáp ứng đúng History Pattern
- ❌ **KHÔNG đáp ứng CQRS Pattern** - đây là vi phạm nghiêm trọng theo user rules

**Cần refactor ngay để tuân thủ kiến trúc backend base.**


