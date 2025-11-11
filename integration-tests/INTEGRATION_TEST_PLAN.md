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

### 2.1 PostgreSQL DAO with AspectTableMapping Tests
**Class**: `PostgresDaoAspectTableMappingTest`
- **Setup**: Use embedded-postgres, create two custom tables via AspectTableMapping
- **Table 1**: "person" table (entity_id, name, age) - standard entity table
- **Table 2**: "settings" table (catalog_id, entity_id, key, value) - composite PK
- **Tests**:
  - Register two AspectTableMappings with PostgresDao
  - Create tables using `dao.createTable(mapping)`
  - Save catalog with aspects mapped to custom tables
  - Load catalog and verify aspects loaded from custom tables
  - Update aspects and verify persistence
  - Delete catalog and verify custom table data cleaned up

### 2.2 SQLite DAO with AspectTableMapping Tests
**Class**: `SqliteDaoAspectTableMappingTest`
- **Setup**: Use temp file SQLite database, create two custom tables via AspectTableMapping
- **Table 1**: "product" table (entity_id, sku, name, price) - standard entity table
- **Table 2**: "category" table (catalog_id, category_name, description) - catalog-scoped only
- **Tests**: Same pattern as PostgreSQL test above

### 2.3 MariaDB DAO with AspectTableMapping Tests
**Class**: `MariaDbDaoAspectTableMappingTest`
- **Setup**: Use `DatabaseRunnerExtension` and `MariaDbTestDb`, create two custom tables
- **Table 1**: "employee" table (entity_id, employee_id, name, department)
- **Table 2**: "metadata" table (key, value) - no catalog_id or entity_id (lookup table pattern)
- **Tests**: Same pattern as PostgreSQL test above, plus test foreign key constraints if enabled

## 3. End-to-End REST Integration Tests (Service + Client)

### 3.1 PostgreSQL REST Integration Tests
**Class**: `PostgresRestClientIntegrationTest`
- **Setup**:
  - `@SpringBootTest` with `RANDOM_PORT` and `@ActiveProfiles("postgres-test")`
  - Initialize embedded PostgreSQL with schema
  - Create `CheapRestClient` pointing to `http://localhost:{port}`
  - Register AspectTableMapping for "address" table with PostgresDao

- **Test Suite**:
  1. **Catalog Lifecycle**: Create catalog via client, retrieve it, verify properties
  2. **AspectDef CRUD**: Create multiple aspect defs (including one for mapped table), list them, get by name/ID
  3. **Custom Table Mapping**: Verify "address" aspects stored in custom table via direct DB query
  4. **Aspect Upsert**: Upsert aspects using client, query them back
  5. **Entity List Hierarchy**: Create EntityList hierarchy, add entities, retrieve paginated
  6. **Entity Directory Hierarchy**: Create EntityDirectory, add entities, retrieve full tree
  7. **Aspect Map Hierarchy**: Create AspectMap, upsert aspects, retrieve with pagination
  8. **Error Handling**: Test 404s, 400s with invalid data

### 3.2 SQLite REST Integration Tests
**Class**: `SqliteRestClientIntegrationTest`
- **Setup**: Similar to PostgreSQL but with SQLite temp file database
- **Custom Table**: "order_item" table
- **Test Suite**: Same as PostgreSQL tests (8 tests covering all major operations)

### 3.3 MariaDB REST Integration Tests
**Class**: `MariaDbRestClientIntegrationTest`
- **Setup**: Use `DatabaseRunnerExtension`, create separate database per test class
- **Custom Table**: "inventory" table with foreign keys enabled
- **Test Suite**: Same as PostgreSQL tests plus verify foreign key constraints work

## 4. Cross-Database Consistency Tests

### 4.1 Multi-Database Validation Test
**Class**: `CrossDatabaseConsistencyTest`
- **Purpose**: Verify same operations produce consistent results across all databases
- **Approach**: Run same test scenario against all 3 databases, compare results
- **Tests**:
  1. Create identical catalog structure in all 3 databases
  2. Perform identical upsert operations via REST client
  3. Query data back and verify JSON responses are identical
  4. Test pagination consistency across databases
  5. Test sorting/ordering consistency

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
Expected: Cross-database consistency tests pass, identical operations produce identical results across all 3 databases.
**Remember**: Commit as you complete each test scenario within the consistency test class.

## 5. Complex Scenario Tests

### 5.1 Full Workflow Integration Test
**Class**: `FullWorkflowIntegrationTest` (parameterized for all 3 databases)
- **Scenario**: Simulate real-world usage pattern
  1. Create catalog
  2. Define multiple aspect types (person, address, phone)
  3. Register AspectTableMapping for one aspect type
  4. Create multiple hierarchies (EntityList, EntityDirectory, AspectMap)
  5. Bulk upsert 100+ entities with aspects
  6. Query aspects by entity IDs
  7. Retrieve hierarchies with pagination
  8. Update subset of entities
  9. Delete catalog and verify cleanup

### 5.2 Concurrent Operations Test
**Class**: `ConcurrentOperationsIntegrationTest`
- **Purpose**: Test thread safety and transaction isolation
- **Tests**:
  1. Concurrent aspect upserts from multiple clients
  2. Concurrent hierarchy retrievals while updates happening
  3. Verify data consistency after concurrent operations

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
Expected: Complex scenario tests pass, full workflow completes successfully, concurrent operations maintain data consistency.
**Remember**: Commit FullWorkflowIntegrationTest and ConcurrentOperationsIntegrationTest separately as each is completed.

## 6. Performance Baseline Tests

### 6.1 Performance Comparison Test
**Class**: `DatabasePerformanceBaselineTest`
- **Purpose**: Establish performance baselines for each database (not strict assertions, just measurements)
- **Measurements**:
  1. Time to upsert 1000 aspects (custom table vs standard tables)
  2. Time to query 1000 aspects
  3. Time to retrieve large EntityList hierarchy
  4. Document results for future optimization work

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
Expected: Performance baseline tests run successfully, measurements documented for all 3 databases.
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
- **Setup**:
  - Use docker-remote-api to start PostgreSQL container (official postgres:17 image)
  - Wait for database readiness (health check or connection retry)
  - Initialize schema using PostgresCheapSchema via JDBC
  - Start cheap-rest container configured for PostgreSQL
  - Wait for REST service readiness (actuator health endpoint)
  - Create CheapRestClient pointing to cheap-rest container

- **Test Suite**:
  - Full CRUD operations via REST client
  - AspectTableMapping test with custom "address" table
  - Verify data persists across cheap-rest container restarts
  - Test connection pooling and transaction handling

#### 7.4.2 MariaDB Docker Integration Test
**Class**: `MariaDbDockerIntegrationTest`
- **Setup**:
  - Start MariaDB container (official mariadb:11 image)
  - Wait for database readiness
  - Initialize schema with foreign keys
  - Start cheap-rest container configured for MariaDB
  - Create CheapRestClient

- **Test Suite**:
  - Same as PostgreSQL Docker test
  - Additional foreign key constraint validation
  - Test with and without foreign keys enabled

#### 7.4.3 SQLite Docker Integration Test
**Class**: `SqliteDockerIntegrationTest`
- **Setup**:
  - Start cheap-rest container with SQLite profile
  - Mount volume for SQLite database file
  - Create CheapRestClient

- **Test Suite**:
  - Same core tests as PostgreSQL
  - Test database file persistence across restarts
  - Verify file-based database isolation

#### 7.4.4 Multi-Container Orchestration Test
**Class**: `MultiDatabaseDockerOrchestrationTest`
- **Setup**:
  - Start all three database containers simultaneously
  - Start three cheap-rest containers (one per database)
  - Create three CheapRestClient instances

- **Tests**:
  - Perform same operations across all three services in parallel
  - Verify consistent behavior across all backends
  - Test network isolation between containers
  - Validate each service only accesses its designated database

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
- `waitForDatabaseReady(host, port, maxWaitSeconds)`: Wait for database connection
- `waitForRestServiceReady(url, maxWaitSeconds)`: Wait for REST service health check
- `initializeDatabaseSchema(dataSource, databaseType)`: Initialize schema via JDBC
- `getDynamicPort(containerId)`: Get dynamically mapped port
- `execInContainer(containerId, command)`: Execute command in container

**Class**: `DockerContainerManager`
- Manages lifecycle of test containers
- Provides fluent API for container configuration
- Handles cleanup on test failure
- Logs container output for debugging

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
