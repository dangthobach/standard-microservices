# Strategic Roadmap: Phase 2 - Production Readiness & 1M CCU scaling

## Status Overview
We have successfully implemented the **Centralized Dynamic Authorization** architecture (Phase 1).
*   ✅ **Gateway**: Dynamic Policy Enforcement, L1/L2 Caching.
*   ✅ **IAM**: Policy/Permission Management API.
*   ✅ **Workflow**: Optimized for high concurrency.

## Phase 2 Objectives
To effectively handle **1 Million CCU**, we must now move from "Architectural Correctness" to "Operational Excellence".

### 1. Database Migration & Seeding (Critical)
*   **Context**: We created Entities (`EndpointProtection`) but haven't set up the DB Schema management.
*   **Action**: Integrate **Flyway** (or Liquibase).
*   **Tasks**:
    *   Create SQL migration scripts for `iam-service` (tables: `endpoint_protections`, `permissions`, `roles`, etc.).
    *   **Seed Data**: Insert default Policies (e.g., `/public/**` -> Public, `/api/admin/**` -> ADMIN_ACCESS).
    *   *Why*: Without this, the system starts with no rules and might block/allow incorrectly.

### 2. Business Service Optimization (Read-Heavy)
*   **Context**: 1M CCU usually means 90% Read traffic (browsing products/content).
*   **Action**: Implement **Multi-Level Caching for Business Data**.
*   **Tasks**:
    *   Apply `Spring Cache` + `Caffeine` + `Redis` pattern to `business-service` (products, categories).
    *   Implement **Cache Stampede Protection** (using `Redisson` or `Spring Cache` sync).

### 3. Asynchronous Processing (Write-Heavy)
*   **Context**: Direct DB writes at 1M CCU will crash the database (Connection Pool exhaustion).
*   **Action**: Implement **Event-Driven Architecture (EDA)** using Kafka.
*   **Tasks**:
    *   Move "Write" operations (e.g., `Place Order`, `Audit Log`) to Kafka Topics.
    *   Implement Consumers to process writes in batches (Throttling).

### 4. Advanced Resiliency
*   **Context**: One rogue service or DDoS attack can bring down the mesh.
*   **Action**: Tune **Rate Limiting** & **Circuit Breakers**.
*   **Tasks**:
    *   Configure `Bucket4j` Rate Limiting rules per API plan (Free/Premium).
    *   Test Circuit Breakers (`Resilience4j`) on Gateway -> Business calls.

### 5. Observability (Visualizing 1M CCU)
*   **Context**: You can't fix what you can't see.
*   **Action**: Finalize Monitoring Stack.
*   **Tasks**:
    *   Verify Distributed Tracing (TraceID propagation from Gateway -> IAM -> Business).
    *   Set up Prometheus Alerts for "Cache Miss Rate" and "AuthZ Latency".

## Recommendation for Next Step
I recommend starting with **Item 1: Database Migration & Seeding**.
This ensures our new AuthZ code actually runs against a real database schema and has the necessary data to function.

**Would you like me to proceed with setting up Flyway and the initial Migration Scripts?**
