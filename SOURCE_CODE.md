# Source Code Overview — cheap (Java)

| Directory | Contents |
|-----------|----------|
| `cheap-core/` | Core CHEAP interfaces and basic implementations — the foundational library with zero external dependencies. |
| `cheap-db-postgres/` | PostgreSQL persistence layer for CHEAP catalogs, using JDBC; recommended for production. |
| `cheap-db-sqlite/` | SQLite persistence layer; lightweight, ideal for development and single-user apps. |
| `cheap-db-mariadb/` | MariaDB/MySQL persistence layer for CHEAP catalogs. |
| `cheap-json/` | JSON serialization and deserialization for CHEAP objects using Jackson. |
| `cheap-rest/` | Spring Boot REST API service with Swagger UI and support for all three database backends. |
| `cheap-rest-client/` | Java client library for consuming the `cheap-rest` REST API. |
| `cheap-rag/` | Java metadata extractor (JavaParser-based) that outputs `MetadataArtifact` JSON for the cheap-rag pipeline. |
| `cheap-rag-client/` | Type-safe Java REST client (Spring WebFlux) for querying the Python cheap-rag API. |
| `integration-tests/` | Heavyweight integration tests that run against real database instances. |
| `doc/` | Design documentation for the CHEAP data model. |
| `gradle/` | Gradle wrapper and version catalog (`libs.versions.toml`) for dependency management. |
