# ============================================
# Sarabun PDF API - Docker Image
# ============================================
# Multi-stage build for smaller image size

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml first (for dependency caching)
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="ETDA <info@etda.or.th>"
LABEL description="Sarabun PDF Generation API"
LABEL version="1.0.0"

WORKDIR /app

# Install fonts support for Thai language
RUN apk add --no-cache \
    fontconfig \
    ttf-dejavu \
    && fc-cache -f -v

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Copy fonts (Thai fonts are bundled in resources)
# Fonts are already included in the JAR via src/main/resources/fonts/

# Create temp directory for PDF files
RUN mkdir -p /tmp/sarabun_pdf_files && chown appuser:appgroup /tmp/sarabun_pdf_files

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8888

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8888/actuator/health || exit 1

# JVM options for container
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
