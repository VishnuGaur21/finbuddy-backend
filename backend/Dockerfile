# Multi-stage Dockerfile for Finbuddy CRM Backend (Java 21 LTS)
# Stage 1: Build & Package
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Cache Maven dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw || true

# Copy source code and build production artifact
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -DskipTests || mvn clean package -DskipTests

# Stage 2: Minimal Distroless / Hardened Runtime
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Non-root user for enterprise container compliance
RUN addgroup -S finbuddy && adduser -S finbuddy -G finbuddy
USER finbuddy

# Copy executable jar from builder
COPY --from=builder /workspace/target/*.jar app.jar

# Expose standard Spring Boot port
EXPOSE 8080

# Production JVM flags optimized for container resource constraints
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
