# Atomic — Multi-Database Management Backend

## What it is

Atomic is a REST API backend for a multi-tenant database administration tool —
think a lightweight, self-hosted alternative to phpMyAdmin/DBeaver, but
API-first and built for programmatic/UI-driven access rather than a desktop
client. Users register connection profiles for their own databases (MySQL,
PostgreSQL, with SQLite/MSSQL/Oracle groundwork in place), authenticate per
profile, and then browse schema, inspect table structure, visualize table
relationships as ER diagrams, and run ad-hoc SQL queries — all through a JSON
API secured with JWT.

## Key features

- **Multi-database connection management** — add, test, and store connection
  profiles per database engine (MySQL, PostgreSQL, extensible via a
  strategy-pattern login layer for MSSQL/Oracle).
- **Per-profile JWT authentication** — each database profile has its own login
  flow that issues a JWT scoped to that connection; an `AuthenticationFilter`
  resolves the current user/connection context on every request.
- **Schema introspection** — list tables in a schema, inspect column metadata,
  types, indexes, and constraints via JDBC `DatabaseMetaData`.
- **Table dependency analysis & ER diagrams** — computes foreign-key
  relationships between tables and exposes both a full database ER diagram and
  a single-table dependency view (built for rendering diagrams client-side).
- **Ad-hoc SQL query execution** — a `/run-query` endpoint that safely executes
  arbitrary SELECT/DML statements against the connected database and returns
  typed row/update results.
- **Table data browsing** — paginated data preview for any table.

## Tech stack

- **Java 21** on **Quarkus 3.37** (supersonic, subatomic REST framework)
- **Hibernate ORM with Panache** — active-record style persistence for the
  app's own metadata (registered database profiles, users)
- **Flyway** — versioned SQL migrations for the app's own schema
- **SmallRye JWT** — token issuance/validation for per-profile auth
- **Multiple JDBC drivers** — MySQL, PostgreSQL, SQLite (Quarkiverse) — to
  connect out to arbitrary target databases at runtime
- **SmallRye OpenAPI** — auto-generated API documentation
- **WebSockets** extension included (for future live query/streaming features)
- **Jakarta Bean Validation** (`quarkus-hibernate-validator`) for request DTOs

## Architecture

Layered, package-by-feature under `com.database` (feature code) and
`com.shared` (cross-cutting concerns):

```
resources/   → JAX-RS endpoints (thin controllers), grouped by
               connection / structure / info / helper
services/    → business logic orchestrating utils + repositories
utils/       → low-level JDBC/database logic (connection building,
               query execution, schema/structure/dependency queries,
               per-engine login strategies)
repository/  → Panache repositories for the app's own entities
model/       → JPA entities (DatabaseEntity, DbUserEntity)
dto/         → request/response records, split into request/ and response/

com.shared/
  security/    → AuthenticationFilter, CurrentUser (request-scoped context)
  exceptions/  → GlobalExceptionMapper + typed exceptions (409/404/401/validation)
  dto/         → common envelopes (ApiResponse, PaginatedListResponse)
  utils/       → JwtUtils
```

Request flow example (run a query):
`DatabaseTableStructureResource → DatabaseTableStructureService →
DatabaseQueryExecutionUtility → target DB via JDBC`

## Resume-ready summary

> Designed and built **Atomic**, a Quarkus/Java 21 backend for a multi-tenant
> database administration platform supporting MySQL and PostgreSQL. Implemented
> JWT-secured per-database-profile authentication, JDBC-based schema
> introspection, automated ER-diagram/table-dependency generation, and a
> safe ad-hoc SQL query execution API, following a layered
> resource/service/utility architecture with Flyway-managed migrations and
> OpenAPI documentation.

### Bullet points (pick 2–4 for a resume)

- Built a Quarkus REST API enabling users to connect to and administer
  multiple external databases (MySQL, PostgreSQL) through a single
  JWT-authenticated backend.
- Implemented JDBC schema-introspection services to generate table metadata,
  column/index details, and cross-table ER diagrams from live database
  connections.
- Designed a per-connection-profile authentication model issuing scoped JWTs,
  with a custom `AuthenticationFilter` resolving request-level database
  context.
- Developed a safe, generic SQL query execution engine returning typed
  row/update results over REST, with pagination support for large table
  previews.
- Structured the codebase in a layered resource/service/utility/repository
  architecture with Flyway migrations, Bean Validation, and OpenAPI docs.
