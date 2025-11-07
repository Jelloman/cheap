# cheap-rest Module

This file provides guidance to Claude Code when working with the cheap-rest module.

## Module Overview

The cheap-rest module is a Spring Boot REST API service that exposes Cheap catalog functionality through HTTP endpoints. It supports multiple database backends (PostgreSQL, SQLite, MariaDB) selectable via Spring profiles.

## Documentation

This module has comprehensive documentation in separate files:

- [README.md](README.md) - Complete API documentation, endpoints, configuration, and usage
- [DEPLOYMENT.md](DEPLOYMENT.md) - Deployment guide for Docker, JAR, and production
- [OPENAPI.md](OPENAPI.md) - OpenAPI/Swagger documentation details

Always refer to these files for specific information.

## Package Structure

```
net.netbeing.cheap.rest/
├── controller/     # REST controllers (endpoints)
├── service/        # Business logic layer
├── dto/            # Data transfer objects
├── config/         # Spring configuration classes
└── exception/      # Exception handlers
```

## Development Guidelines

### Adding a New Endpoint

1. **Create/Update DTOs** in `dto/` package:
   ```java
   public record NewOperationRequest(
       UUID catalogId,
       String parameter
   ) {}

   public record NewOperationResponse(
       boolean success,
       String message
   ) {}
   ```

2. **Add Service Method** in appropriate service class:
   ```java
   @Service
   public class CatalogService {
       public NewOperationResponse performOperation(NewOperationRequest request) {
           // Business logic here
       }
   }
   ```

3. **Add Controller Endpoint** with OpenAPI annotations:
   ```java
   @RestController
   @RequestMapping("/api/catalog")
   public class CatalogController {

       @Operation(summary = "Perform new operation")
       @ApiResponses(value = {
           @ApiResponse(responseCode = "200", description = "Operation successful"),
           @ApiResponse(responseCode = "400", description = "Invalid request")
       })
       @PostMapping("/{catalogId}/operation")
       public NewOperationResponse performOperation(
           @PathVariable UUID catalogId,
           @RequestBody NewOperationRequest request
       ) {
           return catalogService.performOperation(request);
       }
   }
   ```

4. **Write Tests** for controller and service:
   ```java
   @WebFluxTest(CatalogController.class)
   class CatalogControllerTest {
       @Test
       void testNewOperation() {
           // Test implementation
       }
   }
   ```

5. **Update Documentation**:
   - Add endpoint to README.md
   - OpenAPI annotations auto-update Swagger UI
   - Update DEPLOYMENT.md if deployment changes needed

### Spring Profile Configuration

The module supports three database profiles:

#### PostgreSQL Profile (`postgres`)
```yaml
# application-postgres.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cheap
    username: cheap_user
    password: ${DB_PASSWORD}

cheap:
  database:
    type: postgres
```

#### SQLite Profile (`sqlite`)
```yaml
# application-sqlite.yml
spring:
  datasource:
    url: jdbc:sqlite:${CHEAP_DB_PATH:./cheap.db}

cheap:
  database:
    type: sqlite
```

#### MariaDB Profile (`mariadb`)
```yaml
# application-mariadb.yml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/cheap
    username: cheap_user
    password: ${DB_PASSWORD}

cheap:
  database:
    type: mariadb
```

### Running with Different Profiles

```bash
# PostgreSQL
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=postgres'

# SQLite (default for development)
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=sqlite'

# MariaDB
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=mariadb'
```

### Exception Handling

Use the centralized exception handler:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCatalogNotFound(
        CatalogNotFoundException e
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

### Validation

Use Jakarta Bean Validation annotations on DTOs:

```java
public record CreateCatalogRequest(
    @NotNull CatalogDef catalogDef,
    @NotNull CatalogSpecies species,
    @Valid URI uri
) {}
```

Enable validation in controllers:

```java
@PostMapping
public CreateCatalogResponse create(
    @Valid @RequestBody CreateCatalogRequest request
) {
    // Request is validated automatically
}
```

### Transaction Management

Use `@Transactional` for operations that modify data:

```java
@Service
public class CatalogService {

    @Transactional
    public void saveCatalog(Catalog catalog) {
        dao.saveCatalog(catalog);
        // Automatically rolled back on exception
    }

    @Transactional(readOnly = true)
    public Catalog getCatalog(UUID id) {
        return dao.loadCatalog(id);
    }
}
```

## Testing Guidelines

### Controller Tests

Use `@WebFluxTest` for controller unit tests:

```java
@WebFluxTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private CatalogService catalogService;

    @Test
    void testCreateCatalog() {
        CreateCatalogRequest request = new CreateCatalogRequest(...);

        webClient.post()
            .uri("/api/catalog")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(CreateCatalogResponse.class);
    }
}
```

### Service Tests

Test business logic without web layer:

```java
@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CatalogDao dao;

    @InjectMocks
    private CatalogService service;

    @Test
    void testSaveCatalog() {
        // Test service logic
    }
}
```

### Integration Tests

Use `@SpringBootTest` for full integration tests:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CatalogIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    void testFullCatalogLifecycle() {
        // Test complete workflow
    }
}
```

### Testing with Different Databases

Configure test profile for each database:

```java
@SpringBootTest
@ActiveProfiles("postgres-test")
class PostgresIntegrationTest {
    // Tests with PostgreSQL
}

@SpringBootTest
@ActiveProfiles("sqlite-test")
class SqliteIntegrationTest {
    // Tests with SQLite (faster)
}
```

## Common Tasks

### Adding a New Controller

1. Create controller class in `controller/` package
2. Add `@RestController` and `@RequestMapping` annotations
3. Add `@Tag` annotation for OpenAPI grouping
4. Implement endpoints with proper annotations
5. Write controller tests

### Adding Business Logic

1. Create/update service class in `service/` package
2. Add `@Service` annotation
3. Inject dependencies via constructor
4. Use `@Transactional` for data modifications
5. Write service tests

### Adding Configuration

1. Create config class in `config/` package
2. Add `@Configuration` annotation
3. Define beans with `@Bean` methods
4. Document configuration properties

Example:
```java
@Configuration
public class CheapConfig {

    @Bean
    public CatalogDao catalogDao(DataSource dataSource) {
        return new CatalogDao(dataSource);
    }
}
```

### Adding Health Checks

Spring Boot Actuator is included. Add custom health indicators:

```java
@Component
public class CatalogHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // Check catalog system health
            return Health.up()
                .withDetail("catalogs", catalogCount)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

## OpenAPI/Swagger Integration

### Accessing Swagger UI

```
http://localhost:8080/swagger-ui.html
```

### OpenAPI Specification

```
http://localhost:8080/api-docs
```

### Documenting Endpoints

Use OpenAPI annotations:

```java
@Operation(
    summary = "Create a new catalog",
    description = "Creates a catalog with the specified definition and species"
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "201",
        description = "Catalog created successfully"
    ),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid catalog definition"
    )
})
@PostMapping
public CreateCatalogResponse create(
    @Parameter(description = "Catalog creation request")
    @RequestBody CreateCatalogRequest request
) {
    // Implementation
}
```

## Performance Considerations

### Connection Pooling

Configure HikariCP appropriately:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
```

### Pagination

Always use pagination for list endpoints:

```java
@GetMapping
public PageResponse<UUID> listCatalogs(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    // Return paginated results
}
```

### Caching

Add caching for frequently accessed data:

```java
@Service
public class CatalogService {

    @Cacheable(value = "catalogs", key = "#id")
    public Catalog getCatalog(UUID id) {
        return dao.loadCatalog(id);
    }

    @CacheEvict(value = "catalogs", key = "#catalog.id")
    public void updateCatalog(Catalog catalog) {
        dao.saveCatalog(catalog);
    }
}
```

## Dependencies

- **cheap-core** - Core data model
- **cheap-json** - JSON serialization
- **cheap-db-postgres** - PostgreSQL persistence
- **cheap-db-sqlite** - SQLite persistence
- **cheap-db-mariadb** - MariaDB persistence
- **Spring Boot Starter Web** - Web framework
- **Spring Boot Starter Validation** - Request validation
- **Spring Boot Starter Actuator** - Monitoring and health checks
- **SpringDoc OpenAPI** - OpenAPI/Swagger documentation

## Build Commands

```bash
# Build module
./gradlew :cheap-rest:build

# Run with PostgreSQL
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=postgres'

# Run with SQLite
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=sqlite'

# Run with MariaDB
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=mariadb'

# Run tests
./gradlew :cheap-rest:test

# Create bootable JAR
./gradlew :cheap-rest:bootJar

# Run JAR
java -jar cheap-rest/build/libs/cheap-rest-0.1.jar --spring.profiles.active=postgres
```

## Related Modules

- `cheap-core` - Core data model (required)
- `cheap-json` - JSON handling (required)
- `cheap-db-postgres` - PostgreSQL backend (optional)
- `cheap-db-sqlite` - SQLite backend (optional)
- `cheap-db-mariadb` - MariaDB backend (optional)
- `cheap-rest-client` - Java client library

## Monitoring

Access Spring Boot Actuator endpoints:

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Info: `GET /actuator/info`
- Prometheus: `GET /actuator/prometheus`

Configure in application.yml:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```
