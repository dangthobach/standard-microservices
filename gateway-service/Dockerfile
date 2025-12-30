# Multi-stage build for Gateway Service
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY ../pom.xml ../pom.xml
COPY ../common-lib ../common-lib

# Copy source
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

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
