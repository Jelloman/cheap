# Multi-stage Dockerfile for cheap-rest
# Stage 1: Build the application
FROM gradle:8.5-jdk24 AS build

WORKDIR /app

# Copy gradle files
COPY settings.gradle.kts .
COPY gradle ./gradle
COPY gradlew .
COPY gradle.properties .

# Copy all module build files
COPY cheap-core/build.gradle.kts cheap-core/
COPY cheap-json/build.gradle.kts cheap-json/
COPY cheap-db-postgres/build.gradle.kts cheap-db-postgres/
COPY cheap-db-sqlite/build.gradle.kts cheap-db-sqlite/
COPY cheap-db-mariadb/build.gradle.kts cheap-db-mariadb/
COPY cheap-rest/build.gradle.kts cheap-rest/

# Download dependencies
RUN gradle dependencies --no-daemon

# Copy source code
COPY cheap-core/src cheap-core/src
COPY cheap-json/src cheap-json/src
COPY cheap-db-postgres/src cheap-db-postgres/src
COPY cheap-db-sqlite/src cheap-db-sqlite/src
COPY cheap-db-mariadb/src cheap-db-mariadb/src
COPY cheap-rest/src cheap-rest/src

# Build the application
RUN gradle :cheap-rest:bootJar --no-daemon

# Stage 2: Create runtime image
FROM eclipse-temurin:24-jre

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/cheap-rest/build/libs/cheap-rest-0.1.jar app.jar

# Create directory for SQLite database
RUN mkdir -p /data

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=postgres
ENV DB_PASSWORD=changeme
ENV CHEAP_DB_PATH=/data/cheap.db

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
