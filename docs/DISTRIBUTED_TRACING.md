# OpenTelemetry & Distributed Tracing Configuration

## Overview

This document describes the distributed tracing setup using OpenTelemetry for all microservices.

## Maven Dependencies

Add to each service's `pom.xml`:

```xml
<!-- OpenTelemetry Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
</dependency>
```

## Application Configuration

Add to `application.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100% of traces in dev/staging, reduce to 0.1 in production
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

spring:
  application:
    name: business-service  # Change per service
```

## Services Configuration

### Business Service

```yaml
spring:
  application:
    name: business-service
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411/api/v2/spans}
```

### IAM Service

```yaml
spring:
  application:
    name: iam-service
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411/api/v2/spans}
```

### Process Management Service

```yaml
spring:
  application:
    name: process-management-service
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411/api/v2/spans}
```

### Gateway Service

```yaml
spring:
  application:
    name: gateway-service
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411/api/v2/spans}
```

## Docker Compose Configuration

Zipkin is already configured in `docker-compose.yml`:

```yaml
zipkin:
  image: openzipkin/zipkin:3.4
  container_name: zipkin
  ports:
    - "9411:9411"
  networks:
    - microservices-network
```

## Usage

### Accessing Zipkin UI

**Local:**
- http://localhost:9411

**Production:**
- https://monitoring.example.com/zipkin

### Viewing Traces

1. Open Zipkin UI
2. Click "Find Traces"
3. Select service or search by trace ID
4. View trace timeline and span details

### Trace Example

```
Request: POST /api/products
  |
  ├─ gateway-service (100ms)
  │   └─ Authentication (20ms)
  │
  ├─ business-service (400ms)
  │   ├─ Validate Request (10ms)
  │   ├─ Save to Database (50ms)
  │   └─ Publish to RabbitMQ (30ms)
  │
  └─ process-service (200ms)
      ├─ Receive Message (5ms)
      ├─ Start Workflow (100ms)
      └─ Update Status (50ms)
```

## Custom Spans

Add custom spans for business logic:

```java
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;

@Service
public class ProductService {
    
    private final Tracer tracer;
    
    public ProductDTO createProduct(CreateProductRequest request) {
        // Create custom span
        Span span = tracer.nextSpan().name("createProduct");
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            // Add custom tags
            span.tag("product.sku", request.getSku());
            span.tag("product.category", request.getCategory());
            
            // Business logic here
            ProductDTO result = doCreateProduct(request);
            
            span.tag("product.id", result.getId().toString());
            span.event("product.created");
            
            return result;
        } finally {
            span.end();
        }
    }
}
```

## Propagation Context

Trace context is automatically propagated across:
- HTTP requests (via headers)
- RabbitMQ messages (via message headers)
- Database queries

## Sampling Strategies

### Development/Staging
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100%
```

### Production
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% (reduces overhead)
```

### Dynamic Sampling (Advanced)
```java
@Bean
public Sampler defaultSampler() {
    return new Sampler() {
        @Override
        public SamplingResult shouldSample(
                Context parentContext,
                String traceId,
                String name,
                SpanKind spanKind,
                Attributes attributes,
                List<LinkData> parentLinks) {
            
            // Always sample errors
            if (name.contains("error") || name.contains("exception")) {
                return SamplingResult.recordAndSample();
            }
            
            // Sample 10% of normal requests
            return Math.random() < 0.1 
                ? SamplingResult.recordAndSample() 
                : SamplingResult.drop();
        }
    };
}
```

## Performance Impact

- **Sampling 100%:** ~2-5ms overhead per request
- **Sampling 10%:** ~0.2-0.5ms overhead per request
- **Storage:** ~1KB per trace

## Troubleshooting

### Traces Not Appearing

1. Check Zipkin is running:
   ```bash
   curl http://localhost:9411/health
   ```

2. Verify configuration:
   ```bash
   curl http://localhost:8081/actuator/info
   ```

3. Check logs for errors:
   ```bash
   grep -i "zipkin\|tracing" logs/application.log
   ```

### High Latency

If tracing adds too much overhead:
1. Reduce sampling probability
2. Use async export
3. Increase batch size

## Integration with Monitoring

Trace IDs are automatically added to logs:

```
2024-02-04 INFO [business-service,a1b2c3d4e5f6,a1b2c3d4e5f6] Created product: SKU-123
```

Format: `[service-name, trace-id, span-id]`

## Best Practices

1. **Use meaningful span names**: `createProduct` not `method1`
2. **Add relevant tags**: SKU, user ID, status
3. **Record important events**: `product.created`, `workflow.started`
4. **Keep spans focused**: One logical operation per span
5. **Don't over-sample**: Balance observability vs. performance
