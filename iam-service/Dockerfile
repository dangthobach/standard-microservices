# Multi-stage build for IAM Service with Virtual Threads
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy parent POM
COPY pom.xml .

# Copy all service POMs and common-lib for multi-module resolution
COPY common-lib ./common-lib
COPY gateway-service/pom.xml ./gateway-service/pom.xml
COPY iam-service/pom.xml ./iam-service/pom.xml
COPY business-service/pom.xml ./business-service/pom.xml

# Copy iam-service source
COPY iam-service/src ./iam-service/src

# Build iam-service and its dependencies
RUN mvn clean package -DskipTests -pl iam-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /workspace/iam-service/target/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

# Enable Virtual Threads (Project Loom)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
