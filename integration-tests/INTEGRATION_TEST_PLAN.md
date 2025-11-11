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

## 1. Test Infrastructure & Utilities

### 1.1 Base Test Classes
- **`BaseRestIntegrationTest`**: Abstract base class for all REST integration tests
  - Extends `@SpringBootTest` with `RANDOM_PORT`
  - Provides database cleanup methods
  - Provides helper methods for common operations (create catalog, create aspect def, etc.)
  - Provides JSON loading utilities

- **Database-specific base classes**:
  - **`PostgresRestIntegrationTest`**: Sets up embedded PostgreSQL, initializes schema
  - **`SqliteRestIntegrationTest`**: Sets up in-memory/temp SQLite database, initializes schema
  - **`MariaDbRestIntegrationTest`**: Uses `DatabaseRunnerExtension` and `MariaDbTestDb` pattern

### 1.2 Test Configuration Files
- `application-postgres-test.yml`: PostgreSQL test configuration
- `application-sqlite-test.yml`: SQLite test configuration
- `application-mariadb-test.yml`: MariaDB test configuration
- Each configures appropriate `cheap.database.type` and datasource settings

### 1.3 Test Data & Resources
- `test/resources/integration-tests/` directory for shared JSON test data
- Sample catalog definitions, aspect definitions, entity data

## 2. Database-Specific Integration Tests (Direct DAO Testing)

These phase 2 tests were removed, they are not in scope for integration testing.

### 2.1 PostgreSQL DAO with AspectTableMapping Tests
### 2.2 SQLite DAO with AspectTableMapping Tests
### 2.3 MariaDB DAO with AspectTableMapping Tests

## 3. End-to-End REST Integration Tests (Service + Client)

### Architecture Overview

The Phase 3 tests follow a strict client-server separation architecture:

**Client Side** (Test Process):
- Separate Spring Boot Application containing ONLY cheap-rest-client dependencies
- Tests run in this application context
- NO cheap-rest or cheap-db-* dependencies
- NO direct database access
- Uses `@ContextConfiguration` to load client-only configuration

**Server Side** (3 Separate Applications):
- **PostgreSQL Server**: Spring Boot app with cheap-rest + cheap-db-postgres ONLY
- **SQLite Server**: Spring Boot app with cheap-rest + cheap-db-sqlite ONLY
- **MariaDB Server**: Spring Boot app with cheap-rest + cheap-db-mariadb ONLY
- Each server runs on a different port
- AspectTableMapping configured on server startup
- Uses `@ContextConfiguration` to load database-specific server configurations

**Key Principles**:
1. Tests NEVER access CheapDao or any server-side beans
2. Tests NEVER perform direct database operations
3. Tests ONLY interact with cheap-rest-client
4. All verification is done through REST API responses
5. Each database server is completely isolated from the others

### 3.1 Test Infrastructure

#### 3.1.1 Client Configuration
**Class**: `ClientTestConfig`
- Spring Boot configuration for test client
- Provides `CheapRestClient` beans configured for each server
- NO database dependencies
- NO cheap-rest dependencies

#### 3.1.2 Server Configurations
**PostgreSQL Server Config**: `PostgresServerTestConfig`
- Loads cheap-rest with ONLY cheap-db-postgres
- Configures embedded PostgreSQL
- Registers AspectTableMapping for "address" table on startup
- Starts on port 8081

**SQLite Server Config**: `SqliteServerTestConfig`
- Loads cheap-rest with ONLY cheap-db-sqlite
- Configures SQLite database
- Registers AspectTableMapping for "order_item" table on startup
- Starts on port 8082

**MariaDB Server Config**: `MariaDbServerTestConfig`
- Loads cheap-rest with ONLY cheap-db-mariadb
- Configures MariaDB test database
- Registers AspectTableMapping for "inventory" table on startup
- Starts on port 8083

#### 3.1.3 Base Test Classes
**Class**: `BaseClientIntegrationTest`
- Abstract base for all client tests
- Provides utility methods (testUuid, JSON parsing, etc.)
- NO `@SpringBootTest` annotation (subclasses add their own)
- NO database access methods
- Provides helper methods for common client operations

**Database-Specific Base Classes**:
- `PostgresClientIntegrationTest`: Configures client for PostgreSQL server (port 8081)
- `SqliteClientIntegrationTest`: Configures client for SQLite server (port 8082)
- `MariaDbClientIntegrationTest`: Configures client for MariaDB server (port 8083)

Each uses `@ContextConfiguration` to load:
1. The appropriate server configuration (as a separate context)
2. The client configuration (as the test context)

### 3.2 PostgreSQL REST Integration Tests
**Class**: `PostgresRestClientIntegrationTest`
- **Setup**:
  - Extends `PostgresClientIntegrationTest`
  - Uses `@ContextConfiguration` to load PostgreSQL server + client configs
  - Gets `CheapRestClient` bean via `@Autowired`
  - NO CheapDao injection
  - NO database setup code in tests

- **Test Suite** (ALL tests use ONLY CheapRestClient):
  1. **Catalog Lifecycle**: Create catalog via client, retrieve it, verify properties via client responses
  2. **AspectDef CRUD**: Create multiple aspect defs (including "address" for mapped table), list them, get by name/ID - all via client
  3. **Custom Table Mapping**: Upsert "address" aspects via client, query them back via client to verify they were stored and retrieved correctly
  4. **Aspect Upsert**: Upsert aspects using client, query them back via client
  5. **Entity List Hierarchy**: Create EntityList via REST API (new endpoint needed), add entities via API, retrieve paginated via client
  6. **Entity Directory Hierarchy**: Create EntityDirectory via REST API, add entries via API, retrieve via client
  7. **Aspect Map Hierarchy**: Create AspectMap via REST API, upsert aspects via API, retrieve with pagination via client
  8. **Error Handling**: Test 404s, 400s with invalid data via client, verify error responses

**Note**: Tests that previously used DAO directly (entityListHierarchy, entityDirectoryHierarchy, aspectMapHierarchy) need REST API endpoints to create and populate hierarchies. Alternative: Pre-populate test data on server startup.

### 3.3 SQLite REST Integration Tests
**Class**: `SqliteRestClientIntegrationTest`
- **Setup**:
  - Extends `SqliteClientIntegrationTest`
  - Uses `@ContextConfiguration` to load SQLite server + client configs
  - Gets `CheapRestClient` bean via `@Autowired`
  - NO direct database access

- **Custom Table**: "order_item" table (configured on server)
- **Test Suite**: Same as PostgreSQL tests (8 tests, all using ONLY client)

### 3.4 MariaDB REST Integration Tests
**Class**: `MariaDbRestClientIntegrationTest`
- **Setup**:
  - Extends `MariaDbClientIntegrationTest`
  - Uses `@ContextConfiguration` to load MariaDB server + client configs
  - Gets `CheapRestClient` bean via `@Autowired`
  - NO direct database access

- **Custom Table**: "inventory" table (configured on server)
- **Test Suite**: Same as PostgreSQL tests plus:
  9. **Foreign Key Constraints**: Verify foreign key behavior through client operations only (check error responses when constraints violated)

### 3.5 Phase 3 Implementation Steps

1. **Create Client Configuration** (`ClientTestConfig.java`)
   - Minimal Spring Boot configuration
   - Bean factory methods for `CheapRestClient` instances

2. **Create Server Configurations** (one per database)
   - `PostgresServerTestConfig.java`
   - `SqliteServerTestConfig.java`
   - `MariaDbServerTestConfig.java`
   - Each registers AspectTableMapping on startup

3. **Create Base Test Classes**
   - `BaseClientIntegrationTest.java` - Common utilities, no Spring annotations
   - `PostgresClientIntegrationTest.java` - Loads Postgres server + client
   - `SqliteClientIntegrationTest.java` - Loads SQLite server + client
   - `MariaDbClientIntegrationTest.java` - Loads MariaDB server + client

4. **Modify Existing Test Classes**
   - Remove all `@Autowired CheapDao` injections
   - Remove all `@Autowired CheapFactory` injections (move to server config)
   - Remove all direct database access (`getDataSource()`, SQL queries)
   - Remove all `cheapDao.loadCatalog()` / `cheapDao.saveCatalog()` calls
   - Replace with client-only operations
   - Update hierarchy tests to use REST API only (or pre-populated test data)

5. **Add Missing REST Endpoints** (if needed)
   - Hierarchy creation endpoints (POST /catalogs/{id}/hierarchies/entity-list/{name})
   - Hierarchy population endpoints (POST /catalogs/{id}/hierarchies/entity-list/{name}/entities)
   - Or: Add server-side test data initialization

### 3.6 Phase 3 Verification
After completing Phase 3 implementation:
```bash
# Build integration tests
./gradlew :integration-tests:build

# Run all REST client tests
./gradlew :integration-tests:integrationTest --tests "*RestClient*"

# Run database-specific tests
./gradlew :integration-tests:integrationTest --tests "*PostgresRestClient*"
./gradlew :integration-tests:integrationTest --tests "*SqliteRestClient*"
./gradlew :integration-tests:integrationTest --tests "*MariaDbRestClient*"

# Commit work (do NOT push)
git add .
git commit -m "Complete Phase 3: REST client integration tests with proper client-server separation"
```

Expected: All REST client tests pass, each database server runs in complete isolation, tests never access database directly.

## 4. Cross-Database Consistency Tests

### 4.1 Multi-Database Validation Test
**Class**: `CrossDatabaseConsistencyTest`
- **Purpose**: Verify same operations produce consistent results across all databases
- **Architecture**:
  - Uses `@ContextConfiguration` to load ALL THREE server configurations + client configuration
  - Gets three separate `CheapRestClient` beans (one for each server)
  - NO direct database access
  - ALL verification through REST API responses only

- **Setup**:
  - Extends `BaseClientIntegrationTest`
  - Loads all three server configs (PostgreSQL on 8081, SQLite on 8082, MariaDB on 8083)
  - Injects three separate `CheapRestClient` beans via `@Autowired` `@Qualifier`
  - NO database beans injected

- **Tests** (all using ONLY REST clients):
  1. **Catalog Structure Consistency**: Create identical catalogs via each client, verify responses are structurally identical
  2. **AspectDef Consistency**: Create identical aspect defs via each client, compare JSON responses
  3. **Upsert Consistency**: Perform identical upsert operations via all three clients, verify success responses are identical
  4. **Query Consistency**: Query same data from all three servers via clients, verify JSON responses are identical (ignoring database-specific fields if any)
  5. **Pagination Consistency**: Test pagination with identical parameters across all databases via clients, verify page structures and content are identical
  6. **Sorting/Ordering Consistency**: Test ordering with same parameters across all databases via clients, verify sort order is identical

### 4.2 Phase 4 Verification
After completing Phase 4 implementation:
```bash
# Build and run all integration tests
./gradlew integration-tests

# Run consistency tests
./gradlew :integration-tests:integrationTest --tests "*Consistency*"

# Commit work (do NOT push)
git add .
git commit -m "Complete Phase 4: Cross-database consistency tests"
```
Expected: Cross-database consistency tests pass, identical operations produce identical results across all 3 databases, all verified through REST API only.
**Remember**: Commit as you complete each test scenario within the consistency test class.

## 5. Complex Scenario Tests

### 5.1 Full Workflow Integration Test
**Class**: `FullWorkflowIntegrationTest` (parameterized for all 3 databases)
- **Architecture**:
  - Uses `@ParameterizedTest` to run against all three database servers
  - Each test iteration gets a different `CheapRestClient` (one per database)
  - NO direct database access
  - ALL operations through REST API only

- **Setup**:
  - Extends `BaseClientIntegrationTest`
  - Loads all three server configurations
  - Uses `@MethodSource` to provide client instances for each database
  - NO CheapDao or CheapFactory injection

- **Scenario** (Simulate real-world usage pattern via REST API ONLY):
  1. Create catalog via client
  2. Define multiple aspect types (person, address, phone) via client
  3. AspectTableMapping already registered on server (no test involvement)
  4. Create multiple hierarchies via REST API (EntityList, EntityDirectory, AspectMap)
  5. Bulk upsert 100+ entities with aspects via client
  6. Query aspects by entity IDs via client
  7. Retrieve hierarchies with pagination via client
  8. Update subset of entities via client
  9. Delete catalog via client (if DELETE endpoint exists) or verify data isolation

**Note**: Step 4 requires REST endpoints for hierarchy creation, or test data can be pre-populated on server.

### 5.2 Concurrent Operations Test
**Class**: `ConcurrentOperationsIntegrationTest`
- **Architecture**:
  - Creates multiple `CheapRestClient` instances (all pointing to same server)
  - Uses `ExecutorService` to run concurrent operations
  - NO direct database access
  - Verifies consistency through REST API queries only

- **Setup**:
  - Extends `PostgresClientIntegrationTest` (test against one database is sufficient)
  - Creates 10+ `CheapRestClient` instances for concurrent operations
  - NO database beans injected

- **Tests** (all using ONLY REST clients):
  1. **Concurrent Aspect Upserts**: Launch 10 threads, each upserting different aspects via separate client instances, verify all upserts succeed via client queries
  2. **Concurrent Reads During Writes**: Launch reader threads retrieving hierarchies while writer threads update aspects, verify no errors and eventual consistency via client
  3. **Data Consistency After Concurrent Ops**: Perform concurrent operations, then query final state via client and verify data integrity

### 5.3 Phase 5 Verification
After completing Phase 5 implementation:
```bash
# Build and run all integration tests
./gradlew integration-tests

# Run workflow tests
./gradlew :integration-tests:integrationTest --tests "*Workflow*"
./gradlew :integration-tests:integrationTest --tests "*Concurrent*"

# Commit work (do NOT push)
git add .
git commit -m "Complete Phase 5: Complex scenario and workflow tests"
```
Expected: Complex scenario tests pass, full workflow completes successfully, concurrent operations maintain data consistency, all verified through REST API only.
**Remember**: Commit FullWorkflowIntegrationTest and ConcurrentOperationsIntegrationTest separately as each is completed.

## 6. Performance Baseline Tests

### 6.1 Performance Comparison Test
**Class**: `DatabasePerformanceBaselineTest`
- **Architecture**:
  - Uses `@ParameterizedTest` to test all three database servers
  - Each test iteration gets a different `CheapRestClient` (one per database)
  - NO direct database access
  - ALL operations through REST API only
  - Measures end-to-end REST API performance (not just database performance)

- **Setup**:
  - Extends `BaseClientIntegrationTest`
  - Loads all three server configurations
  - Uses `@MethodSource` to provide client instances for each database
  - NO database beans injected

- **Purpose**: Establish performance baselines for each database (not strict assertions, just measurements)

- **Measurements** (all via REST client):
  1. **Bulk Upsert Performance**: Time to upsert 1000 aspects via client (both custom table and standard tables)
  2. **Query Performance**: Time to query 1000 aspects via client
  3. **Hierarchy Retrieval Performance**: Time to retrieve large EntityList hierarchy via client
  4. **Pagination Performance**: Time to paginate through 1000 entities via client
  5. Document results for future optimization work

- **Output**: Console and/or log file with timing results for each database

### 6.2 Phase 6 Verification
After completing Phase 6 implementation:
```bash
# Build and run all integration tests
./gradlew integration-tests

# Run performance tests
./gradlew :integration-tests:integrationTest --tests "*Performance*"

# Commit work (do NOT push)
git add .
git commit -m "Complete Phase 6: Performance baseline tests"
```
Expected: Performance baseline tests run successfully, measurements documented for all 3 databases, all measurements taken via REST API.
**Remember**: Commit the performance test class and any documentation of results.

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

## 9. Documentation and Maintenance

### 9.1 Documentation Files
- **`INTEGRATION_TESTS.md`**: Overview of integration test suite
- **`DOCKER_TESTING.md`**: Docker-based testing guide
- **`TROUBLESHOOTING.md`**: Common issues and solutions

### 9.2 Test Data Management
- Use consistent test data across embedded and Docker tests
- Provide test data generators for reproducible datasets
- Document test data schemas and expected values

## Summary

**Total Test Classes**: ~18-20 classes
- Embedded database tests: 11-12 classes
- Docker-based tests: 7-8 classes

**Total Test Methods**: ~100-120 tests

**Coverage**:
- ✅ All 3 database backends (PostgreSQL, SQLite, MariaDB)
- ✅ AspectTableMapping with at least 2 custom tables per database
- ✅ Direct DAO testing for each database
- ✅ End-to-end REST client integration tests
- ✅ All major REST endpoints (catalogs, aspect defs, aspects, hierarchies)
- ✅ All hierarchy types (EntityList, EntitySet, EntityDirectory, EntityTree, AspectMap)
- ✅ Error handling and edge cases
- ✅ Cross-database consistency validation
- ✅ Docker-based testing with real database containers
- ✅ Multi-container orchestration tests
- ✅ Performance baseline measurements

**Next Steps**:
1. ✅ Write this plan to `integration-tests/INTEGRATION_TEST_PLAN.md`
2. Begin implementation with test infrastructure (Section 1)
3. Implement embedded tests first (Sections 2-6)
4. Add Docker support and Docker tests (Section 7)
5. Validate test coverage and add missing scenarios
