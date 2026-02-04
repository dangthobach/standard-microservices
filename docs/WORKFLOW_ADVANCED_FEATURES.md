# Workflow Advanced Features Guide

## Overview

This document covers advanced workflow features including dynamic multi-level approvals, parallel approvals, task delegation, and analytics.

---

## 1. Dynamic Multi-Level Approval

### Approval Levels

The system automatically determines approval levels based on business rules:

| Level | Criteria | Approvers Required |
|-------|----------|-------------------|
| **BASIC** | Price < $1,000 AND normal category | Checker only |
| **STANDARD** | Price < $10,000 AND normal category | Checker + Confirmer |
| **SENIOR** | Price >= $10,000 OR high-risk category | Checker + Confirmer + Senior |
| **EXECUTIVE** | Price >= $50,000 OR critical category | All levels + Executive |

### Configuration

**ApprovalLevelEvaluator delegate:**
```java
@Component("approvalLevelEvaluator")
public class ApprovalLevelEvaluator implements JavaDelegate {
    
    private String determineApprovalLevel(BigDecimal price, String category, Integer stockQuantity) {
        if (price.compareTo(BigDecimal.valueOf(50000)) >= 0 || "CRITICAL".equals(category)) {
            return "EXECUTIVE";
        }
        if (price.compareTo(BigDecimal.valueOf(10000)) >= 0 || "HIGH_RISK".equals(category)) {
            return "SENIOR";
        }
        if (price.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return "STANDARD";
        }
        return "BASIC";
    }
}
```

### Usage Example

```http
### Create low-value product (BASIC level - Checker only)
POST http://localhost:8081/api/products
Content-Type: application/json

{
  "name": "Basic Product",
  "sku": "BASIC-001",
  "price": 500.00,
  "category": "ELECTRONICS",
  "stockQuantity": 100
}

### Create high-value product (SENIOR level - 3 approvers)
POST http://localhost:8081/api/products
Content-Type: application/json

{
  "name": "Premium Product",
  "sku": "PREMIUM-001",
  "price": 15000.00,
  "category": "HIGH_RISK",
  "stockQuantity": 10
}
```

---

## 2. Parallel Approval Workflows

### Quorum-Based Approval

For high-value products, require approval from multiple checkers in parallel:

**Features:**
- Multiple checkers can approve simultaneously
- Configurable quorum (e.g., 2 out of 3 must approve)
- Faster processing through parallelization

**BPMN Configuration:**
```xml
<parallelGateway id="parallelApprovalGateway"/>

<sequenceFlow sourceRef="parallelApprovalGateway" targetRef="checker1Task"/>
<sequenceFlow sourceRef="parallelApprovalGateway" targetRef="checker2Task"/>
<sequenceFlow sourceRef="parallelApprovalGateway" targetRef="checker3Task"/>

<userTask id="checker1Task" name="Checker 1" flowable:candidateUsers="${checker1}"/>
<userTask id="checker2Task" name="Checker 2" flowable:candidateUsers="${checker2}"/>
<userTask id="checker3Task" name="Checker 3" flowable:candidateUsers="${checker3}"/>

<!-- Collection gateway to wait for all -->
<parallelGateway id="joinGateway"/>

<sequenceFlow sourceRef="checker1Task" targetRef="joinGateway"/>
<sequenceFlow sourceRef="checker2Task" targetRef="joinGateway"/>
<sequenceFlow sourceRef="checker3Task" targetRef="joinGateway"/>
```

### Voting Logic

```java
@Component
public class ApprovalVotingDelegate implements JavaDelegate {
    
    @Override
    public void execute(DelegateExecution execution) {
        Boolean checker1Approved = (Boolean) execution.getVariable("checker1Approved");
        Boolean checker2Approved = (Boolean) execution.getVariable("checker2Approved");
        Boolean checker3Approved = (Boolean) execution.getVariable("checker3Approved");
        
        int approvalCount = 0;
        if (Boolean.TRUE.equals(checker1Approved)) approvalCount++;
        if (Boolean.TRUE.equals(checker2Approved)) approvalCount++;
        if (Boolean.TRUE.equals(checker3Approved)) approvalCount++;
        
        // Require 2 out of 3 approvals
        boolean quorumReached = approvalCount >= 2;
        execution.setVariable("quorumReached", quorumReached);
        
        log.info("Approval voting result: {}/3 approved, quorum: {}", 
                approvalCount, quorumReached);
    }
}
```

---

## 3. Task Delegation

### Delegate Task to Another User

**API Endpoint:**
```http
### Delegate task to another user
POST http://localhost:8083/api/flow/tasks/{{taskId}}/delegate
Content-Type: application/json

{
  "delegateUserId": "user123",
  "comment": "Delegating to specialist for review"
}
```

**Implementation:**
```java
@PostMapping("/tasks/{taskId}/delegate")
public ResponseEntity<Void> delegateTask(
        @PathVariable String taskId,
        @RequestBody DelegateTaskRequest request) {
    
    taskService.delegateTask(taskId, request.getDelegateUserId());
    
    // Add comment
    taskService.addComment(taskId, null, request.getComment());
    
    return ResponseEntity.ok().build();
}
```

### Claim Delegated Task

```http
### Claim delegated task
POST http://localhost:8083/api/flow/tasks/{{taskId}}/claim
```

### Return Delegated Task

```http
### Return task to original assignee
POST http://localhost:8083/api/flow/tasks/{{taskId}}/resolve
```

---

## 4. Workflow Analytics

### Available Endpoints

#### 1. Approval Time by Role
```http
GET http://localhost:8083/api/analytics/workflow/approval-time-by-role
```

**Response:**
```json
[
  {
    "assignee_role": "ROLE_CHECKER",
    "total_approvals": 150,
    "avg_approval_time_seconds": 245.5,
    "min_approval_time_seconds": 30.0,
    "max_approval_time_seconds": 1800.0,
    "median_approval_time_seconds": 180.0,
    "p95_approval_time_seconds": 600.0
  }
]
```

#### 2. Approval Success Rate
```http
GET http://localhost:8083/api/analytics/workflow/approval-success-rate?days=30
```

**Response:**
```json
[
  {
    "approval_date": "2024-02-04",
    "total_products": 45,
    "approved_count": 38,
    "rejected_count": 7,
    "approval_rate_percent": 84.44,
    "rejection_rate_percent": 15.56
  }
]
```

#### 3. Workflow Bottlenecks
```http
GET http://localhost:8083/api/analytics/workflow/bottlenecks
```

**Response:**
```json
[
  {
    "task_name": "checkerApprovalTask",
    "pending_count": 23,
    "avg_wait_time_seconds": 3600.5,
    "max_wait_time_seconds": 7200.0,
    "oldest_task_created_at": "2024-02-03T10:30:00"
  }
]
```

#### 4. User Performance
```http
GET http://localhost:8083/api/analytics/workflow/user-performance?username=checker1
```

#### 5. Process Duration Analysis
```http
GET http://localhost:8083/api/analytics/workflow/process-duration?days=30
```

#### 6. Overall Statistics
```http
GET http://localhost:8083/api/analytics/workflow/statistics
```

**Response:**
```json
{
  "active_processes": 15,
  "pending_tasks": 23,
  "completed_today": 12,
  "avg_completion_time_today": 450.5,
  "total_approved_products": 1250,
  "total_rejected_products": 180
}
```

#### 7. Approval Funnel
```http
GET http://localhost:8083/api/analytics/workflow/funnel
```

**Response:**
```json
{
  "pending_checker": 15,
  "pending_confirmer": 8,
  "approved": 1250,
  "rejected": 180
}
```

---

## 5. Grafana Dashboard Configuration

### Metrics Visualization

**Dashboard JSON:**
```json
{
  "dashboard": {
    "title": "Workflow Analytics",
    "panels": [
      {
        "title": "Approval Success Rate",
        "type": "graph",
        "targets": [
          {
            "query": "SELECT approval_date, approval_rate_percent FROM v_approval_success_rate ORDER BY approval_date",
            "interval": "1d"
          }
        ]
      },
      {
        "title": "Pending Tasks by Type",
        "type": "pie",
        "targets": [
          {
            "query": "SELECT task_name, pending_count FROM v_workflow_bottlenecks"
          }
        ]
      },
      {
        "title": "Average Approval Time",
        "type": "gauge",
        "targets": [
          {
            "query": "SELECT avg_approval_time_seconds FROM v_approval_time_by_role WHERE assignee_role = 'ROLE_CHECKER'"
          }
        ]
      }
    ]
  }
}
```

---

## 6. Best Practices

### Dynamic Approval Configuration

1. **Business Rules Engine**: Consider using Drools or similar for complex approval logic
2. **Configuration Database**: Store approval thresholds in database for easy updates
3. **Audit Trail**: Log all approval level determinations

### Parallel Approvals

1. **Timeout Handling**: Set timeouts for parallel tasks
2. **Escalation**: Auto-escalate if quorum not reached within SLA
3. **Notification**: Notify all parallel approvers simultaneously

### Task Delegation

1. **Authorization**: Verify delegatee has required permissions
2. **Audit**: Log all delegation actions
3. **Notification**: Notify both delegator and delegatee

### Analytics

1. **Caching**: Cache analytics queries for performance
2. **Materialized Views**: Use materialized views for complex aggregations
3. **Archiving**: Archive old workflow data to separate tables
4. **Real-time**: Use event streaming for real-time dashboards

---

## 7. Troubleshooting

### Approval Level Not Determined

**Issue:** Product stuck in workflow, approval level not set

**Solution:**
```sql
-- Check process variables
SELECT * FROM act_ru_variable 
WHERE proc_inst_id_ = 'process-instance-id';

-- Set approval level manually
UPDATE act_ru_variable 
SET text_ = 'STANDARD' 
WHERE name_ = 'approvalLevel' 
  AND proc_inst_id_ = 'process-instance-id';
```

### Parallel Tasks Not Completing

**Issue:** Process stuck waiting for parallel gateway

**Solution:**
```java
// Check active tasks
List<Task> tasks = taskService.createTaskQuery()
    .processInstanceId("process-instance-id")
    .list();

// Complete stuck tasks programmatically
tasks.forEach(task -> {
    taskService.complete(task.getId());
});
```

### Analytics Queries Slow

**Issue:** Analytics endpoints timing out

**Solution:**
```sql
-- Refresh materialized views
REFRESH MATERIALIZED VIEW v_approval_time_by_role;

-- Add missing indexes
CREATE INDEX idx_hi_taskinst_composite 
ON act_hi_taskinst(task_def_key_, end_time_, assignee_);

-- Analyze tables
ANALYZE act_hi_taskinst;
ANALYZE act_hi_procinst;
```

---

## 8. Future Enhancements

### Planned Features

1. **AI-Powered Approval Routing**
   - Machine learning to predict approval outcomes
   - Smart task assignment based on approver performance

2. **Mobile Approval App**
   - Push notifications for pending tasks
   - Quick approve/reject interface

3. **Workflow Templates**
   - Pre-configured workflows for different product categories
   - Template marketplace

4. **Advanced Analytics**
   - Predictive analytics for bottleneck prevention
   - Anomaly detection for unusual approval patterns
   - Cost analysis per approval level
