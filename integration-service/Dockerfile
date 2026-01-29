# Multi-stage build for Integration Service
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Step 1: Copy only POM files first (for dependency caching)
COPY pom.xml .
COPY common-lib/pom.xml ./common-lib/pom.xml
COPY gateway-service/pom.xml ./gateway-service/pom.xml
COPY iam-service/pom.xml ./iam-service/pom.xml
COPY business-service/pom.xml ./business-service/pom.xml
COPY process-management-service/pom.xml ./process-management-service/pom.xml
COPY integration-service/pom.xml ./integration-service/pom.xml

# Step 2: Download dependencies
RUN for i in 1 2 3; do \
        mvn dependency:go-offline -pl integration-service -am \
            -Dmaven.wagon.http.retryHandler.count=3 \
            -Dmaven.wagon.http.connectTimeout=10000 \
            && break || \
        (echo "Attempt $i failed, waiting before retry..." && sleep 5); \
    done

# Step 3: Copy source code
COPY common-lib/src ./common-lib/src
COPY integration-service/src ./integration-service/src

# Step 4: Build
RUN mvn clean package -DskipTests -o -pl integration-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install wget for health checks
RUN apk add --no-cache wget

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR
COPY --from=builder /workspace/integration-service/target/*.jar app.jar

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
