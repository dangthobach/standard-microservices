# business-process-managerment

Spring Boot 3 + Flowable (BPMN/DMN/CMMN) + Flyway + PostgreSQL + Flowable UI (all-in-one for modeling).

## Prerequisites
- JDK 17+
- Maven 3.9+
- Docker (optional, for Postgres & Flowable UI)

## Run Postgres & Flowable UI
```bash
cd docker
docker compose up -d
```

Access Flowable UI: http://localhost:8081 (admin / test)

## Configure DB
The app connects to PostgreSQL at `jdbc:postgresql://localhost:5432/flowdb?currentSchema=flowable`
with user `flowuser` / `flowpass`.

Flyway `V1__init_flowable_schema.sql` creates the `flowable` schema.
Flowable will then auto-create/upgrade its tables in that schema on startup (DEV).

## Run the app
```bash
./mvnw spring-boot:run
```

## Quick tests
```bash
# Start BPMN process (uses DMN internally)
curl -X POST "http://localhost:8080/api/flow/start" -H "Content-Type: application/json" -d '{"amount":500}'

# List tasks
curl "http://localhost:8080/api/flow/tasks"

# Evaluate DMN directly
curl -X POST "http://localhost:8080/api/dmn/evaluate/routeDecision"   -H "Content-Type: application/json" -d '{"amount":1500}'

# Start CMMN case
curl -X POST "http://localhost:8080/api/case/start"
```

## Notes
- For production, set `flowable.database-schema-update=validate` and manage tables via migrations if preferred.
- The Flowable all-in-one image is for **development & modeling only**. Export BPMN/DMN/CMMN and keep them in `src/main/resources`.
