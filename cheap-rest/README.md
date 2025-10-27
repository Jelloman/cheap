# cheap-rest

A Spring Boot REST API for the Cheap data model, providing HTTP endpoints for creating and querying catalogs, AspectDefs, hierarchies, and aspects.

## Overview

cheap-rest exposes the Cheap data caching system through RESTful endpoints. It supports three database backends (PostgreSQL, SQLite, MariaDB) selectable via Spring profiles at runtime.

## Features

- **Write Operations:**
  - Create catalogs with full CatalogDef
  - Add AspectDefs to existing catalogs
  - Upsert aspects with automatic entity creation

- **Read Operations:**
  - List catalogs with pagination
  - Retrieve catalog definitions
  - List and retrieve AspectDefs
  - Query hierarchy contents (all 5 types)
  - Multi-dimensional aspect queries

- **Infrastructure:**
  - Configurable database backend (PostgreSQL, SQLite, MariaDB)
  - Transaction management for data integrity
  - Comprehensive error handling and validation
  - Jackson JSON integration with cheap-json serializers
  - Spring Boot Actuator for monitoring and health checks
  - OpenAPI/Swagger documentation for interactive API exploration

## Quick Start

### Prerequisites

- Java 24
- One of: PostgreSQL, SQLite, or MariaDB
- Gradle 8.5+

### Running with PostgreSQL

```bash
# Set up PostgreSQL database
createdb cheap
createuser cheap_user

# Run the application
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=postgres'
```

### Running with SQLite

```bash
# SQLite requires no setup
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=sqlite'
```

### Running with MariaDB

```bash
# Set up MariaDB database
mysql -u root -e "CREATE DATABASE cheap;"
mysql -u root -e "CREATE USER 'cheap_user'@'localhost';"

# Run the application
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=mariadb'
```

## API Endpoints

### Catalog Operations

#### Create Catalog
```
POST /api/catalog
```

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

**Response:** `201 Created`
```json
{
  "catalogId": "550e8400-e29b-41d4-a716-446655440000",
  "uri": "http://example.com/catalogs/my-catalog",
  "message": "Catalog created successfully"
}
```

#### List Catalogs
```
GET /api/catalog?page=0&size=20
```

**Response:** `200 OK`
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

#### Get Catalog Definition
```
GET /api/catalog/{catalogId}
```

**Response:** `200 OK` - Returns full CatalogDef JSON

### AspectDef Operations

#### Create AspectDef
```
POST /api/catalog/{catalogId}/aspect-defs
```

**Request Body:**
```json
{
  "name": "com.example.AddressAspect",
  "id": "750e8400-e29b-41d4-a716-446655440000",
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
    }
  }
}
```

**Response:** `201 Created`
```json
{
  "aspectDefId": "750e8400-e29b-41d4-a716-446655440000",
  "aspectDefName": "com.example.AddressAspect",
  "message": "AspectDef created successfully"
}
```

#### List AspectDefs
```
GET /api/catalog/{catalogId}/aspect-defs?page=0&size=20
```

#### Get AspectDef
```
GET /api/catalog/{catalogId}/aspect-defs/{aspectDefIdOrName}
```

Supports both UUID and name lookup.

### Aspect Operations

#### Upsert Aspects
```
POST /api/catalog/{catalogId}/aspects/{aspectDefName}
```

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

**Response:** `200 OK` or `207 Multi-Status`
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
    }
  ],
  "successCount": 2,
  "failureCount": 0
}
```

#### Query Aspects
```
POST /api/catalog/{catalogId}/aspects/query
```

**Request Body:**
```json
{
  "entityIds": [
    "850e8400-e29b-41d4-a716-446655440000",
    "850e8400-e29b-41d4-a716-446655440001"
  ],
  "aspectDefNames": [
    "com.example.PersonAspect",
    "com.example.AddressAspect"
  ]
}
```

**Response:** `200 OK`
```json
{
  "catalogId": "550e8400-e29b-41d4-a716-446655440000",
  "results": {
    "850e8400-e29b-41d4-a716-446655440000": {
      "com.example.PersonAspect": {
        "name": "John Doe",
        "age": 30
      }
    }
  }
}
```

### Hierarchy Operations

#### Get Hierarchy Contents
```
GET /api/catalog/{catalogId}/hierarchies/{hierarchyName}?page=0&size=20
```

Response format varies by hierarchy type (ENTITY_LIST, ENTITY_SET, ENTITY_DIR, ENTITY_TREE, ASPECT_MAP).

## Configuration

### Database Configuration

Configuration is managed through Spring profiles. Create an `application-{profile}.yml` file or set environment variables.

#### PostgreSQL Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cheap
    username: cheap_user
    password: ${DB_PASSWORD}

cheap:
  database:
    type: postgres
```

#### SQLite Configuration
```yaml
spring:
  datasource:
    url: jdbc:sqlite:${CHEAP_DB_PATH:./cheap.db}

cheap:
  database:
    type: sqlite
```

#### MariaDB Configuration
```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/cheap
    username: cheap_user
    password: ${DB_PASSWORD}

cheap:
  database:
    type: mariadb
```

### Application Settings

```yaml
cheap:
  pagination:
    default-page-size: 20
    max-page-size: 100
  aspect-upsert:
    max-batch-size: 1000
```

## Monitoring and Health Checks

### Actuator Endpoints

Spring Boot Actuator provides monitoring and management endpoints:

- **Health Check:** `GET /actuator/health` - Service health status
- **Info:** `GET /actuator/info` - Application information
- **Metrics:** `GET /actuator/metrics` - Application metrics
- **Prometheus:** `GET /actuator/prometheus` - Metrics in Prometheus format

Example health check response:
```json
{
  "status": "UP"
}
```

### Actuator Configuration

Configure Actuator in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

## API Documentation

### Interactive Swagger UI

Access interactive API documentation at:

```
http://localhost:8080/swagger-ui.html
```

The Swagger UI provides:
- Complete API endpoint documentation
- Request/response schemas
- Try-it-out functionality for testing endpoints
- Model definitions for all DTOs

### OpenAPI Specification

The raw OpenAPI 3.0 specification is available at:

```
http://localhost:8080/api-docs
```

This can be imported into API clients like Postman or Insomnia.

## Error Handling

The API returns standard HTTP status codes with detailed error messages:

- `200 OK` - Successful request
- `201 Created` - Resource successfully created
- `207 Multi-Status` - Partial success (some aspects succeeded, some failed)
- `400 Bad Request` - Invalid request or validation error
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists
- `422 Unprocessable Entity` - Semantic error (e.g., entities don't exist)
- `500 Internal Server Error` - Database or server error

**Error Response Format:**
```json
{
  "timestamp": "2025-10-22T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Catalog with ID 550e8400-e29b-41d4-a716-446655440000 not found",
  "path": "/api/catalog/550e8400-e29b-41d4-a716-446655440000"
}
```

## Building

```bash
# Build all modules
./gradlew build

# Build just cheap-rest
./gradlew :cheap-rest:build

# Run tests
./gradlew :cheap-rest:test

# Create bootable JAR
./gradlew :cheap-rest:bootJar
```

The bootable JAR will be created at `cheap-rest/build/libs/cheap-rest-0.1.jar`.

## Running the JAR

```bash
# PostgreSQL
java -jar cheap-rest-0.1.jar --spring.profiles.active=postgres

# SQLite
java -jar cheap-rest-0.1.jar --spring.profiles.active=sqlite

# MariaDB
java -jar cheap-rest-0.1.jar --spring.profiles.active=mariadb
```

## Docker

See the `Dockerfile` and `docker-compose.yml` files for containerized deployment options.

## Architecture

- **Controllers** - REST endpoints, request/response handling
- **Services** - Business logic, validation, transaction management
- **DAOs** - Database access via cheap-db modules
- **DTOs** - Request/response data transfer objects
- **Exception Handlers** - Centralized error handling

## Development

### Adding a New Endpoint

1. Create request/response DTOs in `dto/` package
2. Add service method in appropriate service class
3. Add controller method with `@GetMapping` or `@PostMapping`
4. Add validation logic
5. Update this README

### Database Schema

The Cheap database schema is managed by the cheap-db modules. On first startup, the schema will be created automatically via Flyway migrations (for PostgreSQL and MariaDB) or direct DDL execution (for SQLite).

## License

Licensed under the Apache License, Version 2.0. See the LICENSE file in the project root for details.

## Related Modules

- `cheap-core` - Core Cheap data model
- `cheap-json` - JSON serialization/deserialization
- `cheap-db-postgres` - PostgreSQL persistence
- `cheap-db-sqlite` - SQLite persistence
- `cheap-db-mariadb` - MariaDB persistence
