# Integration Tests

Standalone integration test suite for the Cheap project. Tests the complete stack from REST API through persistence layers using both embedded and Docker-based databases.

## Quick Start

### Embedded Tests (Fast, No Docker Required)

```bash
./gradlew :integration-tests:integrationTest
```

Runs integration tests with embedded databases (EmbeddedPostgres, MariaDB4j). Suitable for rapid development and CI pipelines.

### Docker Tests (Comprehensive, Requires Docker)

```bash
# Start containers
./gradlew :integration-tests:startDockerTestEnvironment

# Run tests
./gradlew :integration-tests:dockerIntegrationTest

# Stop containers
./gradlew :integration-tests:stopDockerTestEnvironment
```

Or use Docker Compose:

```bash
cd integration-tests
docker-compose up -d
cd ..
./gradlew :integration-tests:dockerIntegrationTest
cd integration-tests
docker-compose down -v
```

### All Tests

```bash
./gradlew :integration-tests:allIntegrationTests
```

Runs both embedded and Docker tests.

## Test Types

| Test Type | Tag | Requirements | Use Case |
|-----------|-----|--------------|----------|
| **Embedded** | None | None | Development, fast CI |
| **Docker** | `@Tag("docker")` | Docker daemon | Comprehensive validation |

## Test Filtering

Run tests for specific databases:

```bash
# PostgreSQL tests
./gradlew :integration-tests:integrationTest --tests "*Postgres*"

# MariaDB tests
./gradlew :integration-tests:dockerIntegrationTest --tests "*MariaDb*"

# SQLite tests
./gradlew :integration-tests:dockerIntegrationTest --tests "*Sqlite*"
```

## Architecture

- **Tests**: Run in JVM process (not in containers)
- **Client**: Uses `CheapRestClient` to interact with REST API
- **Server**: `cheap-rest` runs in-process (embedded) or in Docker container
- **Databases**: Embedded or Docker containers (PostgreSQL, MariaDB, SQLite)

Tests validate end-to-end functionality without direct database access.

## Documentation

- **[DOCKER_INTEGRATION_TESTS.md](DOCKER_INTEGRATION_TESTS.md)** - Detailed Docker test guide
- **[CLAUDE.md](CLAUDE.md)** - Development guidelines for Claude Code
- **[docker-compose.yml](docker-compose.yml)** - Docker Compose configuration

## Prerequisites

- **Java 24**: Required for all tests
- **Gradle 8.5+**: Included via wrapper
- **Docker**: Only for Docker-based tests

## Troubleshooting

### Docker Not Running
```
Error: error during connect: ... dockerDesktopLinuxEngine
```
**Solution**: Start Docker Desktop and ensure it's fully initialized.

### Port Conflicts
```
Error: Bind for 0.0.0.0:XXXX failed: port is already allocated
```
**Solution**: Stop services using the same ports or modify `docker-compose.yml`.

### Leftover Containers
```bash
# List all containers
docker ps -a

# Remove specific container
docker rm -f cheap-rest-postgres-test

# Remove all test containers
docker rm -f $(docker ps -a | grep cheap- | awk '{print $1}')
```

## CI/CD Usage

Recommended pipeline stages:

```bash
# Stage 1: Fast feedback (no Docker)
./gradlew test :integration-tests:integrationTest

# Stage 2: Comprehensive validation (requires Docker)
./gradlew :integration-tests:dockerIntegrationTest

# Stage 3: Full suite (pre-release)
./gradlew :integration-tests:allIntegrationTests
```
