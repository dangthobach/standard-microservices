# User Creation Request (Maker/Checker) Implementation

## Tổng quan

Đã triển khai đầy đủ workflow Maker/Checker cho việc tạo user mới, đảm bảo separation of duties (tách biệt trách nhiệm) và audit trail đầy đủ.

## Các thành phần đã triển khai

### 1. Domain Model

#### UserRequestStatus Enum
- `DRAFT`: Trạng thái ban đầu khi tạo request
- `WAITING_FOR_APPROVAL`: Đã submit, chờ Checker duyệt
- `APPROVED`: Đã được duyệt, User đã được tạo
- `REJECTED`: Bị từ chối, có thể chỉnh sửa và resubmit

#### UserRequestAction Enum
- `CREATE`: Tạo request mới
- `UPDATE`: Cập nhật request
- `SUBMIT`: Submit để duyệt
- `APPROVE`: Duyệt request
- `REJECT`: Từ chối request

#### UserRequest Entity
- Extends `StatefulEntity<UUID, UserRequestStatus>`
- Fields: `email`, `firstName`, `lastName`, `roleIds`, `requestCreatorId`
- State machine validation tự động
- Indexes tối ưu cho 1M user scale

#### UserRequestHistory Entity
- Extends `AuditableEntity<UUID>`
- Audit log cho mọi thay đổi trạng thái
- Fields: `oldStatus`, `newStatus`, `action`, `actorId`, `comment`, `metadata`
- Indexes tối ưu cho fast history reads

### 2. Repositories

- `UserRequestRepository`: CRUD và queries tối ưu
- `UserRequestHistoryRepository`: Queries lịch sử
- `RoleRepository`: Load roles để validate

### 3. Service Layer

#### UserRequestService
- **State Machine Logic**:
  - DRAFT -> WAITING_FOR_APPROVAL (Submit)
  - WAITING_FOR_APPROVAL -> APPROVED (Approve)
  - WAITING_FOR_APPROVAL -> REJECTED (Reject)
  - REJECTED -> WAITING_FOR_APPROVAL (Resubmit)

- **Separation of Duties**:
  - ✅ **Strict validation**: `Checker.id != Maker.id`
  - ✅ **Self-approval prevention**: Throw `AccessDeniedException` nếu Maker tự approve

- **Validation**:
  - Email uniqueness check
  - Role existence validation
  - Status transition validation
  - MANDATORY rejectReason khi REJECT

- **Post-Actions**:
  - On APPROVED: Tự động tạo User entity
  - Publish `UserRequestProcessedEvent` cho email notifications

### 4. REST API Endpoints

Base path: `/api/iam/requests`

| Method | Endpoint | Description | Permission |
|--------|----------|-------------|------------|
| POST | `/api/iam/requests` | Create draft request | USER_REQUEST_CREATE |
| PUT | `/api/iam/requests/{id}` | Update draft/rejected | USER_REQUEST_CREATE |
| POST | `/api/iam/requests/{id}/submit` | Submit for approval | USER_REQUEST_CREATE |
| POST | `/api/iam/requests/{id}/approve` | Approve request | USER_REQUEST_APPROVE |
| POST | `/api/iam/requests/{id}/reject` | Reject request | USER_REQUEST_APPROVE |
| POST | `/api/iam/requests/batch/approve` | Batch approve | USER_REQUEST_APPROVE |
| POST | `/api/iam/requests/batch/reject` | Batch reject | USER_REQUEST_APPROVE |
| GET | `/api/iam/requests` | List all (paginated) | USER_REQUEST_VIEW |
| GET | `/api/iam/requests/{id}` | Get by ID | USER_REQUEST_VIEW |
| GET | `/api/iam/requests/{id}/history` | Get audit log | USER_REQUEST_VIEW |

### 5. DTOs

- `UserRequestDTO`: Response DTO
- `CreateUserRequestDTO`: Create request DTO với validation
- `UpdateUserRequestDTO`: Update request DTO
- `UserRequestHistoryDTO`: History entry DTO
- `BatchApproveDTO`: Batch approve DTO
- `BatchRejectDTO`: Batch reject DTO (với mandatory reason)

### 6. Database Migration

**V3__Add_User_Request_Permissions.sql**:
- Tạo `user_requests` table với indexes
- Tạo `user_request_roles` table (ElementCollection)
- Tạo `user_request_history` table với indexes
- Insert permissions: `USER_REQUEST_CREATE`, `USER_REQUEST_VIEW`, `USER_REQUEST_APPROVE`
- Assign permissions to roles:
  - ADMIN: All permissions
  - MANAGER: All permissions
  - USER: CREATE + VIEW only (không thể approve)

### 7. Events

- `UserRequestProcessedEvent`: Published khi approve/reject
  - Fields: `requestId`, `email`, `fullName`, `status`, `approved`, `rejectReason`
  - Dùng cho email notifications

## Workflow Example

### Scenario 1: Maker tạo và submit request

1. **Maker** (userA@example.com) tạo request:
   ```bash
   POST /api/iam/requests
   {
     "email": "newuser@example.com",
     "firstName": "John",
     "lastName": "Doe",
     "roleIds": ["role-uuid-1"]
   }
   ```
   → Status: `DRAFT`

2. **Maker** submit request:
   ```bash
   POST /api/iam/requests/{id}/submit
   ```
   → Status: `WAITING_FOR_APPROVAL`

### Scenario 2: Checker approve/reject

3. **Checker** (userB@example.com) approve:
   ```bash
   POST /api/iam/requests/{id}/approve
   ```
   → Status: `APPROVED`
   → User entity được tạo tự động
   → Event published cho email

4. **Nếu Checker reject**:
   ```bash
   POST /api/iam/requests/{id}/reject
   {
     "reason": "Incomplete information"
   }
   ```
   → Status: `REJECTED`
   → Event published với rejectReason

### Scenario 3: Maker chỉnh sửa và resubmit

5. **Maker** chỉnh sửa request bị reject:
   ```bash
   PUT /api/iam/requests/{id}
   {
     "email": "newuser@example.com",
     "firstName": "John",
     "lastName": "Doe Updated",
     "roleIds": ["role-uuid-1", "role-uuid-2"]
   }
   ```
   → Status vẫn là `REJECTED`

6. **Maker** resubmit:
   ```bash
   POST /api/iam/requests/{id}/submit
   ```
   → Status: `WAITING_FOR_APPROVAL` (lại)

## Separation of Duties Validation

### ✅ Test Case 1: Maker không thể approve request của chính mình

```java
// Maker (userA) tạo request
UserRequest request = userRequestService.createRequest(...);
// request.requestCreatorId = "userA@example.com"

// Maker (userA) cố approve → FAIL
try {
    userRequestService.approveRequest(request.getId());
    // Current user = "userA@example.com"
    // requestCreatorId = "userA@example.com"
    // → AccessDeniedException: "Separation of duties violation"
} catch (AccessDeniedException e) {
    // ✅ Expected: Maker cannot approve own request
}
```

### ✅ Test Case 2: Checker (khác Maker) có thể approve

```java
// Maker (userA) tạo và submit request
UserRequest request = userRequestService.createRequest(...);
userRequestService.submitRequest(request.getId());

// Checker (userB) approve → SUCCESS
// Current user = "userB@example.com"
// requestCreatorId = "userA@example.com"
// → userB != userA → ✅ Allowed
UserRequest approved = userRequestService.approveRequest(request.getId());
```

## Performance Optimizations

1. **Indexes**:
   - `idx_request_status`: Filter by status
   - `idx_request_creator`: Filter by creator
   - `idx_request_email`: Email uniqueness check
   - `idx_history_request_id`: Fast history reads

2. **History Table**:
   - Dedicated table thay vì Envers
   - Indexed queries cho fast reads
   - JSONB metadata cho flexibility

3. **Pagination**:
   - Tất cả list endpoints hỗ trợ pagination
   - Page size mặc định: 20

## Security & Permissions

### Permissions

- `USER_REQUEST_CREATE`: Tạo/update requests (Maker)
- `USER_REQUEST_VIEW`: Xem requests (Both)
- `USER_REQUEST_APPROVE`: Approve/reject (Checker)

### Role Assignments

- **ADMIN**: All permissions
- **MANAGER**: All permissions  
- **USER**: CREATE + VIEW only (không thể approve)

## Testing Recommendations

### Unit Tests

1. **State Machine Transitions**:
   - Test tất cả valid transitions
   - Test invalid transitions (throw exception)

2. **Separation of Duties**:
   - Test Maker không thể approve own request
   - Test Checker có thể approve request của Maker khác

3. **Validation**:
   - Test email uniqueness
   - Test role existence
   - Test mandatory rejectReason

### Integration Tests

1. **Full Flow**:
   - Create → Submit → Approve → User created
   - Create → Submit → Reject → Update → Resubmit → Approve

2. **Batch Operations**:
   - Batch approve multiple requests
   - Batch reject với reason

3. **History**:
   - Verify history entries được tạo đúng
   - Verify actorId, oldStatus, newStatus, action

## Next Steps (Optional Enhancements)

1. **Email Notifications**:
   - Implement event listener cho `UserRequestProcessedEvent`
   - Send email khi approve/reject

2. **Keycloak Integration**:
   - Tạo Keycloak user khi request approved
   - Update `keycloakId` trong User entity

3. **Frontend**:
   - Angular components cho request list
   - Bulk selection với checkboxes
   - Reject modal với reason input

4. **Advanced Features**:
   - Request expiration (auto-reject sau X ngày)
   - Multi-level approval (nếu cần)
   - Request templates

## Files Created/Modified

### New Files

1. `iam-service/src/main/java/com/enterprise/iam/entity/UserRequestStatus.java`
2. `iam-service/src/main/java/com/enterprise/iam/entity/UserRequestAction.java`
3. `iam-service/src/main/java/com/enterprise/iam/entity/UserRequest.java`
4. `iam-service/src/main/java/com/enterprise/iam/entity/UserRequestHistory.java`
5. `iam-service/src/main/java/com/enterprise/iam/repository/UserRequestRepository.java`
6. `iam-service/src/main/java/com/enterprise/iam/repository/UserRequestHistoryRepository.java`
7. `iam-service/src/main/java/com/enterprise/iam/repository/RoleRepository.java`
8. `iam-service/src/main/java/com/enterprise/iam/service/UserRequestService.java`
9. `iam-service/src/main/java/com/enterprise/iam/controller/UserRequestController.java`
10. `iam-service/src/main/java/com/enterprise/iam/dto/UserRequestDTO.java`
11. `iam-service/src/main/java/com/enterprise/iam/dto/CreateUserRequestDTO.java`
12. `iam-service/src/main/java/com/enterprise/iam/dto/UpdateUserRequestDTO.java`
13. `iam-service/src/main/java/com/enterprise/iam/dto/UserRequestHistoryDTO.java`
14. `iam-service/src/main/java/com/enterprise/iam/dto/BatchApproveDTO.java`
15. `iam-service/src/main/java/com/enterprise/iam/dto/BatchRejectDTO.java`
16. `iam-service/src/main/resources/db/migration/V3__Add_User_Request_Permissions.sql`

## Verification Checklist

- [x] UserRequest entity với StatefulEntity
- [x] UserRequestHistory entity với AuditableEntity
- [x] State machine validation
- [x] Separation of duties (Maker != Checker)
- [x] Mandatory rejectReason
- [x] Email uniqueness validation
- [x] Role validation
- [x] Automatic user creation on approval
- [x] Event publishing
- [x] Audit logging
- [x] REST API endpoints
- [x] Database migration
- [x] Permissions setup
- [x] Indexes for performance

## Summary

✅ **Hoàn thành đầy đủ** Maker/Checker workflow với:
- State machine validation
- Separation of duties (strict)
- Audit trail (UserRequestHistory)
- Performance optimizations (indexes)
- REST API đầy đủ
- Database migration
- Permissions & RBAC

Hệ thống sẵn sàng để test và deploy! 🚀

