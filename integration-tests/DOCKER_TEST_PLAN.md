# Integration Tests Plan for cheap-rest with All Database Backends

## Development Workflow Guidelines

### Git Commit Strategy
**IMPORTANT**: During all phases of implementation, commit work to git in reasonably-sized chunks. Do NOT push commits.

- **Commit frequently**: After completing each test class, test method group, or logical unit of work
- **Use descriptive commit messages**: Clearly indicate what was implemented or fixed
- **Reasonably-sized commits**: Each commit should represent a coherent piece of work (e.g., one complete test class, configuration file, or utility class)
- **Do NOT push**: Keep all commits local until the entire phase or feature is complete and reviewed
- **Example commit pattern**:
  - "Add PostgresRestClientIntegrationTest with catalog lifecycle tests"
  - "Implement AspectDef CRUD tests in PostgresRestClientIntegrationTest"
  - "Add error handling tests to PostgresRestClientIntegrationTest"
  - "Complete PostgresRestClientIntegrationTest with all 8 test scenarios"

This approach ensures work is saved incrementally and provides clear rollback points if needed.

## 7. Docker-Based Integration Tests

### 7.1 Gradle Configuration
**File**: `integration-tests/build.gradle.kts`
- Add `com.bmuschko.docker-remote-api` plugin
- Define Docker tasks for:
  - Building cheap-rest Docker image
  - Starting PostgreSQL container
  - Starting MariaDB container
  - Starting cheap-rest containers (one per database backend)
  - Network creation for container communication
  - Container cleanup

### 7.2 Docker Image for cheap-rest
**File**: `cheap-rest/Dockerfile`
- Multi-stage build using Java 24 base image
- Copy JAR from Gradle build
- Expose port 8080
- Support environment variables for database configuration
- Configurable via `SPRING_PROFILES_ACTIVE` environment variable

### 7.3 Docker Compose Configuration (Optional)
**File**: `integration-tests/docker-compose.yml`
- Define services for:
  - PostgreSQL database
  - MariaDB database
  - cheap-rest-postgres (cheap-rest connected to PostgreSQL)
  - cheap-rest-mariadb (cheap-rest connected to MariaDB)
  - cheap-rest-sqlite (cheap-rest with embedded SQLite)
- Network configuration for inter-container communication
- Volume mounts for SQLite persistence
- Health checks for database readiness

### 7.4 Docker-Based Test Classes

#### 7.4.1 PostgreSQL Docker Integration Test
**Class**: `PostgresDockerIntegrationTest`
- **Architecture**:
  - Client test runs in JVM process (NOT in Docker)
  - Server (cheap-rest) runs in Docker container
  - PostgreSQL runs in Docker container
  - AspectTableMapping configured in cheap-rest Docker container (NOT in test)
  - Test ONLY uses CheapRestClient (NO database access)

- **Setup**:
  - Use docker-remote-api to start PostgreSQL container (official postgres:17 image)
  - Wait for database readiness (health check or connection retry)
  - Schema initialization handled by cheap-rest container on startup
  - Start cheap-rest Docker container configured for PostgreSQL with "address" AspectTableMapping pre-configured
  - Wait for REST service readiness (actuator health endpoint)
  - Create CheapRestClient in test pointing to cheap-rest container
  - NO database beans injected
  - NO direct database access

- **Test Suite** (ALL via REST client ONLY):
  - Full CRUD operations via REST client
  - AspectTableMapping test via REST client (upsert and query "address" aspects)
  - Verify data persists across cheap-rest container restarts (by querying via client before and after restart)
  - Test connection pooling and transaction handling via repeated client operations

#### 7.4.2 MariaDB Docker Integration Test
**Class**: `MariaDbDockerIntegrationTest`
- **Architecture**:
  - Client test runs in JVM process (NOT in Docker)
  - Server (cheap-rest) runs in Docker container
  - MariaDB runs in Docker container
  - Test ONLY uses CheapRestClient

- **Setup**:
  - Start MariaDB container (official mariadb:11 image)
  - Wait for database readiness
  - Schema initialization handled by cheap-rest container on startup
  - Start cheap-rest Docker container configured for MariaDB with "inventory" AspectTableMapping pre-configured
  - Create CheapRestClient in test
  - NO database beans injected
  - NO direct database access

- **Test Suite** (ALL via REST client ONLY):
  - Same as PostgreSQL Docker test
  - Foreign key constraint validation via client (attempt operations that would violate constraints, verify error responses)

#### 7.4.3 SQLite Docker Integration Test
**Class**: `SqliteDockerIntegrationTest`
- **Architecture**:
  - Client test runs in JVM process (NOT in Docker)
  - Server (cheap-rest) runs in Docker container with SQLite
  - Test ONLY uses CheapRestClient

- **Setup**:
  - Start cheap-rest Docker container with SQLite profile and "order_item" AspectTableMapping pre-configured
  - Mount volume for SQLite database file
  - Create CheapRestClient in test
  - NO database beans injected
  - NO direct database access

- **Test Suite** (ALL via REST client ONLY):
  - Same core tests as PostgreSQL
  - Test database file persistence across restarts via client queries

#### 7.4.4 Multi-Container Orchestration Test
**Class**: `MultiDatabaseDockerOrchestrationTest`
- **Architecture**:
  - Client test runs in JVM process (NOT in Docker)
  - Three separate cheap-rest servers run in Docker containers
  - Three database containers (PostgreSQL, MariaDB, SQLite)
  - Test uses three separate CheapRestClient instances

- **Setup**:
  - Start all three database containers simultaneously
  - Start three cheap-rest containers (one per database), each with appropriate AspectTableMapping pre-configured
  - Create three CheapRestClient instances in test
  - NO database beans injected
  - NO direct database access

- **Tests** (ALL via REST clients ONLY):
  - Perform same operations across all three services in parallel via clients
  - Verify consistent behavior across all backends via client responses
  - Test network isolation by verifying each client can only access its designated server
  - Validate each service only accesses its designated database (indirectly via client operations)

### 7.5 Gradle Docker Tasks

Define these tasks in `integration-tests/build.gradle.kts`:

```kotlin
// Docker image tasks
- buildCheapRestImage: Build Docker image for cheap-rest
- removeCheapRestImage: Remove Docker image (cleanup)

// PostgreSQL container tasks
- createPostgresContainer: Create PostgreSQL container
- startPostgresContainer: Start PostgreSQL container
- stopPostgresContainer: Stop PostgreSQL container
- removePostgresContainer: Remove PostgreSQL container

// MariaDB container tasks
- createMariaDbContainer: Create MariaDB container
- startMariaDbContainer: Start MariaDB container
- stopMariaDbContainer: Stop MariaDB container
- removeMariaDbContainer: Remove MariaDB container

// cheap-rest container tasks (per database)
- createCheapRestPostgresContainer: Create cheap-rest with PostgreSQL
- startCheapRestPostgresContainer: Start cheap-rest-postgres
- stopCheapRestPostgresContainer: Stop cheap-rest-postgres
- removeCheapRestPostgresContainer: Remove cheap-rest-postgres
(Similar tasks for MariaDB and SQLite variants)

// Network tasks
- createDockerNetwork: Create Docker network for containers
- removeDockerNetwork: Remove Docker network

// Orchestration tasks
- startDockerTestEnvironment: Start all containers
- stopDockerTestEnvironment: Stop all containers
- dockerIntegrationTest: Run Docker-based integration tests

// Composite task
- integrationTestWithDocker: Run all Docker tests
```

### 7.6 Docker Test Lifecycle Management

**Container Management Strategy**:
- Use JUnit 5 `@BeforeAll` / `@AfterAll` for container lifecycle
- Start containers once per test class
- Clean database between tests (truncate tables)
- Stop and remove containers after all tests complete

**Health Check Strategy**:
- Poll database containers until accepting connections
- Poll cheap-rest containers until actuator `/actuator/health` returns UP
- Use exponential backoff for retries
- Fail fast with clear error messages if containers don't start

**Port Mapping**:
- Use dynamic port assignment to avoid conflicts
- Retrieve mapped ports from Docker API
- Configure connection strings dynamically

### 7.7 Docker Test Utilities

**Class**: `DockerTestUtils`
- `waitForDatabaseReady(host, port, maxWaitSeconds)`: Wait for database container to be ready (uses health check, NOT direct connection)
- `waitForRestServiceReady(url, maxWaitSeconds)`: Wait for REST service health check via HTTP
- `getDynamicPort(containerId)`: Get dynamically mapped port from Docker API
- `execInContainer(containerId, command)`: Execute command in container (for debugging only)
- NO `initializeDatabaseSchema()` method - schema initialization handled by cheap-rest containers

**Class**: `DockerContainerManager`
- Manages lifecycle of test containers
- Provides fluent API for container configuration
- Handles cleanup on test failure
- Logs container output for debugging
- NO database connection management - only container lifecycle

### 7.8 Phase 7 Verification
After completing Phase 7 implementation:
```bash
# Build Docker images
./gradlew buildCheapRestImage

# Start Docker test environment
./gradlew startDockerTestEnvironment

# Run Docker integration tests
./gradlew dockerIntegrationTest

# Stop Docker test environment
./gradlew stopDockerTestEnvironment

# Commit work (do NOT push)
git add .
git commit -m "Complete Phase 7: Docker-based integration tests"
```
Expected: Docker containers start successfully, Docker-based integration tests pass, all 3 database backends work in Docker environment.
**Remember**: Commit incrementally - Dockerfile, docker-compose.yml, Gradle Docker tasks, each Docker test class, and utilities separately.

## 8. Test Execution Strategy

### 8.1 Test Organization
- **Embedded Tests** (Sections 2-6): Fast, run by default with `./gradlew integration-tests`
- **Docker Tests** (Section 7): Slower, run separately with `./gradlew dockerIntegrationTest`
- Use JUnit `@Tag` annotations:
  - `@Tag("embedded")` for embedded database tests
  - `@Tag("docker")` for Docker-based tests
  - `@Tag("postgres")`, `@Tag("mariadb")`, `@Tag("sqlite")` for database-specific tests

### 8.2 Test Execution Modes
- **Default mode**: Run embedded tests only (faster CI builds)
- **Docker mode**: Run Docker tests only (comprehensive validation)
- **Full mode**: Run both embedded and Docker tests (release validation)

### 8.3 Gradle Task Configuration
```kotlin
// Default integration test task (embedded only)
./gradlew integration-tests

// Docker integration tests only
./gradlew dockerIntegrationTest

// All integration tests
./gradlew allIntegrationTests
```

### 8.4 CI/CD Considerations
- Embedded tests: Run on every commit
- Docker tests: Run on PR merge or nightly builds
- Require Docker daemon available for Docker tests
- Use GitHub Actions or similar with Docker support

