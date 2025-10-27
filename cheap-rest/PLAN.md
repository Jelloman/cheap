# cheap-rest Implementation Plan

## Overview

cheap-rest is a Spring Boot REST service module that exposes Cheap JSON elements through REST endpoints. The service will be configurable to use any of the three cheap-db modules (PostgreSQL, SQLite, or MariaDB) for catalog storage.

### Key Features

**Write Operations (Priority 1):**
1. **Create Catalog** - POST endpoint to create new catalogs with full CatalogDef
2. **Create AspectDef** - POST endpoint to add AspectDefs to existing catalogs
3. **Upsert Aspects** - POST endpoint to insert/update aspects with automatic entity creation

**Read Operations (Priority 2):**
4. **List Catalogs** - Paginated list of catalog IDs
5. **Get CatalogDef** - Retrieve full catalog definition
6. **List AspectDefs** - Paginated list of AspectDefs in a catalog
7. **Get AspectDef** - Retrieve single AspectDef by name or UUID
8. **Get Hierarchy** - Paginated hierarchy contents (type-specific responses)
9. **Query Aspects** - Multi-dimensional aspect queries by entity IDs and AspectDef filters

**Infrastructure:**
- Spring Boot with configurable database backend (PostgreSQL, SQLite, MariaDB)
- Transaction management for write operations
- Jackson integration with cheap-json serializers/deserializers
- Comprehensive error handling and validation
- Docker support with all three database backends

## Project Structure

```
cheap-rest/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── net/netbeing/cheap/rest/
│   │   │       ├── CheapRestApplication.java           # Spring Boot main class
│   │   │       ├── config/
│   │   │       │   ├── DatabaseConfig.java             # Database configuration
│   │   │       │   ├── CheapDaoConfig.java             # DAO factory configuration
│   │   │       │   └── JacksonConfig.java              # Jackson JSON configuration
│   │   │       ├── service/
│   │   │       │   ├── CatalogService.java             # Business logic for catalogs
│   │   │       │   ├── AspectDefService.java           # Business logic for aspect defs
│   │   │       │   └── HierarchyService.java           # Business logic for hierarchies
│   │   │       ├── controller/
│   │   │       │   ├── CatalogController.java          # REST endpoints for catalogs
│   │   │       │   ├── AspectDefController.java        # REST endpoints for aspect defs
│   │   │       │   ├── HierarchyController.java        # REST endpoints for hierarchies
│   │   │       │   └── AspectController.java           # REST endpoints for aspects
│   │   │       ├── dto/
│   │   │       │   ├── CreateCatalogRequest.java       # Create catalog request
│   │   │       │   ├── CreateCatalogResponse.java      # Create catalog response
│   │   │       │   ├── CreateAspectDefRequest.java     # Create AspectDef request
│   │   │       │   ├── CreateAspectDefResponse.java    # Create AspectDef response
│   │   │       │   ├── UpsertAspectsRequest.java       # Upsert aspects request
│   │   │       │   ├── UpsertAspectsResponse.java      # Upsert aspects response
│   │   │       │   ├── CatalogListResponse.java        # Paginated catalog list
│   │   │       │   ├── AspectDefListResponse.java      # Paginated aspect def list
│   │   │       │   ├── HierarchyContentResponse.java   # Paginated hierarchy content
│   │   │       │   └── AspectQueryRequest.java         # Multi-aspect query request
│   │   │       └── exception/
│   │   │           ├── ResourceNotFoundException.java      # 404 exception
│   │   │           ├── ResourceConflictException.java      # 409 exception
│   │   │           ├── ValidationException.java            # 400 exception
│   │   │           ├── UnprocessableEntityException.java   # 422 exception
│   │   │           └── GlobalExceptionHandler.java         # Exception handling
│   │   └── resources/
│   │       ├── application.yml                         # Main configuration
│   │       ├── application-postgres.yml                # PostgreSQL profile
│   │       ├── application-sqlite.yml                  # SQLite profile
│   │       └── application-mariadb.yml                 # MariaDB profile
│   └── test/
│       ├── java/
│       │   └── net/netbeing/cheap/rest/
│       │       ├── controller/
│       │       │   ├── CatalogControllerTest.java
│       │       │   ├── AspectDefControllerTest.java
│       │       │   ├── HierarchyControllerTest.java
│       │       │   └── AspectControllerTest.java
│       │       └── integration/
│       │           └── CheapRestIntegrationTest.java
│       └── resources/
│           └── application-test.yml                    # Test configuration
└── build.gradle.kts
```

## Phase 1: Module Setup and Configuration System

### 1.1 Build Configuration (build.gradle.kts)

**Dependencies:**
- Spring Boot Starter Web
- Spring Boot Starter JDBC (for database connection management and transaction support)
- cheap-core (compile dependency)
- cheap-json (compile dependency)
- cheap-db-postgres (runtime dependency)
- cheap-db-sqlite (runtime dependency)
- cheap-db-mariadb (runtime dependency)
- Lombok
- Spring Boot Starter Validation (for request validation)
- JUnit Jupiter (test)
- Spring Boot Test (test)
- Embedded PostgreSQL (test - for integration tests)

**Key Points:**
- Use `runtimeOnly` for the three cheap-db modules to allow runtime selection
- Use `api` for cheap-core and cheap-json
- Configure Java 24 toolchain
- Enable Spring Boot plugin

### 1.2 Database Configuration

**Strategy: Spring Profiles**

Use Spring profiles to select database backend at runtime:
- `postgres` - Use PostgreSQL
- `sqlite` - Use SQLite
- `mariadb` - Use MariaDB

**Configuration Files:**

`application.yml` (base configuration):
```yaml
server:
  port: 8080

cheap:
  pagination:
    default-page-size: 20
    max-page-size: 100
```

`application-postgres.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cheap
    username: cheap_user
    password: ${DB_PASSWORD:changeme}
    driver-class-name: org.postgresql.Driver

cheap:
  database:
    type: postgres
```

`application-sqlite.yml`:
```yaml
spring:
  datasource:
    url: jdbc:sqlite:${CHEAP_DB_PATH:./cheap.db}
    driver-class-name: org.sqlite.JDBC

cheap:
  database:
    type: sqlite
```

`application-mariadb.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/cheap
    username: cheap_user
    password: ${DB_PASSWORD:changeme}
    driver-class-name: org.mariadb.jdbc.Driver

cheap:
  database:
    type: mariadb
```

### 1.3 DAO Factory Configuration

**CheapDaoConfig.java:**

Create a Spring Configuration class that:
1. Reads the `cheap.database.type` property
2. Creates the appropriate JDBC Connection from Spring DataSource
3. Instantiates the correct DAO implementation (PostgresDao, SqliteDao, or MariaDbDao)
4. Creates and configures a CheapFactory instance
5. Exposes both as Spring beans

**Key Implementation Details:**
- Use `@ConditionalOnProperty` to create different beans based on database type
- Leverage Spring's DataSource abstraction
- Handle DAO initialization and schema setup
- Create separate configuration classes for each database type if needed

**Example Structure:**
```java
@Configuration
public class CheapDaoConfig {

    @Bean
    public CheapFactory cheapFactory() {
        return new CheapFactory();
    }

    @Configuration
    @ConditionalOnProperty(name = "cheap.database.type", havingValue = "postgres")
    static class PostgresConfig {
        @Bean
        public CheapDao cheapDao(DataSource dataSource, CheapFactory factory) {
            // Create PostgresAdapter and PostgresDao
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "cheap.database.type", havingValue = "sqlite")
    static class SqliteConfig {
        @Bean
        public CheapDao cheapDao(DataSource dataSource, CheapFactory factory) {
            // Create SqliteAdapter and SqliteDao
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "cheap.database.type", havingValue = "mariadb")
    static class MariaDbConfig {
        @Bean
        public CheapDao cheapDao(DataSource dataSource, CheapFactory factory) {
            // Create MariaDbAdapter and MariaDbDao
        }
    }
}
```

### 1.4 Jackson Configuration

**JacksonConfig.java:**

Configure Jackson ObjectMapper to use cheap-json serializers:
1. Register Cheap serializers from cheap-json module
2. Configure date/time formatting
3. Configure null handling
4. Configure pretty printing for development

## Phase 2: Service Layer

Create service classes that encapsulate business logic and DAO interactions:

### 2.1 CatalogService

**Responsibilities:**
- Create new catalogs from CatalogDef
- List all catalog IDs (with pagination)
- Retrieve catalog by ID
- Retrieve catalog definition by ID
- Handle catalog not found scenarios
- Validate catalog data before persistence

**Key Methods:**
```java
public class CatalogService {
    UUID createCatalog(CatalogDef catalogDef, CatalogSpecies species, UUID upstream);
    Page<UUID> listCatalogIds(Pageable pageable);
    Catalog getCatalog(UUID catalogId);
    CatalogDef getCatalogDef(UUID catalogId);
    void validateCatalogDef(CatalogDef catalogDef);
}
```

### 2.2 AspectDefService

**Responsibilities:**
- Create new AspectDef in a catalog
- List all AspectDefs in a catalog (with pagination)
- Retrieve AspectDef by catalog ID and name
- Retrieve AspectDef by catalog ID and UUID
- Handle AspectDef not found scenarios
- Validate AspectDef data before persistence

**Key Methods:**
```java
public class AspectDefService {
    AspectDef createAspectDef(UUID catalogId, AspectDef aspectDef);
    Page<AspectDef> listAspectDefs(UUID catalogId, Pageable pageable);
    AspectDef getAspectDefByName(UUID catalogId, String name);
    AspectDef getAspectDefById(UUID catalogId, UUID aspectDefId);
    void validateAspectDef(AspectDef aspectDef);
}
```

### 2.3 HierarchyService

**Responsibilities:**
- Retrieve hierarchy by catalog ID and name
- Paginate hierarchy contents based on hierarchy type
- Handle different hierarchy types appropriately

**Key Methods:**
```java
public class HierarchyService {
    Hierarchy getHierarchy(UUID catalogId, String hierarchyName);
    Page<?> getHierarchyContents(UUID catalogId, String hierarchyName, Pageable pageable);
}
```

### 2.4 AspectService

**Responsibilities:**
- Insert or update (upsert) aspects in a catalog
- Query aspects by multiple entity IDs and AspectDef filters
- Return matching aspects organized by entity and AspectDef
- Handle complex multi-dimensional queries
- Validate aspect data against AspectDef
- Create entities as needed during aspect upsert

**Key Methods:**
```java
public class AspectService {
    Map<UUID, Boolean> upsertAspects(
        UUID catalogId,
        String aspectDefName,
        Map<UUID, Map<String, Object>> aspectsByEntity
    );

    Map<UUID, Map<String, Aspect>> queryAspects(
        UUID catalogId,
        Set<UUID> entityIds,
        Set<String> aspectDefNames
    );

    void validateAspectData(AspectDef aspectDef, Map<String, Object> properties);
}
```

## Phase 3: REST API Design

### Write Operations (Implement First)

These endpoints should be implemented first as they enable creating test data for the read endpoints.

### 3.1 Endpoint 1: Create Catalog

**URL:** `POST /api/catalog`

**Request Body:**
```json
{
  "catalogDef": {
    "hierarchyDefs": [
      {
        "name": "people",
        "type": "ENTITY_SET"
      }
    ],
    "aspectDefs": [
      {
        "name": "com.example.PersonAspect",
        "id": "650e8400-e29b-41d4-a716-446655440000",
        "properties": {
          "name": {
            "name": "name",
            "type": "STRING",
            "isReadable": true,
            "isWritable": true,
            "isNullable": false,
            "isRemovable": false,
            "isMultivalued": false
          },
          "age": {
            "name": "age",
            "type": "INTEGER",
            "isReadable": true,
            "isWritable": true,
            "isNullable": true,
            "isRemovable": false,
            "isMultivalued": false
          }
        }
      }
    ]
  },
  "species": "SINK",
  "upstream": null,
  "uri": "http://example.com/catalogs/my-catalog"
}
```

**Request Body Fields:**
- `catalogDef` (required) - Full CatalogDef as defined in cheap-json schema
- `species` (required) - One of: SOURCE, SINK, MIRROR, CACHE, CLONE, FORK
- `upstream` (optional) - UUID of upstream catalog (required for MIRROR, CACHE, CLONE; must be null for SOURCE, SINK)
- `uri` (optional) - URI for the catalog

**Response:**
```json
{
  "catalogId": "550e8400-e29b-41d4-a716-446655440000",
  "uri": "http://example.com/catalogs/my-catalog",
  "message": "Catalog created successfully"
}
```

**Status Codes:**
- 201 Created - Catalog successfully created
- 400 Bad Request - Invalid catalog definition, species, or upstream configuration
- 404 Not Found - Upstream catalog not found (when upstream is specified)
- 409 Conflict - Catalog with same ID already exists
- 500 Internal Server Error - Database error

**Validation Rules:**
1. CatalogDef must have valid hierarchyDefs and aspectDefs
2. Species must be a valid CatalogSpecies enum value
3. If species is MIRROR, CACHE, or CLONE, upstream must be provided and must exist
4. If species is SOURCE or SINK, upstream must be null
5. All AspectDef names must be unique within the catalog
6. All HierarchyDef names must be unique within the catalog
7. AspectMap hierarchies are auto-created from AspectDefs (don't include in hierarchyDefs)

**Implementation Notes:**
- Use CheapFactory to create catalog instance
- Use DAO.saveCatalog() to persist
- Transaction should roll back if any part fails
- Auto-create AspectMap hierarchies for each AspectDef

### 3.2 Endpoint 2: Create AspectDef in Catalog

**URL:** `POST /api/catalog/{catalogId}/aspect-defs`

**Path Parameters:**
- `catalogId` (UUID) - The catalog's global ID

**Request Body:**
```json
{
  "name": "com.example.AddressAspect",
  "id": "750e8400-e29b-41d4-a716-446655440000",
  "uri": "http://example.com/aspects/address",
  "version": 1,
  "properties": {
    "street": {
      "name": "street",
      "type": "STRING",
      "isReadable": true,
      "isWritable": true,
      "isNullable": false,
      "isRemovable": false,
      "isMultivalued": false
    },
    "city": {
      "name": "city",
      "type": "STRING",
      "isReadable": true,
      "isWritable": true,
      "isNullable": false,
      "isRemovable": false,
      "isMultivalued": false
    },
    "zipCode": {
      "name": "zipCode",
      "type": "STRING",
      "isReadable": true,
      "isWritable": true,
      "isNullable": true,
      "isRemovable": false,
      "isMultivalued": false
    }
  }
}
```

**Request Body Fields:**
- Full AspectDef JSON as defined in cheap-json schema
- `name` (required) - Unique name for the AspectDef (reverse domain notation recommended)
- `id` (optional) - UUID for the AspectDef (will be generated if not provided)
- `uri` (optional) - URI for the AspectDef
- `version` (optional) - Version number
- `properties` (required) - Map of PropertyDef objects

**Response:**
```json
{
  "aspectDefId": "750e8400-e29b-41d4-a716-446655440000",
  "aspectDefName": "com.example.AddressAspect",
  "message": "AspectDef created successfully"
}
```

**Status Codes:**
- 201 Created - AspectDef successfully created
- 400 Bad Request - Invalid AspectDef structure or property definitions
- 404 Not Found - Catalog not found
- 409 Conflict - AspectDef with same name or ID already exists in catalog
- 500 Internal Server Error - Database error

**Validation Rules:**
1. AspectDef name must be unique within the catalog
2. AspectDef must have at least one property
3. All PropertyDef names must be unique within the AspectDef
4. Property types must be valid PropertyType enum values
5. If ID is provided, it must be a valid UUID and must not already exist

**Implementation Notes:**
- Load the catalog first to verify it exists
- Use CheapFactory to create AspectDef instance
- Auto-create corresponding AspectMap hierarchy in the catalog
- Update catalog's aspectage (AspectDef directory)
- Save both AspectDef and new hierarchy via DAO
- Transaction should roll back if any part fails

### 3.3 Endpoint 3: Upsert Aspects

**URL:** `POST /api/catalog/{catalogId}/aspects/{aspectDefName}`

**Path Parameters:**
- `catalogId` (UUID) - The catalog's global ID
- `aspectDefName` (String) - The AspectDef name for the aspects being upserted

**Request Body:**
```json
{
  "aspects": [
    {
      "entityId": "850e8400-e29b-41d4-a716-446655440000",
      "properties": {
        "name": "John Doe",
        "age": 30
      }
    },
    {
      "entityId": "850e8400-e29b-41d4-a716-446655440001",
      "properties": {
        "name": "Jane Smith",
        "age": 28
      }
    }
  ],
  "createEntities": true
}
```

**Request Body Fields:**
- `aspects` (required) - Array of aspect data objects
  - `entityId` (required) - UUID of the entity
  - `properties` (required) - Map of property name to value
- `createEntities` (optional, default: true) - Whether to auto-create entities that don't exist

**Response:**
```json
{
  "catalogId": "550e8400-e29b-41d4-a716-446655440000",
  "aspectDefName": "com.example.PersonAspect",
  "results": [
    {
      "entityId": "850e8400-e29b-41d4-a716-446655440000",
      "success": true,
      "created": true,
      "message": "Aspect created"
    },
    {
      "entityId": "850e8400-e29b-41d4-a716-446655440001",
      "success": true,
      "created": false,
      "message": "Aspect updated"
    }
  ],
  "successCount": 2,
  "failureCount": 0
}
```

**Status Codes:**
- 200 OK - All aspects processed (check individual results for success/failure)
- 207 Multi-Status - Some aspects succeeded, some failed (check individual results)
- 400 Bad Request - Invalid request structure or property validation failed for all aspects
- 404 Not Found - Catalog or AspectDef not found
- 422 Unprocessable Entity - Entities don't exist and createEntities is false
- 500 Internal Server Error - Database error

**Validation Rules:**
1. Catalog must exist
2. AspectDef must exist in the catalog
3. Property names must match those defined in the AspectDef
4. Property values must be compatible with their PropertyDef types
5. Required (non-nullable) properties must be provided
6. If createEntities is false, all entities must already exist
7. Maximum of 1000 aspects per request (configurable)

**Implementation Notes:**
- Load catalog and AspectDef first to verify they exist
- For each aspect:
  - Validate property data against AspectDef
  - Check if entity exists; create if needed and createEntities is true
  - Check if aspect already exists for this entity
  - Use CheapFactory to create Aspect instance
  - Insert or update via DAO
- Process in a single transaction for consistency
- Continue processing on individual failures, track which succeeded/failed
- Return detailed results for each aspect

**Behavior Details:**
- **Insert vs Update:** If an aspect of this type already exists for the entity, update it; otherwise insert
- **Partial Updates:** All properties in the request replace existing values; properties not in request retain existing values
- **Entity Creation:** If entity doesn't exist and createEntities is true, create it and add to catalog
- **Transaction Handling:** Use single transaction for all aspects; roll back all on database error
- **Error Handling:** Validation errors for individual aspects don't fail the whole request (return in results)

### Read Operations

### 3.4 Endpoint 4: List Catalog IDs

**URL:** `GET /api/catalog`

**Query Parameters:**
- `page` (optional, default: 0) - Page number (zero-indexed)
- `size` (optional, default: 20) - Page size

**Response:**
```json
{
  "content": [
    "550e8400-e29b-41d4-a716-446655440000",
    "550e8400-e29b-41d4-a716-446655440001"
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

### 3.5 Endpoint 5: Get Catalog Definition

**URL:** `GET /api/catalog/{catalogId}`

**Path Parameters:**
- `catalogId` (UUID) - The catalog's global ID

**Response:**
- Full CatalogDef JSON as defined in cheap-json schema
- Status: 200 OK
- Status: 404 Not Found if catalog doesn't exist

### 3.6 Endpoint 6: List AspectDefs in Catalog

**URL:** `GET /api/catalog/{catalogId}/aspect-defs`

**Path Parameters:**
- `catalogId` (UUID) - The catalog's global ID

**Query Parameters:**
- `page` (optional, default: 0) - Page number (zero-indexed)
- `size` (optional, default: 20) - Page size

**Response:**
```json
{
  "catalogId": "550e8400-e29b-41d4-a716-446655440000",
  "content": [
    {
      "name": "com.example.PersonAspect",
      "id": "650e8400-e29b-41d4-a716-446655440000",
      "properties": { ... }
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 15,
  "totalPages": 1
}
```

### 3.7 Endpoint 7: Get Single AspectDef

**URL:** `GET /api/catalog/{catalogId}/aspect-defs/{aspectDefId}`

**Path Parameters:**
- `catalogId` (UUID) - The catalog's global ID
- `aspectDefId` (String) - Either the AspectDef name or UUID

**Response:**
- Full AspectDef JSON as defined in cheap-json schema
- Status: 200 OK
- Status: 404 Not Found if AspectDef doesn't exist

**Implementation Note:**
- Try to parse `aspectDefId` as UUID first
- If parsing fails, treat it as a name
- This allows both `/aspect-defs/com.example.PersonAspect` and `/aspect-defs/650e8400-...` to work

### 3.8 Endpoint 8: Get Hierarchy Contents

**URL:** `GET /api/catalog/{catalogId}/hierarchies/{hierarchyName}`

**Path Parameters:**
- `catalogId` (UUID) - The catalog's global ID
- `hierarchyName` (String) - The hierarchy name

**Query Parameters:**
- `page` (optional, default: 0) - Page number (zero-indexed)
- `size` (optional, default: 20) - Page size

**Response:**

The response format varies by hierarchy type:

**ENTITY_LIST:**
```json
{
  "hierarchyName": "myList",
  "hierarchyType": "ENTITY_LIST",
  "content": [
    "entity-uuid-1",
    "entity-uuid-2"
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

**ENTITY_SET:**
```json
{
  "hierarchyName": "mySet",
  "hierarchyType": "ENTITY_SET",
  "content": [
    "entity-uuid-1",
    "entity-uuid-2"
  ],
  "page": 0,
  "size": 20,
  "totalElements": 50,
  "totalPages": 3
}
```

**ENTITY_DIR:**
```json
{
  "hierarchyName": "myDirectory",
  "hierarchyType": "ENTITY_DIR",
  "content": [
    {"key": "person1", "entityId": "entity-uuid-1"},
    {"key": "person2", "entityId": "entity-uuid-2"}
  ],
  "page": 0,
  "size": 20,
  "totalElements": 75,
  "totalPages": 4
}
```

**ENTITY_TREE:**
```json
{
  "hierarchyName": "myTree",
  "hierarchyType": "ENTITY_TREE",
  "rootNode": {
    "value": null,
    "children": {
      "branch1": { ... },
      "branch2": { ... }
    }
  }
}
```
Note: Trees are not paginated - full tree structure returned

**ASPECT_MAP:**
```json
{
  "hierarchyName": "com.example.PersonAspect",
  "hierarchyType": "ASPECT_MAP",
  "aspectDefName": "com.example.PersonAspect",
  "content": [
    {
      "entityId": "entity-uuid-1",
      "aspect": {
        "name": "John",
        "age": 30
      }
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 200,
  "totalPages": 10
}
```

### 3.9 Endpoint 9: Query Aspects

**URL:** `POST /api/catalog/{catalogId}/aspects/query`

**Path Parameters:**
- `catalogId` (UUID) - The catalog's global ID

**Request Body:**
```json
{
  "entityIds": [
    "550e8400-e29b-41d4-a716-446655440000",
    "550e8400-e29b-41d4-a716-446655440001"
  ],
  "aspectDefNames": [
    "com.example.PersonAspect",
    "com.example.AddressAspect"
  ]
}
```

**Response:**
```json
{
  "catalogId": "550e8400-e29b-41d4-a716-446655440000",
  "results": {
    "550e8400-e29b-41d4-a716-446655440000": {
      "com.example.PersonAspect": {
        "name": "John",
        "age": 30
      },
      "com.example.AddressAspect": {
        "street": "123 Main St",
        "city": "Springfield"
      }
    },
    "550e8400-e29b-41d4-a716-446655440001": {
      "com.example.PersonAspect": {
        "name": "Jane",
        "age": 28
      }
    }
  }
}
```

**Behavior:**
- If `aspectDefNames` is empty or null, return all aspects for the given entities
- If `entityIds` is empty, return 400 Bad Request
- If an entity doesn't have a requested aspect, omit it from the response
- Maximum of 100 entity IDs per request (configurable)

## Phase 4: Error Handling and Validation

### 4.1 Exception Handling

Create `GlobalExceptionHandler` with `@ControllerAdvice` to handle:
- `ResourceNotFoundException` → 404 Not Found
- `ResourceConflictException` → 409 Conflict
- `ValidationException` → 400 Bad Request
- `UnprocessableEntityException` → 422 Unprocessable Entity
- `IllegalArgumentException` → 400 Bad Request
- `MethodArgumentNotValidException` → 400 Bad Request (Spring validation)
- `SQLException` → 500 Internal Server Error
- `DataAccessException` → 500 Internal Server Error
- General exceptions → 500 Internal Server Error

**Error Response Format:**
```json
{
  "timestamp": "2025-10-21T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Catalog with ID 550e8400-e29b-41d4-a716-446655440000 not found",
  "path": "/api/catalog/550e8400-e29b-41d4-a716-446655440000"
}
```

**Validation Error Response Format:**
```json
{
  "timestamp": "2025-10-21T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/catalog",
  "errors": [
    {
      "field": "catalogDef.aspectDefs[0].name",
      "message": "AspectDef name cannot be null"
    },
    {
      "field": "species",
      "message": "Must be one of: SOURCE, SINK, MIRROR, CACHE, CLONE, FORK"
    }
  ]
}
```

### 4.2 Input Validation

**Path and Query Parameters:**
- Validate UUID format in path parameters
- Validate pagination parameters (page >= 0, size > 0 and <= max)
- Validate entity ID limits in aspect query endpoint
- Return meaningful validation error messages

**Request Body Validation:**

**Create Catalog:**
- CatalogDef structure is valid and complete
- Species is a valid enum value
- Upstream validation based on species type
- AspectDef names are unique
- HierarchyDef names are unique
- PropertyDef validation within AspectDefs

**Create AspectDef:**
- AspectDef name is non-null and non-empty
- At least one property is defined
- Property names are unique
- Property types are valid
- UUID format if ID is provided

**Upsert Aspects:**
- Aspects array is non-empty
- Entity IDs are valid UUIDs
- Properties object is non-null
- Maximum batch size not exceeded (1000 default)
- Property values match PropertyDef types
- Required properties are present

**Validation Strategy:**
- Use Spring Validation annotations (`@Valid`, `@NotNull`, etc.) where applicable
- Custom validators for Cheap-specific rules
- Service layer validation for business rules
- Return all validation errors in a single response (not fail-fast)

## Phase 5: Testing Strategy

### 5.1 Unit Tests

Test each component in isolation:
- Controller tests with MockMvc
- Service tests with mocked DAOs
- Configuration tests for each database type

### 5.2 Integration Tests

- Use embedded PostgreSQL for integration tests
- Test end-to-end request/response cycles
- Test pagination logic
- Test error scenarios
- Test JSON serialization/deserialization

### 5.3 Test Data

Create test fixtures:
- Sample catalogs with various hierarchy types
- Sample AspectDefs with different property configurations
- Sample entities with multiple aspects

## Phase 6: Documentation and Deployment

### 6.1 API Documentation

Options:
1. SpringDoc OpenAPI (Swagger) integration
2. Manual API documentation in README
3. Postman collection for testing

### 6.2 Docker Support

Create Dockerfile:
- Multi-stage build (Gradle build + runtime)
- Support for all three database backends
- Environment variable configuration
- Health check endpoint

### 6.3 Docker Compose

Provide docker-compose.yml with:
- cheap-rest service
- PostgreSQL service (default)
- Volume mounts for SQLite option
- Environment variable examples

## Implementation Order

### Priority 1: Write Operations (Implement First)

1. **Phase 1:** Set up module structure, build configuration, and database configuration system
   - Create build.gradle.kts with all dependencies
   - Configure Spring profiles for three databases
   - Create CheapDaoConfig with conditional DAO creation
   - Configure Jackson with cheap-json serializers
   - Enable Spring transaction management

2. **Phase 2:** Implement write operations service layer
   - CatalogService.createCatalog() with transaction support
   - AspectDefService.createAspectDef() with transaction support
   - AspectService.upsertAspects() with transaction support
   - Validation logic for all write operations

3. **Phase 3:** Implement write operations controllers
   - POST /api/catalog (Endpoint 1)
   - POST /api/catalog/{catalogId}/aspect-defs (Endpoint 2)
   - POST /api/catalog/{catalogId}/aspects/{aspectDefName} (Endpoint 3)
   - Error handling and validation
   - Integration tests for write operations

### Priority 2: Read Operations

4. **Phase 4:** Implement read operations service layer
   - CatalogService read methods
   - AspectDefService read methods
   - HierarchyService read methods
   - AspectService query methods

5. **Phase 5:** Implement read operations controllers
   - GET /api/catalog (Endpoint 4)
   - GET /api/catalog/{catalogId} (Endpoint 5)
   - GET /api/catalog/{catalogId}/aspect-defs (Endpoint 6)
   - GET /api/catalog/{catalogId}/aspect-defs/{aspectDefId} (Endpoint 7)
   - GET /api/catalog/{catalogId}/hierarchies/{hierarchyName} (Endpoint 8)
   - POST /api/catalog/{catalogId}/aspects/query (Endpoint 9)
   - Integration tests for read operations

### Priority 3: Polish and Deployment

6. **Phase 6:** Comprehensive testing
   - Unit tests for all services
   - Controller tests with MockMvc
   - Integration tests with all three database backends
   - Performance testing for pagination and batch operations
   - Test data fixtures

7. **Phase 7:** Documentation and deployment
   - API documentation (OpenAPI/Swagger)
   - README with usage examples
   - Docker support
   - Docker Compose configurations
   - Deployment guides

## Key Design Decisions

### Database Selection Strategy

**Choice: Spring Profiles**

Pros:
- Native Spring Boot mechanism
- Clear separation of configurations
- Easy to extend with additional databases
- No custom plugin system needed
- Well-documented and widely understood

Cons:
- Must include all DB drivers in classpath
- Cannot completely exclude unused drivers at build time

**Alternative Considered: Custom Plugin System**

This would involve a plugin interface and dynamic classloading, but adds unnecessary complexity for this use case.

### JSON Serialization Strategy

**Choice: Leverage existing cheap-json module**

The cheap-json module already has Jackson serializers and deserializers for all Cheap types. We'll configure Spring's ObjectMapper to use these.

**Implementation:**
- Create `@Configuration` class for Jackson
- Register cheap-json serializers/deserializers
- Configure module with Spring's ObjectMapper

### Pagination Strategy

**Choice: Spring Data domain classes**

Use Spring's `Pageable` and `Page` abstractions even though we're not using Spring Data JPA:
- Provides consistent pagination API
- Works with query parameters out of the box
- Can be manually constructed from DAO results

### Transaction Management

**Strategy: Spring Declarative Transactions**

Use Spring's `@Transactional` annotation for write operations:

**Write Operations (Require Transactions):**
- Create Catalog - Single transaction for entire catalog creation including hierarchies and AspectDefs
- Create AspectDef - Single transaction for AspectDef and AspectMap hierarchy creation
- Upsert Aspects - Single transaction for all aspect inserts/updates in a batch

**Read Operations:**
- Use read-only transactions (`@Transactional(readOnly = true)`) for consistency
- Can use connection from pool without explicit transaction in some cases

**Implementation Details:**
```java
@Service
public class CatalogService {

    @Transactional
    public UUID createCatalog(CatalogDef catalogDef, CatalogSpecies species, UUID upstream) {
        // All operations in this method execute in single transaction
        // Automatic rollback on any exception
    }

    @Transactional(readOnly = true)
    public CatalogDef getCatalogDef(UUID catalogId) {
        // Read-only transaction
    }
}
```

**Transaction Boundaries:**
- Keep transactions at service layer, not controller layer
- One transaction per REST request for write operations
- Short-lived transactions to avoid lock contention
- Rollback on any exception (RuntimeException by default)

**Isolation Level:**
- Use default isolation level (READ_COMMITTED for most databases)
- Can configure per method if needed for specific operations

**Future Enhancements:**
- Optimistic locking for concurrent updates
- Retry logic for transient failures
- Transaction event listeners for audit logging

## Security Considerations (Future)

Not included in initial implementation, but should be considered:
- Authentication (OAuth2, JWT)
- Authorization (role-based access to catalogs)
- Rate limiting
- CORS configuration
- HTTPS enforcement

## Performance Considerations

### Caching Strategy

Consider adding Spring Cache abstraction:
- Cache catalog definitions (rarely change)
- Cache AspectDef lists per catalog
- Invalidation strategy for mutations

### Connection Pooling

Use HikariCP (Spring Boot default) for connection pooling:
- Configure appropriate pool sizes
- Monitor connection usage
- Handle connection timeouts

### Pagination Defaults

- Default page size: 20
- Max page size: 100
- Prevent unbounded result sets

## Monitoring and Observability

Include Spring Boot Actuator:
- Health check endpoint
- Metrics endpoint
- Database connection health
- Custom metrics for query performance

## Open Questions

1. Should we support filtering/sorting in list endpoints?
2. Should we add a search endpoint for entities by property values?
3. Should we support partial AspectDef responses (e.g., only property names)?
4. Should we add bulk operations endpoints?
5. What's the versioning strategy for the REST API (URL-based, header-based)?

## Success Criteria

The cheap-rest module will be considered complete when:

### Write Operations (Priority 1)
1. ✅ Create Catalog endpoint (POST /api/catalog) is implemented and functional
2. ✅ Create AspectDef endpoint (POST /api/catalog/{catalogId}/aspect-defs) is implemented and functional
3. ✅ Upsert Aspects endpoint (POST /api/catalog/{catalogId}/aspects/{aspectDefName}) is implemented and functional
4. ✅ All write operations use proper transaction management with rollback on failure
5. ✅ Request validation works correctly with meaningful error messages
6. ✅ Write operations create proper database records that can be queried

### Read Operations (Priority 2)
7. ✅ All six read endpoints are implemented and functional
8. ✅ Pagination works correctly for all applicable endpoints
9. ✅ JSON responses match cheap-json schema specifications
10. ✅ Query aspects endpoint handles multi-dimensional queries correctly

### Infrastructure
11. ✅ Database backend can be selected via Spring profiles
12. ✅ All three database backends (PostgreSQL, SQLite, MariaDB) work correctly
13. ✅ Jackson integration with cheap-json serializers works correctly
14. ✅ Connection pooling is configured properly

### Testing
15. ✅ Unit tests for all services achieve >80% code coverage
16. ✅ Integration tests for all endpoints with embedded database
17. ✅ All three database backends tested in integration tests
18. ✅ Error scenarios are tested (404, 400, 409, 500)

### Documentation and Deployment
19. ✅ API documentation is complete and accurate (OpenAPI/Swagger)
20. ✅ README with usage examples and configuration guide
21. ✅ Application can be run via Docker with all database options
22. ✅ Docker Compose example configurations provided
