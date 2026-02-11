# integration-tests Module

This module provides guidance to Claude Code when working with integration tests for the Cheap project.

## Module Purpose

The integration-tests module is a standalone test-only subproject that contains heavyweight integration tests for the entire Cheap system. It tests the complete stack from REST API down through persistence layers.

## Test Architecture

This module contains two types of integration tests:

1. **Embedded Tests** (default, tag: none)
   - Use embedded databases (EmbeddedPostgres, MariaDB4j)
   - Fast, no Docker required
   - Suitable for development and CI

2. **Docker Tests** (tag: `@Tag("docker")`)
   - Use Docker containers for cheap-rest and databases
   - Comprehensive production-like environment
   - Require Docker daemon running

## Project Structure

```
src/integration/java/     # Integration test source code
src/integration/resources/ # Test resources
build.gradle.kts          # Gradle build with Docker tasks
docker-compose.yml        # Docker Compose configuration
DOCKER_INTEGRATION_TESTS.md # Detailed Docker test documentation
```

## Running Tests

```bash
# Embedded tests only (fast)
./gradlew :integration-tests:integrationTest

# Docker tests only (requires Docker)
./gradlew :integration-tests:dockerIntegrationTest

# All integration tests
./gradlew :integration-tests:allIntegrationTests

# Specific database tests
./gradlew :integration-tests:integrationTest --tests "*Postgres*"
./gradlew :integration-tests:dockerIntegrationTest --tests "*MariaDb*"
```

## Test Guidelines

### General
- All tests use `CheapRestClient` to interact with the REST API
- No direct database access from tests
- Tests validate end-to-end functionality across all layers

### Docker Tests
- Annotate with `@Tag("docker")`
- Must manage Docker containers via test code or Gradle tasks
- Tests assume containers are already running
- See DOCKER_INTEGRATION_TESTS.md for Docker management

### Test Data
- Use fixed UUIDs for reproducible tests
- Clean up test data between test methods
- Prefer unique catalog names per test to avoid conflicts

### Dependencies
- This module depends on ALL other modules
- Tests validate integration between modules
- Changes to any module may require test updates

## Common Tasks

### Adding a New Integration Test

1. Determine test type (embedded or Docker)
2. For Docker tests, add `@Tag("docker")` annotation
3. Use CheapRestClient to interact with API
4. Follow existing test patterns
5. Verify tests pass with both test modes

### Testing a New Feature

1. Add embedded tests first (faster development cycle)
2. Add Docker tests for comprehensive validation
3. Test with all supported databases (Postgres, MariaDB, SQLite)
4. Verify cleanup/teardown works correctly

## Key Technologies

- **Testing**: JUnit Jupiter with tags for test filtering
- **Build**: Gradle with Docker plugin (com.bmuschko.docker-remote-api)
- **Embedded Databases**: EmbeddedPostgres, MariaDB4j
- **Docker**: Docker Java Client API
- **HTTP Client**: Spring WebClient (via CheapRestClient)

## Related Documentation

- [DOCKER_INTEGRATION_TESTS.md](DOCKER_INTEGRATION_TESTS.md) - Detailed Docker test documentation
- [docker-compose.yml](docker-compose.yml) - Docker Compose configuration
- [../cheap-rest-client/CLAUDE.md](../cheap-rest-client/CLAUDE.md) - REST client guidance
