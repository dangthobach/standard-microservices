# Consul Service Discovery Integration

## Overview

This document describes the integration of HashiCorp Consul as a service discovery solution for the Enterprise Microservices Platform. Consul provides service registration, health checking, and dynamic service discovery capabilities.

## Architecture

### Components

1. **Consul Server**: Service registry and health check coordinator
2. **Gateway Service**: Registers with Consul and uses it for load balancing to downstream services
3. **IAM Service**: Registers with Consul for service discovery
4. **Business Service**: Registers with Consul for service discovery

### Service Discovery Flow

```
Client Request
    ↓
Gateway Service (Consul-aware)
    ↓
Consul (Service Registry)
    ↓
Returns available instances of target service
    ↓
Gateway routes to selected instance (lb://service-name)
```

## Configuration

### 1. Maven Dependencies

All services include the Consul discovery dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>
    <version>${spring-cloud-consul.version}</version>
</dependency>
```

### 2. Application Configuration

#### Gateway Service ([gateway-service/src/main/resources/application.yml](../gateway-service/src/main/resources/application.yml#L9-L28))

```yaml
spring:
  cloud:
    consul:
      host: ${CONSUL_HOST:localhost}
      port: ${CONSUL_PORT:8500}
      discovery:
        enabled: true
        register: true
        instance-id: ${spring.application.name}:${random.value}
        service-name: ${spring.application.name}
        prefer-ip-address: true
        health-check-path: /actuator/health
        health-check-interval: 10s
        health-check-timeout: 5s
        health-check-critical-timeout: 30s
        tags:
          - gateway
          - reactive
          - webflux
      config:
        enabled: false
```

#### IAM Service ([iam-service/src/main/resources/application.yml](../iam-service/src/main/resources/application.yml#L8-L28))

```yaml
spring:
  cloud:
    consul:
      host: ${CONSUL_HOST:localhost}
      port: ${CONSUL_PORT:8500}
      discovery:
        enabled: true
        register: true
        instance-id: ${spring.application.name}:${random.value}
        service-name: ${spring.application.name}
        prefer-ip-address: true
        health-check-path: /actuator/health
        health-check-interval: 10s
        health-check-timeout: 5s
        health-check-critical-timeout: 30s
        tags:
          - iam
          - virtual-threads
          - authentication
      config:
        enabled: false
```

#### Business Service ([business-service/src/main/resources/application.yml](../business-service/src/main/resources/application.yml#L8-L27))

```yaml
spring:
  cloud:
    consul:
      host: ${CONSUL_HOST:localhost}
      port: ${CONSUL_PORT:8500}
      discovery:
        enabled: true
        register: true
        instance-id: ${spring.application.name}:${random.value}
        service-name: ${spring.application.name}
        prefer-ip-address: true
        health-check-path: /actuator/health
        health-check-interval: 10s
        health-check-timeout: 5s
        health-check-critical-timeout: 30s
        tags:
          - business
          - virtual-threads
      config:
        enabled: false
```

### 3. Gateway Routes

The Gateway Service uses the `lb://` (Load Balancer) scheme to route requests through Consul service discovery:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: iam-service
          uri: lb://iam-service  # Resolved via Consul
          predicates:
            - Path=/api/iam/**

        - id: business-service
          uri: lb://business-service  # Resolved via Consul
          predicates:
            - Path=/api/business/**
```

## Deployment

### Docker Compose

Consul is deployed as a single-node server in development mode:

```yaml
consul:
  image: hashicorp/consul:1.20
  container_name: consul-server
  command: agent -server -ui -bootstrap-expect=1 -client=0.0.0.0
  ports:
    - "8500:8500"  # HTTP API and Web UI
    - "8600:8600/udp"  # DNS
  environment:
    CONSUL_BIND_INTERFACE: eth0
    CONSUL_CLIENT_INTERFACE: eth0
```

All services include Consul environment variables:

```yaml
environment:
  CONSUL_HOST: consul
  CONSUL_PORT: 8500
```

### Kubernetes

Consul is deployed as a StatefulSet with 3 replicas for high availability:

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: consul-server
spec:
  serviceName: consul-server
  replicas: 3
  template:
    spec:
      containers:
        - name: consul
          image: hashicorp/consul:1.20
          args:
            - "agent"
            - "-server"
            - "-bootstrap-expect=3"
            - "-ui"
            - "-retry-join=consul-server-0.consul-server.$(NAMESPACE).svc.cluster.local"
            - "-retry-join=consul-server-1.consul-server.$(NAMESPACE).svc.cluster.local"
            - "-retry-join=consul-server-2.consul-server.$(NAMESPACE).svc.cluster.local"
```

Services reference Consul via environment variables:

```yaml
env:
  - name: CONSUL_HOST
    value: "consul-server"
  - name: CONSUL_PORT
    value: "8500"
```

## Health Checks

Consul performs health checks on all registered services:

- **Health Check Path**: `/actuator/health`
- **Check Interval**: 10 seconds
- **Check Timeout**: 5 seconds
- **Critical Timeout**: 30 seconds (service deregistered after this time if failing)

Services must implement Spring Boot Actuator health endpoints to respond to these checks.

## Service Registration

When a service starts up:

1. Service connects to Consul at `CONSUL_HOST:CONSUL_PORT`
2. Registers itself with:
   - Service name (e.g., `iam-service`)
   - Instance ID (unique, using random value)
   - IP address or hostname
   - Port number
   - Health check configuration
   - Tags for categorization

3. Consul begins health checking the service
4. If healthy, service becomes available for discovery

## Service Discovery

When the Gateway needs to route a request:

1. Parses the `lb://service-name` URI
2. Queries Consul for all healthy instances of `service-name`
3. Applies load balancing algorithm (default: round-robin)
4. Routes request to selected instance
5. If instance fails, retries with another instance (based on Resilience4j configuration)

## Accessing Consul UI

### Docker Compose
- URL: http://localhost:8500/ui
- No authentication required in development mode

### Kubernetes
- Access via LoadBalancer service `consul-ui`
- Or port-forward: `kubectl port-forward svc/consul-ui 8500:8500`
- URL: http://localhost:8500/ui

## Monitoring and Observability

### Service Status

Check service registration status:

```bash
# List all services
curl http://localhost:8500/v1/catalog/services

# Get service details
curl http://localhost:8500/v1/catalog/service/iam-service

# Check service health
curl http://localhost:8500/v1/health/service/iam-service
```

### Metrics

Consul provides metrics that can be integrated with Prometheus:

```bash
curl http://localhost:8500/v1/agent/metrics
```

## Troubleshooting

### Service Not Registering

**Symptom**: Service starts but doesn't appear in Consul

**Solutions**:
1. Check Consul connectivity:
   ```bash
   curl http://${CONSUL_HOST}:${CONSUL_PORT}/v1/status/leader
   ```

2. Verify environment variables:
   ```bash
   echo $CONSUL_HOST
   echo $CONSUL_PORT
   ```

3. Check application logs for Consul connection errors

4. Ensure service has `spring-cloud-starter-consul-discovery` dependency

### Service Marked Unhealthy

**Symptom**: Service registered but marked as failing health checks

**Solutions**:
1. Check actuator health endpoint:
   ```bash
   curl http://localhost:8081/actuator/health
   ```

2. Verify health check path configuration matches actuator path

3. Check if service port is accessible from Consul

4. Review service logs for startup errors

### Load Balancing Not Working

**Symptom**: Gateway returns 503 Service Unavailable

**Solutions**:
1. Verify service is registered and healthy in Consul UI

2. Check Gateway route configuration uses `lb://` scheme:
   ```yaml
   uri: lb://iam-service  # NOT http://iam-service
   ```

3. Ensure Gateway has Spring Cloud LoadBalancer dependency

4. Check Gateway logs for service resolution errors

### Consul Cluster Issues (Kubernetes)

**Symptom**: Consul pods not forming cluster

**Solutions**:
1. Check pod logs:
   ```bash
   kubectl logs consul-server-0
   ```

2. Verify StatefulSet DNS resolution:
   ```bash
   kubectl exec consul-server-0 -- nslookup consul-server-0.consul-server
   ```

3. Check network policies allowing pod-to-pod communication

4. Ensure all 3 replicas are running and ready

## Best Practices

### 1. Health Endpoint Implementation

Ensure health endpoints return appropriate status codes:
- **200**: Service is healthy
- **503**: Service is unhealthy (will be deregistered after critical timeout)

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Check critical dependencies
        if (databaseAvailable && cacheAvailable) {
            return Health.up().build();
        }
        return Health.down().build();
    }
}
```

### 2. Instance ID Strategy

Use unique instance IDs to support multiple instances:

```yaml
instance-id: ${spring.application.name}:${random.value}
```

Or use hostname-based IDs in Kubernetes:

```yaml
instance-id: ${spring.application.name}:${HOSTNAME}
```

### 3. Prefer IP Address

Enable `prefer-ip-address: true` in containerized environments to avoid DNS resolution issues:

```yaml
discovery:
  prefer-ip-address: true
```

### 4. Graceful Shutdown

Configure graceful shutdown to deregister from Consul cleanly:

```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

management:
  endpoint:
    shutdown:
      enabled: true
```

### 5. Service Tags

Use tags for categorization and filtering:

```yaml
tags:
  - version:1.0.0
  - environment:production
  - region:us-east-1
```

Query services by tags:

```bash
curl "http://localhost:8500/v1/health/service/iam-service?tag=production"
```

## Security Considerations

### Production Deployment

For production environments, implement:

1. **ACL (Access Control Lists)**: Restrict access to Consul API
   ```bash
   consul acl bootstrap
   ```

2. **TLS Encryption**: Enable TLS for Consul communication
   ```yaml
   spring:
     cloud:
       consul:
         scheme: https
         tls:
           enabled: true
   ```

3. **Gossip Encryption**: Encrypt cluster communication
   ```bash
   consul agent -encrypt="$(consul keygen)"
   ```

4. **Network Policies**: Restrict network access to Consul ports

## Migration from Static Configuration

If migrating from static service URLs:

### Before (Static URLs)
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: iam-service
          uri: http://iam-service:8081  # Static
```

### After (Service Discovery)
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: iam-service
          uri: lb://iam-service  # Dynamic via Consul
```

## References

- [HashiCorp Consul Documentation](https://www.consul.io/docs)
- [Spring Cloud Consul Documentation](https://docs.spring.io/spring-cloud-consul/docs/current/reference/html/)
- [Consul Service Discovery Guide](https://learn.hashicorp.com/tutorials/consul/get-started-service-discovery)
- [Gateway Service Configuration](../gateway-service/src/main/resources/application.yml)
- [Consul StatefulSet](../k8s/base/consul-statefulset.yaml)
