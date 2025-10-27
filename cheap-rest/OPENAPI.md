# OpenAPI/Swagger Documentation

## Overview

The Cheap REST API is documented using OpenAPI 3.0 specifications with Swagger UI for interactive documentation and testing.

## WebFlux Integration

The API documentation is configured for **Spring WebFlux** using:
- `springdoc-openapi-starter-webflux-ui` - WebFlux-specific OpenAPI UI
- Reactive endpoint support (Mono/Flux types)
- Netty server configuration

## Accessing Swagger UI

Once the application is running, access the Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

Or the OpenAPI JSON specification:

```
http://localhost:8080/v3/api-docs
```

## API Documentation

### General Information

- **Title**: Cheap REST API
- **Version**: 0.1
- **Description**: Reactive REST API for the Cheap data caching system using Spring WebFlux
- **License**: Apache License 2.0
- **Server**: http://localhost:8080 (WebFlux/Netty)

### Available Endpoints

#### Catalogs

**Tag**: Catalogs
**Description**: Catalog management endpoints for creating and querying catalogs

##### POST /api/catalog
- **Summary**: Create a new catalog
- **Description**: Creates a new catalog with the specified definition, species, and optional upstream catalog
- **Request Body**: CreateCatalogRequest (catalogDef, species, upstream)
- **Responses**:
  - 201: Catalog created successfully (CreateCatalogResponse)
  - 400: Invalid catalog definition or validation failed
  - 500: Internal server error

##### GET /api/catalog
- **Summary**: List all catalogs
- **Description**: Returns a paginated list of all catalog IDs with metadata
- **Query Parameters**:
  - `page` (integer, default: 0): Page number (zero-indexed)
  - `size` (integer, optional): Page size (defaults to configured value)
- **Responses**:
  - 200: Catalog list retrieved successfully (CatalogListResponse)
  - 400: Invalid page size (exceeds maximum)
  - 500: Internal server error

##### GET /api/catalog/{catalogId}
- **Summary**: Get catalog definition by ID
- **Description**: Retrieves the complete catalog definition including all hierarchy and aspect definitions
- **Path Parameters**:
  - `catalogId` (UUID): Catalog UUID
- **Responses**:
  - 200: Catalog definition retrieved successfully (GetCatalogDefResponse)
  - 404: Catalog not found
  - 500: Internal server error

## Running the Application

### With SQLite (for testing)

```bash
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=sqlite'
```

### With PostgreSQL

```bash
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=postgres'
```

### With MariaDB

```bash
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=mariadb'
```

## Configuration

The OpenAPI configuration is located in:
```
cheap-rest/src/main/java/net/netbeing/cheap/rest/config/OpenApiConfig.java
```

## API Annotations

Controllers use the following OpenAPI annotations:
- `@Tag` - Group endpoints by functional area
- `@Operation` - Document individual endpoint operations
- `@ApiResponses` - Define possible HTTP responses
- `@Parameter` - Document request parameters
- `@Schema` - Define response/request schemas

## Example Request

### Create a Catalog

```bash
curl -X POST http://localhost:8080/api/catalog \
  -H "Content-Type: application/json" \
  -d '{
    "catalogDef": {
      "hierarchyDefs": [
        {
          "name": "people",
          "type": "ES"
        }
      ],
      "aspectDefs": {
        "person": {
          "name": "person",
          "globalId": "550e8400-e29b-41d4-a716-446655440001",
          "propertyDefs": [
            {
              "name": "firstName",
              "type": "String",
              "isNullable": false
            }
          ]
        }
      }
    },
    "species": "SINK",
    "upstream": null,
    "uri": "http://example.com/test-catalog"
  }'
```

## Reactive Types in OpenAPI

All endpoints return reactive types (`Mono<T>` or `Flux<T>`), but these are transparently handled by SpringDoc OpenAPI. The documentation shows the unwrapped response types for clarity.

## Version History

- **0.1**: Initial WebFlux migration with OpenAPI documentation
