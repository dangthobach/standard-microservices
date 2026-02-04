# End-to-End Testing Guide - Product Approval Workflow

## Overview

This guide explains how to run comprehensive end-to-end tests for the product approval workflow, covering all scenarios from product creation to final approval or rejection.

---

## Test Scenarios

### Scenario 1: Happy Path ✅
**Flow:** Product Creation → Checker Approves → Confirmer Confirms → Product ACTIVE

**Steps:**
1. Create product via Business Service
2. Verify workflow triggered (RabbitMQ → Process Service)
3. Verify status = PENDING_APPROVAL
4. Checker completes approval task (approved=true)
5. Verify status = PENDING_CONFIRMATION
6. Confirmer completes confirmation task (confirmed=true)
7. Verify status = ACTIVE and active=true

**Expected Result:** Product status transitions: DRAFT → PENDING_APPROVAL → PENDING_CONFIRMATION → ACTIVE

---

### Scenario 2: Checker Rejection ❌
**Flow:** Product Creation → Checker Rejects → Product REJECTED_BY_CHECKER

**Steps:**
1. Create product
2. Wait for PENDING_APPROVAL
3. Checker completes task with (approved=false)
4. Verify status = REJECTED_BY_CHECKER
5. Process ends

**Expected Result:** Product remains rejected, workflow terminates

---

### Scenario 3: Confirmer Rejection ❌
**Flow:** Product Creation → Checker Approves → Confirmer Rejects → Product REJECTED_BY_CONFIRMER

**Steps:**
1. Create product
2. Checker approves (approved=true)
3. Confirmer completes task with (confirmed=false)
4. Verify status = REJECTED_BY_CONFIRMER
5. Process ends

**Expected Result:** Product rejected at final stage

---

## Prerequisites

### 1. Services Running

```bash
# Terminal 1: Business Service
cd business-service
mvn spring-boot:run
# Expected: Running on port 8081

# Terminal 2: IAM Service  
cd iam-service
mvn spring-boot:run
# Expected: Running on port 8082

# Terminal 3: Process Management Service
cd process-management-service
mvn spring-boot:run
# Expected: Running on port 8083
```

### 2. Infrastructure Running

```bash
# RabbitMQ
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# PostgreSQL (if not already running)
docker run -d --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:15

# Create databases
psql -U postgres -c "CREATE DATABASE business_db;"
psql -U postgres -c "CREATE DATABASE iam_db;"
psql -U postgres -c "CREATE DATABASE process_db;"
```

### 3. Verify Migrations Applied

**IAM Service - Check roles and users exist:**
```sql
-- Connect to iam_db
SELECT name FROM roles WHERE name IN ('ROLE_CHECKER', 'ROLE_CONFIRMER');
-- Should return 2 rows

SELECT email FROM users WHERE email LIKE '%@test.com';
-- Should return 5 users
```

**Process Service - Check BPMN deployed:**
```bash
curl http://localhost:8083/api/process-definitions
# Should show product-approval-process
```

---

## Running Tests

### Method 1: IntelliJ HTTP Client (Recommended)

1. Open `e2e-product-approval-test.http` in IntelliJ IDEA
2. Run each section sequentially:
   - Click ▶️ next to each request
   - Wait for response before proceeding to next
   - Check console for test assertions

**Automated Variables:**
- `productId` - Auto-captured from product creation
- `checkerTaskId` - Auto-captured from task query
- `confirmerTaskId` - Auto-captured from task query

### Method 2: Postman/Insomnia

1. Import `e2e-product-approval-test.http`
2. Create environment with variables:
   - `productId`
   - `checkerTaskId`
   - `confirmerTaskId`
3. Run requests manually, updating variables after each step

### Method 3: cURL Commands

```bash
# Step 1: Create Product
PRODUCT_ID=$(curl -s -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Laptop",
    "sku": "LAP-TEST-001",
    "price": 1299.99,
    "stockQuantity": 100
  }' | jq -r '.data')

echo "Product ID: $PRODUCT_ID"

# Step 2: Wait for workflow
sleep 3

# Step 3: Check product status
curl http://localhost:8081/api/products/$PRODUCT_ID | jq '.data.status'
# Expected: "PENDING_APPROVAL"

# Step 4: Get checker tasks
CHECKER_TASK_ID=$(curl -s "http://localhost:8083/api/flow/tasks?candidateGroup=ROLE_CHECKER" \
  | jq -r ".[0].id")

echo "Checker Task ID: $CHECKER_TASK_ID"

# Step 5: Approve as checker
curl -X POST http://localhost:8083/api/flow/tasks/$CHECKER_TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{"approved": true}'

# Step 6: Wait for status update
sleep 3

# Step 7: Check status again
curl http://localhost:8081/api/products/$PRODUCT_ID | jq '.data.status'
# Expected: "PENDING_CONFIRMATION"

# Step 8: Get confirmer tasks
CONFIRMER_TASK_ID=$(curl -s "http://localhost:8083/api/flow/tasks?candidateGroup=ROLE_CONFIRMER" \
  | jq -r ".[0].id")

# Step 9: Confirm as confirmer
curl -X POST http://localhost:8083/api/flow/tasks/$CONFIRMER_TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{"confirmed": true}'

# Step 10: Final check
sleep 3
curl http://localhost:8081/api/products/$PRODUCT_ID | jq '.data | {status, active}'
# Expected: {"status": "ACTIVE", "active": true}
```

---

## Verification Points

### 1. RabbitMQ Message Flow

**Check Queues:**
```bash
# RabbitMQ Management UI
http://localhost:15672
# Login: guest/guest

# Check queues
- process.request.queue (should have processed messages)
- product.status.queue (should have processed messages)
```

**Expected Message Flow:**
```
Business Service → [business.events exchange] → [process.request.queue]
                        ↓
Process Service consumes → Starts workflow → Updates status
                        ↓
Process Service → [business.events exchange] → [product.status.queue]
                        ↓
Business Service consumes → Updates Product.status in DB
```

### 2. Database Verification

**Product Status in business_db:**
```sql
SELECT id, name, sku, status, active, process_instance_id 
FROM products 
WHERE sku LIKE 'LAP-TEST%'
ORDER BY created_at DESC;
```

**Expected Progression:**
```
Initial:  status=DRAFT, active=true, process_instance_id=NULL
After workflow start: status=PENDING_APPROVAL, process_instance_id=<uuid>
After checker: status=PENDING_CONFIRMATION
After confirmer: status=ACTIVE, active=true
```

**Process Instance in process_db:**
```sql
SELECT * FROM act_ru_execution 
WHERE business_key_ = '<productId>';
-- Should show active execution until completion

SELECT * FROM act_hi_procinst 
WHERE business_key_ = '<productId>';
-- Shows historical process instance
```

### 3. Flowable Task Query

**Active Tasks:**
```bash
# Tasks for checkers
curl "http://localhost:8083/api/flow/tasks?candidateGroup=ROLE_CHECKER"

# Tasks for confirmers
curl "http://localhost:8083/api/flow/tasks?candidateGroup=ROLE_CONFIRMER"
```

**Completed Tasks History:**
```bash
curl "http://localhost:8083/api/history/tasks?processInstanceId=<processInstanceId>"
```

---

## Troubleshooting

### Issue 1: Workflow Not Starting

**Symptoms:**
- Product created but status stays DRAFT
- processInstanceId is NULL

**Checks:**
1. RabbitMQ connection:
   ```bash
   curl http://localhost:15672/api/overview
   ```

2. Process Service logs:
   ```
   INFO  c.e.p.consumer.ProcessRequestConsumer : Received process request
   ```

3. BPMN deployment:
   ```bash
   curl http://localhost:8083/api/process-definitions?key=product-approval-process
   # Should return process definition
   ```

**Fix:**
- Check RabbitMQ is running
- Verify process-service started successfully
- Check ProcessDeploymentRunner logs for deployment errors

---

### Issue 2: Status Not Updating

**Symptoms:**
- Task completed but product status unchanged

**Checks:**
1. ProductStatusDelegate executing:
   ```
   INFO  c.e.p.delegate.ProductStatusDelegate : productId=..., newStatus=PENDING_APPROVAL
   ```

2. ProductStatusProducer sending:
   ```
   INFO  c.e.p.producer.ProductStatusProducer : Sent product status change
   ```

3. ProductStatusConsumer receiving:
   ```
   INFO  c.e.b.consumer.ProductStatusConsumer : Received product status update
   INFO  c.e.b.consumer.ProductStatusConsumer : Updated product status in DB successfully
   ```

**Fix:**
- Verify RabbitMQ routing key matches: `product.status.change`
- Check ProductStatusConsumer is listening to correct queue
- Verify ProductStatusDelegate bean is properly wired

---

### Issue 3: No Tasks Available

**Symptoms:**
- GET /tasks returns empty array

**Checks:**
1. Process instance is active:
   ```sql
   SELECT * FROM act_ru_execution WHERE proc_inst_id_ = '<processInstanceId>';
   ```

2. Task exists:
   ```sql
   SELECT * FROM act_ru_task WHERE proc_inst_id_ = '<processInstanceId>';
   ```

3. Candidate group is correct:
   ```sql
   SELECT task_id_, group_id_ FROM act_ru_identitylink 
   WHERE group_id_ IN ('ROLE_CHECKER', 'ROLE_CONFIRMER');
   ```

**Fix:**
- Verify BPMN has correct candidateGroups
- Check that ROLE_CHECKER and ROLE_CONFIRMER roles exist in IAM

---

## Success Criteria

### Happy Path Test ✅

- [ ] Product created (200 OK)
- [ ] Workflow triggered (processInstanceId set)
- [ ] Status updated to PENDING_APPROVAL
- [ ] Checker task available and assignable
- [ ] Checker approval updates status to PENDING_CONFIRMATION
- [ ] Confirmer task available and assignable
- [ ] Confirmer confirmation updates status to ACTIVE
- [ ] active flag set to true
- [ ] Process instance completed

### Rejection Tests ✅

- [ ] Checker rejection sets status to REJECTED_BY_CHECKER
- [ ] Confirmer rejection sets status to REJECTED_BY_CONFIRMER
- [ ] Process instances complete after rejection

### Performance ✅

- [ ] Product creation < 500ms
- [ ] Workflow trigger (RabbitMQ) < 1s
- [ ] Status update < 1s
- [ ] Task completion < 500ms

---

## Test Report Template

```markdown
# E2E Test Report - Product Approval Workflow

**Date:** 2026-02-04
**Tester:** [Your Name]
**Environment:** Local Development

## Test Results

| Scenario | Expected | Actual | Status |
|----------|----------|--------|--------|
| Happy Path | Product ACTIVE | ACTIVE | ✅ PASS |
| Checker Rejection | REJECTED_BY_CHECKER | REJECTED_BY_CHECKER | ✅ PASS |
| Confirmer Rejection | REJECTED_BY_CONFIRMER | REJECTED_BY_CONFIRMER | ✅ PASS |

## Performance Metrics

- Product Creation: 245ms
- Workflow Trigger: 850ms
- Checker Approval: 320ms
- Confirmer Approval: 295ms
- Total Flow Time: ~4.5s

## Issues Found

None

## Notes

All tests passed successfully. Workflow operates as expected.
```

---

## Next Steps After Testing

Once E2E tests pass:

1. ✅ Phase 5: Implement RabbitMQ monitoring
2. ✅ Phase 6: Enhance Flowable UI for process tracking
3. ✅ Phase 7: Add process version management
