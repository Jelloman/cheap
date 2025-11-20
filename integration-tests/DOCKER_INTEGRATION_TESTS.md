# Docker Integration Tests

This document summarizes the Docker-based integration tests for the Cheap libraries.

## Gradle Docker Tasks

```bash
# Docker image tasks
./gradlew buildCheapRestImage       # Build Docker image for cheap-rest
./gradlew removeCheapRestImage      # Remove Docker image

# Network tasks
./gradlew createDockerNetwork       # Create Docker network
./gradlew removeDockerNetwork       # Remove Docker network

# PostgreSQL container tasks
./gradlew createPostgresContainer
./gradlew startPostgresContainer
./gradlew stopPostgresContainer
./gradlew removePostgresContainer

# MariaDB container tasks
./gradlew createMariaDbContainer
./gradlew startMariaDbContainer
./gradlew stopMariaDbContainer
./gradlew removeMariaDbContainer

# cheap-rest container tasks (per database)
./gradlew createCheapRestPostgresContainer
./gradlew startCheapRestPostgresContainer
./gradlew stopCheapRestPostgresContainer
./gradlew removeCheapRestPostgresContainer

./gradlew createCheapRestMariaDbContainer
./gradlew startCheapRestMariaDbContainer
./gradlew stopCheapRestMariaDbContainer
./gradlew removeCheapRestMariaDbContainer

./gradlew createCheapRestSqliteContainer
./gradlew startCheapRestSqliteContainer
./gradlew stopCheapRestSqliteContainer
./gradlew removeCheapRestSqliteContainer

# Orchestration tasks
./gradlew startDockerTestEnvironment  # Start all containers
./gradlew stopDockerTestEnvironment   # Stop all containers
./gradlew dockerIntegrationTest       # Run Docker integration tests
./gradlew allIntegrationTests         # Run all integration tests
```

## Usage Instructions

### Prerequisites
1. **Docker**: Docker Desktop must be installed and running
2. **Java 24**: Required for building the application
3. **Gradle**: Gradle 8.5+ (wrapper included)

### Running Tests

#### Step 1: Build the Docker Image
```bash
# Build cheap-rest JAR
./gradlew :cheap-rest:bootJar

# Build Docker image
cd cheap-rest
docker build -t cheap-rest:latest -t cheap-rest:0.1 ..
cd ..
```

#### Step 2: Run Docker Integration Tests

**Option A: Manual Docker Management**
```bash
# Start containers
./gradlew startDockerTestEnvironment

# Run tests
./gradlew dockerIntegrationTest

# Stop containers
./gradlew stopDockerTestEnvironment
```

**Option B: Using Docker Compose**
```bash
# Start all services
cd integration-tests
docker-compose up -d

# Run tests
cd ..
./gradlew dockerIntegrationTest

# Stop all services
cd integration-tests
docker-compose down -v
```

#### Step 3: Run All Integration Tests
```bash
# Run both embedded and Docker tests
./gradlew allIntegrationTests
```

### Test Execution Modes

1. **Embedded Tests Only** (default, fast)
   ```bash
   ./gradlew integrationTest
   ```
   - Runs tests with embedded databases
   - No Docker required
   - Suitable for quick feedback during development

2. **Docker Tests Only** (comprehensive)
   ```bash
   ./gradlew dockerIntegrationTest
   ```
   - Runs tests with Docker containers
   - Requires Docker daemon
   - Comprehensive validation of production-like environment

3. **All Tests** (complete validation)
   ```bash
   ./gradlew allIntegrationTests
   ```
   - Runs both embedded and Docker tests
   - Suitable for pre-release validation

### Test Filtering

Run tests by specific tags:

```bash
# Run only PostgreSQL tests (embedded + Docker)
./gradlew integrationTest --tests "*Postgres*"
./gradlew dockerIntegrationTest --tests "*Postgres*"

# Run only MariaDB tests
./gradlew dockerIntegrationTest --tests "*MariaDb*"

# Run only SQLite tests
./gradlew dockerIntegrationTest --tests "*Sqlite*"

# Run orchestration tests
./gradlew dockerIntegrationTest --tests "*Orchestration*"
```

## Architecture

### Test Architecture
- **Client**: Tests run in JVM process (NOT in Docker)
- **Server**: cheap-rest runs in Docker container
- **Database**: PostgreSQL/MariaDB run in separate Docker containers, SQLite embedded in cheap-rest container
- **Communication**: Tests ONLY use CheapRestClient (NO direct database access)

### Network Architecture
- All containers connected via Docker bridge network
- Network isolation between test runs
- Dynamic port mapping for host access
- Container-to-container communication via container names

### Data Persistence
- PostgreSQL: Volume-backed persistence
- MariaDB: Volume-backed persistence
- SQLite: Host-mounted volume for file persistence

## Verification

To verify the implementation is complete:

1. **Build Verification**
   ```bash
   ./gradlew :cheap-rest:bootJar
   ```
   Expected: JAR file created in `cheap-rest/build/libs/cheap-rest-0.1.jar`

2. **Docker Image Build** (requires Docker)
   ```bash
   cd cheap-rest
   docker build -t cheap-rest:latest ..
   ```
   Expected: Docker image built successfully

3. **Docker Compose Validation**
   ```bash
   cd integration-tests
   docker-compose config
   ```
   Expected: Valid compose configuration displayed

4. **Test Compilation**
   ```bash
   ./gradlew :integration-tests:compileIntegrationJava
   ```
   Expected: All test classes compile successfully

## CI/CD Integration

### Recommended CI Pipeline

```yaml
# Example GitHub Actions workflow
stages:
  - name: Fast Tests
    run: ./gradlew test integrationTest
    # Runs embedded integration tests (no Docker)

  - name: Docker Tests
    run: |
      docker build -t cheap-rest:latest .
      ./gradlew dockerIntegrationTest
    # Runs Docker-based tests (requires Docker)

  - name: Full Validation (Pre-Release)
    run: ./gradlew allIntegrationTests
    # Runs all tests (embedded + Docker)
```

## Notes

- Docker tests require Docker daemon to be running
- First test run may be slow due to Docker image downloads (postgres:17, mariadb:11)
- Subsequent runs use cached images and are much faster
- Container cleanup is automatic via `DockerContainerManager.close()`
- Failed tests may leave containers running - use `docker ps` and `docker rm -f` to clean up manually if needed

## Troubleshooting

### Docker Not Running
**Error**: `error during connect: ... dockerDesktopLinuxEngine: The system cannot find the file specified`
**Solution**: Start Docker Desktop and wait for it to be ready

### Port Conflicts
**Error**: `Bind for 0.0.0.0:XXXX failed: port is already allocated`
**Solution**: Stop other services using the same ports, or modify port mappings in docker-compose.yml

### Container Startup Timeout
**Error**: `Container did not become ready in time`
**Solution**: Increase timeout values in `DockerTestUtils.waitFor*` methods, or check container logs

### Image Build Failures
**Error**: Build failures during `docker build`
**Solution**: Ensure cheap-rest:bootJar completed successfully and JAR exists in `cheap-rest/build/libs/`

## Future Enhancements

Potential improvements for future consideration:

1. **Test Parallelization**: Run multiple Docker test classes in parallel
2. **Custom Health Checks**: More sophisticated health checks for cheap-rest containers
3. **Performance Tests**: Add performance benchmarks using Docker setup
4. **Volume Cleanup**: Automated volume cleanup after test completion
5. **Resource Limits**: Configure CPU/memory limits for containers
6. **Test Data Fixtures**: Pre-populate containers with test data
7. **Log Aggregation**: Centralized logging for all containers
