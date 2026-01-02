# Multi-stage build for Gateway Service
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Install DNS utilities and configure network retry
RUN apk add --no-cache curl bind-tools ca-certificates && \
    update-ca-certificates

# Step 1: Copy only POM files first (for dependency caching)
COPY pom.xml .
COPY common-lib/pom.xml ./common-lib/pom.xml
COPY gateway-service/pom.xml ./gateway-service/pom.xml
COPY iam-service/pom.xml ./iam-service/pom.xml
COPY business-service/pom.xml ./business-service/pom.xml

# Step 1.5: Copy Maven settings with retry configuration
COPY .mvn/settings.xml /root/.m2/settings.xml

# Step 2: Download dependencies (this layer is CACHED until POMs change)
# Add retry logic with exponential backoff and DNS verification
RUN echo "Verifying network connectivity..." && \
    nslookup repo.maven.apache.org || nslookup repo1.maven.org || true && \
    for i in 1 2 3 4 5; do \
        echo "=== Maven dependency download attempt $i/5 ==="; \
        mvn dependency:go-offline -pl gateway-service -am \
            -Dmaven.wagon.http.retryHandler.count=5 \
            -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
            -Dmaven.wagon.http.readTimeout=60000 \
            -Dmaven.wagon.http.connectTimeout=30000 \
            -U \
            && echo "Dependencies downloaded successfully!" && break || \
        (echo "Attempt $i failed, waiting before retry..." && \
         sleep $((i * 3))); \
    done || (echo "ERROR: Failed to download dependencies after 5 attempts" && exit 1)

# Step 3: Copy source code (only this executes on code changes)
COPY common-lib/src ./common-lib/src
COPY gateway-service/src ./gateway-service/src

# Step 4: Build with offline mode (dependencies already cached)
RUN mvn clean package -DskipTests -o -pl gateway-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from builder
COPY --from=builder /workspace/gateway-service/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Run with optimized JVM settings for containers
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:+UseStringDeduplication", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
