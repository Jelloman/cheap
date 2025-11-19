# Dockerfile for cheap-rest
# Expects JAR to be pre-built via: ./gradlew :cheap-rest:bootJar
FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

# Copy the pre-built JAR from Gradle build
COPY build/libs/cheap-rest-0.1.jar app.jar

# Create directory for SQLite database
RUN mkdir -p /data

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=postgres
ENV DB_PASSWORD=changeme
ENV CHEAP_DB_PATH=/data/cheap.db
# Java 24 requires native access for SQLite JDBC driver
ENV JAVA_OPTS="--enable-native-access=ALL-UNNAMED"

# Health check using wget (available in alpine)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application with JAVA_OPTS support
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
