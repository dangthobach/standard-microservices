# Multi-stage build for Business Service with Virtual Threads
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Step 1: Copy only POM files first (for dependency caching)
COPY pom.xml .
COPY common-lib/pom.xml ./common-lib/pom.xml
COPY gateway-service/pom.xml ./gateway-service/pom.xml
COPY iam-service/pom.xml ./iam-service/pom.xml
COPY business-service/pom.xml ./business-service/pom.xml

# Step 2: Download dependencies (this layer is CACHED until POMs change)
RUN mvn dependency:go-offline -pl business-service -am

# Step 3: Copy source code (only this executes on code changes)
COPY common-lib/src ./common-lib/src
COPY business-service/src ./business-service/src

# Step 4: Build with offline mode (dependencies already cached)
RUN mvn clean package -DskipTests -o -pl business-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from builder stage
COPY --from=builder /workspace/business-service/target/*.jar app.jar

# Expose business service port
EXPOSE 8082

# Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8082/actuator/health || exit 1

# Enable Virtual Threads (Project Loom) and optimized JVM settings
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:+UseStringDeduplication", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
