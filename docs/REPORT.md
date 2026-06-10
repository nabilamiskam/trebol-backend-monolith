Chapter 1: Introduction & Project Scope
1.1 Baseline and Migration Scope
This report documents a targeted refactor of the Product bounded context inside the Trébol backend monolith. The legacy codebase is organized in conventional technical layers (presentation, business services, data access). In practice, however, the product area exhibits significant coupling: JPA annotations, Spring Data repositories, and QueryDSL predicate composition leak into service code and controller plumbing. That coupling raises maintenance costs and prevents fast, framework-free validation of core business rules. 
The refactor is intentionally incremental and low-risk. Rather than a full rewrite of the monolith, the project applies the Strangler Fig Pattern to migrate one vertical domain (product management) into a Clean Architecture slice. The new org.trebol.product module is introduced alongside legacy modules. An Anti‑Corruption Layer (ACL) provides a reversible read bridge so legacy consumers can read authoritative product data from the new module while writes continue to use the legacy persistence path as a safe fallback. 
For testing and validation, both the legacy and new code run inside the same local H2 file-based test database (./data/trebol). The test setup preserves separation at the package and adapter level while allowing end-to-end checks that cover the ACL bridge, fallback behavior, and persistence adapter round-trips.
1.2 Research and Architectural Objectives
The targeted re-engineering of the Product domain is evaluated against four strict architectural benchmarks:
1.	Concentric-ring insulation: Move business policies into a domain core implemented in plain Java types and value objects, minimizing direct runtime dependencies on framework APIs inside the domain.
2.	Interface inversion compliance: Define persistence contracts (repository ports) inwards so that the business core expresses what it needs, and outbound adapters implement those contracts.
3.	External contract parity: Keep public REST endpoints, JSON wire formats, and security access controls stable so downstream integrators are not required to change.
4.	Multi-tier testing security: Validate the migration with a pyramid of automated tests that run from fast domain-level unit tests to full-context integration tests (MockMvc / SpringBoot test) that exercise ACL reads, legacy fallbacks, and transaction guarantees

Chapter 2: Problem Statement and Legacy Coupling Analysis
The legacy trebol-backend-monolith codebase presents a superficial three‑layer structure (presentation, business services, data access), but responsibilities leak across layers: business orchestration, query construction, persistence mapping, and framework-specific logic are distributed through several service classes. This cross‑cutting of concerns weakens domain boundaries, increases coupling, and impedes safe, incremental refactoring.
2.1 Current State: Package Structure and Class Organization
The codebase is organized by technical role rather than business capability. Key examples:

●	API controllers and transport models: `src/main/java/org/trebol/api/*`
●	Persistence entities and repositories: `src/main/java/org/trebol/jpa/*`
●	Service implementations: `src/main/java/org/trebol/jpa/services/*`

A single product read path crosses multiple packages:
 `DataProductsController` → generic CRUD controller → `ProductsCrudServiceImpl` → `ProductsPredicateServiceImpl` → `ProductsRepository` → `ProductsConverterServiceImpl` → `ProductPojo`. That flow fragments a single logical operation across delivery, orchestration, persistence-query construction, and mapping code, increasing cognitive load and obscuring where domain rules live.[1.1]

Figure 2.1: Legacy Technical Package Layout and Fragmented Product Context.

src/main/java/org/trebol/
├── api/controllers/
│   └── DataProductsController.java         ◄── LEGACY COUPLED INGRESS PERIMETER
├── api/models/
│   └── ProductPojo.java                    ◄── ANEMIC TRANSPORT TRANSFER CONTAINER
├── jpa/entities/
│   └── Product.java                        ◄── PERSISTENCE POISONED RELATIONAL SCHEMA
└── jpa/services/
    ├── crud/impl/ProductsCrudServiceImpl.java
    ├── conversion/impl/ProductsConverterServiceImpl.java
    └── predicates/impl/ProductsPredicateServiceImpl.java


2.2 Class Responsibility and Framework Entanglement
Rather than acting as a thin delivery adapter, DataProductsController inherits behavior from a highly coupled generic controller and pulls internal service schemas and framework concerns into the request path.
Component Asset	Primary Technical Role	Leaked Frameworks / Leakage Vectors
DataProductsController	Exposes REST endpoints, handles presentation routing.	Spring MVC, Spring Web, Spring Security via generic base class.
ProductPojo	Transport model used between API and service layer.	Jackson serialization annotations, Bean Validation API (acts as domain surrogate).
ProductsCrudServiceImpl	Orchestrates read/write flows and transactions.	Spring DI, Spring Transaction API, Spring Data JPA types.
ProductsConverterServiceImpl	Maps data between database entities and DTOs.	Jakarta Persistence API (JPA), Hibernate engine.
ProductsPredicateServiceImpl	Builds dynamic queries from parameters.	QueryDSL type safe metadata, BooleanBuilder primitives.
Table 2.2: Component responsibility matrix and framework infrastructure leakage.


2.3 Dependency Flow Before Refactoring

Requests on /data/products compile HTTP query parameters into a database predicate (QueryDSL/BooleanBuilder) and hand it to the Spring Data repository. The repository returns Hibernate entities, which are converted back into the flat ProductPojo DTO for the client. Infrastructure artefacts (query DSL, JPA entities, repository APIs) are therefore present throughout the lifecycle rather than being isolated at the edge. 

Figure 2.3: Downward dependency flow leading directly to infrastructure concerns.

===================================================================================
                  PRE-REFACTOR TRANSACTONAL DOWNWARD FLOW
===================================================================================
 [Client Inbound GET] ────► DataProductsController (Web)
                                   │
                                   ▼
                            DataCrudGenericController (Base Engine)
                                   │
                                   ▼
                            ProductsCrudServiceImpl (Orchestrator)
                                   │
                     ┌─────────────┴─────────────┐
                     ▼                           ▼
       ProductsPredicateServiceImpl    ProductsRepository (JPA)
       (Leaks QueryDSL Abstractions)             │
                     │                           ▼
                     │                 org.trebol.jpa.entities.Product
                     ▼                           │
       [Compiles DB Predicate] ──────────────────┘
===================================================================================


2.4 Core Architectural Pain Points

The baseline downward dependency flow produces four core problems motivating this refactor:
1.	Framework Coupling: Business logic is hard to execute or test outside a Spring/JPA context.
2.	Persistence Leakage: Domain shape and behavior are driven by database/query mechanics.
3.	Blurred Boundaries: No explicit use-case or port boundary separates application intent from technical orchestration.
4.	Change Amplification: Small API or schema changes require coordinated edits across controllers, mappers, services, and repositories.

2.5 Engineering Consequences and Scope of the Solution

These flaws produce measurable engineering costs:

1.	Reduced isolated testability (unit tests require heavy context or complex mocking).
2.	Elevated regression risk when changing schema or payload shapes.
3.	Increased cognitive load and slower feature delivery due to fragmented logic.

Conclusion
The root problem is architectural: domain rules are insufficiently insulated from framework and persistence concerns. The migration approach (Clean Architecture slice + reversible ACL reads) is targeted to restore clear use-case boundaries, invert dependencies, and enable fast, framework-free testing while preserving operational stability.
Chapter 3: Target Architecture & Decoupling Strategy
To eliminate the technical debt, framework coupling, and change amplification identified in the legacy baseline, the Product bounded context was restructured around a Clean Architecture and Hexagonal Architecture (Ports and Adapters) style. The goal of the target design is to keep business rules inside an insulated core while pushing frameworks, persistence, and web concerns to the outside of the system.
3.1 Target Topology and Package Directory Layout
The new Product slice is isolated under org.trebol.product. Its package structure follows concentric layers, with compile-time source code dependencies allowed to point inward only.
src/main/java/org/trebol/product
│
├── domain/                               ◄── 1. POLICY CORE PERIMETER (Pure Java 17)
│   ├── aggregate/
│   │   └── ProductAggregate.java         ◄── Validates and Enforces Business Invariants
│   ├── vo/                               ◄── Self-Validating Immutable Value Objects
│   │   ├── ProductId.java, ProductCode.java, ProductName.java, ProductPrice.java
│   └── port/
│       └── ProductRepository.java        ◄── Outbound SPI / Abstract Driven Port Contract
│
├── application/                          ◄── 2. USE-CASE ORCHESTRATION PERIMETER
│   ├── usecase/                          ◄── Single-Purpose Business Interactors
│   │   ├── CreateProductUseCase.java, ListProductsUseCase.java, GetProductUseCase.java
│   ├── command/                          ◄── State-Mutation Inputs (Java 17 Records)
│   ├── query/                            ◄── Read-Only Query Inputs (Java 17 Records)
│   ├── result/                           ◄── Flattened Output Data Carriers (Java 17 Records)
│   └── service/
│       └── ProductApplicationService.java◄── Unified Core Inbound Facade Gateway
│
├── adapter/                              ◄── 3. INTERFACE ADAPTER PERIMETER
│   ├── inbound/
│   │   ├── web/
│   │   │   └── ProductController.java    ◄── Driving Web Adapter (HTTP Ingress Routes)
│   │   └── dto/                          ◄── JSON Serialization Request/Response Schemas
│   └── outbound/
│       └── persistence/                  ◄── Driven Persistence Adapter (SQL/Hibernate)
│           ├── ProductJpaEntity.java     ◄── Relational Database Table Schema
│           ├── ProductJpaRepository.java ◄── Spring Data JPA Data Access Engine
│           └── ProductRepositoryAdapter.java
│
└── infrastructure/                       ◄── 4. FRAMEWORK CONFIGURATION PERIMETER
    └── ProductModuleConfiguration.java   ◄── Explicit Spring Manual Bean IoC Wire-up

Figure 3.1: Re-Engineered Product Context Concentric Package-per-Layer Directory Tree.
Architectural Boundary Specifications of the Topology:

1. The Pure Domain Layer (`domain`): The innermost layer containing the rich domain model—aggregates and value objects (for example, `ProductAggregate`, `ProductCode`, `ProductPrice`). Implemented in plain Java with no runtime dependencies on Spring, JPA, or database annotations. This layer captures business invariants and domain logic.

2. The Application Layer (`application`): Coordinates use cases and application workflows. It accepts immutable command/query objects and returns simple result objects. The application layer orchestrates domain behavior without knowledge of HTTP, SQL, or persistence implementation details.

3. The Adapter Layer (`adapter`): Contains boundary translation components that translate between external systems and the core. Inbound adapters (controllers and DTOs) map external requests into application commands/queries; outbound adapters (repository adapters) implement the domain's port interfaces and translate domain types to persistence models.

4. The Infrastructure Layer (`infrastructure`): The outermost layer providing framework and platform concerns (Spring configuration, database connectivity, JPA implementations, logging, etc.). Infrastructure wires and hosts adapters but does not influence domain semantics.

3.2 Port and Adapter Boundary Decisions
To cleanly isolate core workflow orchestrations from volatile external infrastructure tools, the target layout establishes clear technical interfaces acting as application sockets, categorizing interactions into primary and secondary streams.
 
Primary (Driving) Ports
Primary ports define the inbound use cases exposed by the application core. In this implementation, the primary ports are the use-case interfaces implemented by ProductApplicationService. ProductController acts as the driving adapter: it receives HTTP requests, maps them into command or query objects such as CreateProductCommand and ListProductsQuery, and delegates execution to the application service.

Secondary (Driven) Ports
Secondary ports define the outbound contracts the core depends on to reach external systems. In this implementation, ProductRepository is the secondary port. It exposes persistence operations using core domain types such as ProductAggregate, ProductId, and ProductCode, while the concrete JPA implementation lives in an outbound adapter.

3.3 Boundary Inversion Rationale: The Dependency Inversion Principle

Placing the ProductRepository interface inside the inner domain/ is a deliberate application of the Dependency Inversion Principle. The business core defines what it needs from storage, while the persistence layer provides the implementation.

This inversion is important because it prevents JPA, SQL, Spring Data, and database schema details from leaking into the domain model. The repository contract belongs to the core because the core owns the semantics of lookup, uniqueness, save, update, and delete operations. Infrastructure must conform to those rules, not define them.

The result is a one-way architectural flow: the core owns the contract, and the adapter fulfills it.

────┘      │
===================================================================================


 
Chapter 4: Migration and Implementation  Strategy
The Product bounded context was migrated incrementally using the Strangler Fig Pattern. This approach avoids a big-bang cutover and allows the legacy and refactored implementations to coexist while the new slice is validated.
4.1 Migration approach
The refactor was organized as a phased migration rather than a full rewrite. Reads were routed first, because they are easier to validate and can be switched back to the legacy path if needed. Writes remained on the legacy implementation during the early phases so that operational risk stayed low.

This produced a hybrid runtime model:

- the new `org.trebol.product` slice handles the target architecture,
- the legacy `org.trebol.jpa` code remains available as a fallback,
- read traffic can be routed through the Anti-Corruption Layer,
- write operations stay on the legacy path until the migration is complete.


 
src/main/java/org/trebol/
Note: `api/adapters/legacy` contains the Anti‑Corruption Layer (ACL) bridge files used by legacy services to read from the new `org.trebol.product` slice.
├── jpa/                                 ◄── LEGACY MONOLITHIC COUPLING PERIMETER
│   ├── entities/
│   │   └── Product.java                 ◄── Framework-First Relational Model (Hibernate/QueryDSL)
│   └── services/crud/impl/ProductsCrudServiceImpl.java 
├── api/adapters/legacy/               ◄──contains the bridge files used by legacy services to read from the new slice
│   ├── ProductLookupAdapter.java 
│   └── ProductLookupService.java 
└── product/                             ◄── TARGET CONCENTRIC ARCHITECTURE BOUNDARY
    ├── domain/                          ◄── Protected Policy Core (0% Monolithic Footprint)
    │   ├── aggregate/
    │   │   └── ProductAggregate.java
    │   └── vo/
4.2 Scope Boundaries/Limitation

The Product domain is fundamentally intertwined with other monolithic sub-domains—such as Orders, Receipts, and Product Lists, Consequently, the scope of this project is strictly confined to Phase II (Hybrid Integration) for the Product domain.
This obsolescence (Phase III) can only be realized once those dependent domains are fully refactored into the new architecture.

4.3 The Vertical Slice Strategy

The migration was implemented feature by feature instead of layer by layer. A vertical slice covers a complete feature path end‑to‑end (API → application → domain → persistence)

This approach was selected for three practical reasons:
1.	each slice remains buildable and testable on its own,
2.	integration issues surface earlier because the feature path is exercised end to end,
3.	the first slice becomes a reusable template for later slices.

Structural Topology of the Slice (Example: GET)
THE VERTICAL SLICE COLUMN 
Layer 1: Inbound	ProductController (HTTP, Security, Request DTOs) 
──► GET /product-module/{id} 
Layer 2: Core Application 	ProductApplicationService 
──► GetProductQuery ──► GetProductUseCase 
Layer 3: Domain 	ProductAggregate 
──► Value Objects (ProductId, ProductCode, Price) 
Layer 4: Outbound Adapter 	ProductRepositoryAdapter 
──► ProductJpaRepository ──► Physical Database 

Product Bounded Context Slices
The product bounded context was migrated into five vertical feature slices. Each slice maps to one or more HTTP endpoints implemented in `ProductController`:

- Slice 1 — Single-resource querying: GET `/product-module/{id}` (retrieve a single product).
- Slice 2 — Paginated collective queries: GET `/product-module` (paged, filtered listing).
- Slice 3 — Resource registration: POST `/product-module` (create product; requires `products:create`).
- Slice 4 — Full resource update: PUT `/product-module/{id}` (replace/update product; requires `products:update`).
- Slice 5 — Resource teardown: DELETE `/product-module/{id}` (delete product; requires `products:delete`).

- Slice 6 — Bulk partial updates: PATCH `/product-module` (bulk patch by filter; requires `products:update`).

This endpoint accepts the same filter query parameters as the list endpoint and a JSON object containing field changes to apply to all matched products. Processing is transactional: `ProductApplicationService.execute(BulkPatchProductCommand)` applies `applyPatch(...)` per product and persists updates atomically. See `src/main/java/org/trebol/product/adapter/inbound/web/ProductController.java` and `src/main/java/org/trebol/product/application/service/ProductApplicationService.java` for the implementation and behavior.

These slices were implemented as complete vertical flows (API → application → domain → persistence), allowing each feature to be built, tested, and validated independently.

Operational Slicing Asymmetry: Slices 1 and 2 (the read pathways) were prioritized and promoted via the Anti-Corruption Layer (ACL). This structural choice allowed the system to serve production reads from the new architecture while temporarily leaving write operations on the legacy path, deriving immediate architectural value with minimal system risk.
4.4 Implementation order
The implementation within each slice follows a bottom-up sequence:

 
Step 1: Model the pure domain aggregate roots and immutable value objects first. This baseline captures all business invariants in pure Java 17 before writing any framework logic.
Step 2: Construct the application use-case interactors and input query/command records around the stable domain objects.
Step 3: Implement the outbound infrastructure adapters, relational database table schemas, and serialization data mappers required to satisfy the abstract port interfaces.
Step 4: Code the inbound REST delivery controllers, mapping the external HTTP API contracts directly onto the application use cases.

Observed Outcome: This implementation sequence prevents rework. Outer presentation and data storage tiers are forced to conform to the requirements of a stable business core, instead of shaping your core policies around framework constraints. 
4.5 Validation Strategy
The migration was verified at multiple levels:
domain unit tests check value object rules and aggregate behavior,
●	adapter integration tests verify persistence mapping against the test database,
●	controller tests confirm request handling, validation, and JSON response shape,
●	full-stack tests exercise the product flow through the application runtime.
4.6 Managed Coexistence Handling
The slice implementation concludes with the configuration of the safety-net bridge:
●	Legacy Write Preservation: Preservation of legacy write paths within ProductsCrudServiceImpl to retain rollback options.
●	ACL Read Rerouting: Rerouting of the validated read flow through the ProductLookupService ACL interface, maintaining an explicit database fallback mechanism to guarantee uninterrupted runtime availability.
 
Chapter 5: Concrete Implementation of the Product Bounded Context
This chapter describes how the target Product bounded context is implemented in production-grade Java code. It shows the request flow through the refactored module and explains how the Anti-Corruption Layer (ACL) preserves compatibility with legacy services during the transition.
5.1 Scope
The implementation focuses on two things: the new Clean Architecture request flow inside `org.trebol.product`, and the ACL bridge used by legacy services to read from the new module. The goal is to keep the domain core insulated while still supporting the existing monolithic application during migration.

5.2 Single-product read flow

The clearest example of the refactored module is the single-product read path. 

The request flow is straightforward:

1. A client sends a `GET` request to the product controller.
2. The controller validates the input and maps it to an application query.
3. The application service coordinates the use case.
4. The repository port is called through the outbound persistence adapter.
5. The adapter queries the database and maps the result back to the domain model.
6. The controller returns the HTTP response.

This flow keeps transport, orchestration, and persistence separate. Each layer has a single responsibility, which makes the path easier to test and easier to change.

5.3 Filtering and pagination
The list endpoint supports pagination and dynamic filtering without exposing persistence details to the application core.

The request flow is similar to the single-item read:

1. The controller receives page and filter parameters.
2. The controller converts them into a `ListProductsQuery` record.
3. The application service delegates the query to the repository port.
4. The persistence adapter converts the filter map into a Spring Data JPA `Specification`.
5. The query runs through `JpaSpecificationExecutor` with pagination applied.
6. The adapter maps the entities back into domain aggregates.
7. The application layer wraps the output in a `PagedProductResult`.

This design keeps filtering logic in the adapter layer instead of the core. The core sees a typed query and a typed result, not database-specific criteria objects.

 

5.4 Public API Ingress Transport Contracts

Example of two read-centric public API contracts through the modernized ProductController ingress point. These contracts guarantee strict structural stability and full backward compatibility with pre-existing enterprise integrations.

Endpoint 1: Retrieve Single Product
●	Route: GET /product-module/{id}
●	Path Parameter: id (Long) - Product unique database identifier.
●	Response Mappings:
○	200 OK: Resource successfully materialized.
○	404 Not Found: Target resource identifier absent from the persistent store.
{
  "id": 1,
  "code": "PROD-1",
  "name": "Product Name",
  "price": 99.99,
  "isActive": true
}

Endpoint 2: Collective Paged Retrieval with Dynamic Criteria Filters
●	Route: GET /product-module
●	Query Evaluation Parameters:
○	pageIndex (int, default: 0): Zero-based paginated offset index.
○	pageSize (int, default: 10): Quantitative boundary per sub-list page.
○	code (String, optional): Exact match filtering on unique item alphanumeric sequences.
○	nameLike (String, optional): Case-insensitive partial matching sequence on names.
○	barcode (String, optional): Legacy-compatible index field resolution sequence.
○	sortBy (String, optional): Sorting field allocation (id, name, barcode, price).
○	order (String, optional): Evaluation sorting vector direction (asc or desc)
.{
  "items": [
    {
      "id": 1,
      "code": "PROD-1",
      "name": "Product Name",
      "price": 99.99,
      "isActive": true
    }
  ],
  "totalCount": 42
}
 
API Endpoints Reference

| Method | Path | Query Params | Request Body (JSON) | Success | Errors |
|---|---|---|---|---|---|
| GET | /product-module/{id} | — | — | 200 OK — ProductResponse JSON | 404 Not Found; 400 Bad Request; 403 Forbidden |
| GET | /product-module | pageIndex, pageSize, code, nameLike, barcode, sortBy, order | — | 200 OK — PagedProductResponse { items: [...], totalCount } | 400 Bad Request; 403 Forbidden |
| POST | /product-module | — | { "code": "PROD-1", "name": "Product Name", "price": 99.99, "isActive": true } | 201 Created — Location header /product-module/{id} + ProductResponse body | 400 Validation Error; 409 Conflict (code exists); 403 Forbidden |
| PATCH | /product-module | filter params (same as list) | { "price": 19.99 } (map of changes) | 200 OK — BulkPatchProductResponse (results, count) | 400 Validation Error; 404 Not Found (no products matched); 403 Forbidden |
| PUT | /product-module/{id} | — | { "code": "PROD-1", "name": "New Name", "price": 79.99, "isActive": true } | 200 OK — ProductResponse | 400 Validation Error; 404 Not Found; 409 Conflict; 403 Forbidden |
| DELETE | /product-module/{id} | — | — | 204 No Content | 404 Not Found; 403 Forbidden |

5.5 Parallel Coexistence and the Anti-Corruption Layer Bridge
The ACL allows the legacy product services and the new Product bounded context to run in parallel. `ProductLookupService` defines the read contract used by the legacy code, and `ProductLookupAdapter` implements that contract by delegating reads into the new module.

This arrangement lets the new bounded context become the preferred source for reads without forcing an immediate rewrite of the legacy service layer. If the ACL path does not resolve a result, the legacy implementation can still fall back to its original repository-based read logic.

### Legacy read flow

The compatibility path works as follows:

1. The legacy CRUD layer calls `ProductLookupService`.
2. `ProductLookupAdapter` forwards the request into the Product application layer.
3. The Product bounded context resolves the data.
4. If needed, the legacy service falls back to the original repository path.

This bridge keeps the migration reversible and avoids forcing all consumers to move at once.
 

5.6 Dependency contrast
The refactored code follows inward dependencies. The legacy code followed the opposite pattern, where controllers and services depended directly on persistence and framework details.

The practical difference is visible in the module structure:

- the legacy path couples web, service, and persistence layers tightly,
- the new path keeps the domain core independent,
- the adapter layer handles framework translation,
- the application layer coordinates use cases without technical leakage.

This structure makes the Product bounded context easier to maintain, easier to test, and safer to evolve.

 
 
Chapter 6: Testing Strategy And Validation
The Product migration was validated with a layered test strategy. The objective was to confirm that the new Product bounded context behaves correctly in isolation, integrates with the Spring runtime, and remains compatible with the legacy monolith during the transition.

6.1 The Hexagonal Testing Pyramid

The verification strategy is organized into three levels:
============================================================================
                      THE PRODUCT MODULE TESTING PYRAMID
============================================================================
                 ▲
                / \       [Tier 3] INTEGRATION & E2E PERIMETER 
               /   \ - E2E tests validate the full request flow through the running Spring Boot application.
             /──\
            /         \   [Tier 2] APPLICATION USE-CASE PERIMETER (Medium, Isolated)
          /             \ - Validates use cases, commands, queries, and results handlers
         /──────\
        /                 \ [Tier 1] PURE DOMAIN UNIT PERIMETER
       /                   \- Framework-Free Core Unit Tests (Pure Java 17) 
     └────────┘
============================================================================

6.2 Tiered Test Inventory and Scope

Tier 1: Pure Domain Unit Tests
The domain test suite covers aggregate and value object behavior.

Examples include:
- `ProductAggregateTest`
- `ProductCodeTest`
- `ProductIdTest`
- `ProductNameTest`
- `ProductPriceTest`

These tests run without Spring, Mockito, or database dependencies. They verify input validation, invariant enforcement, equality behavior, and aggregate mutation rules.

Tier 2: Application Use-Case Tests
`ProductApplicationServiceTest` exercises the application layer in isolation.

It uses mocked repository ports to verify:

- command and query handling,
- duplicate-code validation,
- result mapping,
- use-case orchestration.

This layer confirms that the business workflow is correct before any web or database concerns are involved.

Tier 3: Inbound Web Controller Slice Tests
`ProductControllerTest` verifies the web ingress layer with a focused Spring MVC slice.

It checks:

- HTTP status codes,
- request routing,
- input validation,
- security annotations,
- JSON response structure.

The controller tests use mocked application services and mappers so that the web contract can be validated without loading the full application context.

Tier 3: Outbound Driven Persistence Adapter Tests
`ProductRepositoryAdapterTest` validates the outbound persistence adapter against the real file-based H2 test database.

It verifies:

- database round-trips,
- sorting and pagination,
- dynamic filtering,
- entity-to-domain mapping,
- domain-to-entity mapping.

These tests use the active test database configuration under `./data/trebol` and do not rely on mocks for persistence behavior.

6.3 Coexistence and Backwards Compatibility (ACL Bridge)

The migration remains reversible through the ACL bridge. The tests show that legacy services can read from the new Product module while writes still follow the legacy repository path.

Key checks include:

- `ProductLookupAdapterTest` verifies that `ProductResult` values are translated into legacy `ProductPojo` and transient JPA representations.
- `ProductsCrudServiceImplAclTest` verifies that legacy read paths prefer the ACL bridge and fall back to the repository when the ACL does not return a match.
- `ProductsCrudServiceImplTest` confirms that legacy create and update flows continue to use the legacy repository and converter path.

These tests show that read migration can proceed independently from write migration.

6.3.1 Evidence Table (claim → test → purpose)

| Claim | Test class | Purpose |
|---|---|---|
| Domain invariants and value objects | [src/test/java/org/trebol/product/domain/ProductPriceTest.java](src/test/java/org/trebol/product/domain/ProductPriceTest.java) | Validate value-object rules, input validation, equality and invariants |
| Use-case orchestration correctness | [src/test/java/org/trebol/product/application/service/ProductApplicationServiceTest.java](src/test/java/org/trebol/product/application/service/ProductApplicationServiceTest.java) | Verify command/query handling, duplicate checks and result mapping |
| Web contract and request validation | [src/test/java/org/trebol/product/adapter/inbound/web/ProductControllerTest.java](src/test/java/org/trebol/product/adapter/inbound/web/ProductControllerTest.java) | Assert HTTP routing, validation annotations, and JSON response shape |
| Persistence adapter mapping & queries | [src/test/java/org/trebol/product/adapter/outbound/persistence/ProductRepositoryAdapterTest.java](src/test/java/org/trebol/product/adapter/outbound/persistence/ProductRepositoryAdapterTest.java) | Verify DB round-trips, filtering, pagination, and entity↔domain mapping |
| ACL adapter mapping | [src/test/java/org/trebol/api/adapters/legacy/ProductLookupAdapterTest.java](src/test/java/org/trebol/api/adapters/legacy/ProductLookupAdapterTest.java) | Ensure `ProductResult` is translated into legacy `ProductPojo`/transient JPA forms |
| Legacy service ACL preference & fallback | [src/test/java/org/trebol/jpa/services/crud/impl/ProductsCrudServiceImplAclTest.java](src/test/java/org/trebol/jpa/services/crud/impl/ProductsCrudServiceImplAclTest.java) | Assert legacy `ProductsCrudServiceImpl` prefers ACL reads and falls back to repository |
| Full end-to-end flow | [src/test/java/org/trebol/product/adapter/inbound/web/ProductControllerE2ETest.java](src/test/java/org/trebol/product/adapter/inbound/web/ProductControllerE2ETest.java) | Full create/list/get/update/delete flow in a running Spring Boot context |


============================================================================
                    ANTI-CORRUPTION LAYER (ACL) BOUNDARY TEST

 [Legacy Monolith Consumer] ──► Requests Barcode Lookup ──► [ProductLookupAdapter]
                                                                  │
                                                        (Translates Payload)
                                                                  ▼
 [Legacy Monolith Consumer] ◄── Receives Legacy ProductPojo ◄── [ProductResult Record]
============================================================================

6.4 Full-Context Integrated Verification Pipeline

`ProductControllerE2ETest` validates the complete Product request lifecycle in a running Spring Boot context.

This test covers:

- dependency injection wiring,
- Spring Security filters,
- controller-to-database execution flow,
- transactional behavior,
- request/response continuity through MockMvc.

The end-to-end test exercises create, list, read, update, and delete operations against the real application runtime.

6.5 Build Verification Evidence
The build was verified through Maven test execution. For report readers, the proof should be stated as a short evidence block rather than as raw console output.

Recommended wording:

"The project build completed successfully with Maven. Targeted controller, integration, and full-project test runs passed without compilation or test failures."

Evidence commands:

```bash
mvn test
```

Optional narrower checks:

```bash
mvn "-Dtest=org.trebol.product.adapter.inbound.web.ProductControllerTest" test
mvn "-Dtest=org.trebol.product.adapter.inbound.web.ProductControllerE2ETest" test
```

If you want stronger proof in the report, add the exact command, the exit code, and a one-line result summary such as: "Exit code 0; all tests passed; no compilation errors were reported."
Chapter 7: Outcomes, Limitations, and Future Work
7.1 Migration outcome
The Product bounded context was separated into a Clean Architecture slice under `org.trebol.product`. The refactor introduced a domain core, an application layer, inbound and outbound adapters, and explicit infrastructure wiring. In parallel, the legacy monolith kept working through the ACL bridge so that read traffic could move gradually without forcing a full cutover.

The result is a hybrid system. The new Product slice now owns the target architecture for reads and application flow, while the legacy service layer remains available for fallback and for any write paths that have not yet been retired.

7.2 Evidence of success

The migration is supported by tests at several levels:

- domain tests verify value object rules and aggregate behavior,
- application tests verify use-case orchestration and business rule handling,
- controller tests verify HTTP routing, validation, status codes, and response shape,
- persistence adapter tests verify mapping, filtering, sorting, pagination, and database round-trips,
- ACL tests verify that legacy consumers can read through the new module,
- end-to-end tests verify the full request flow in a running Spring Boot context.

Together, these tests show that the Product module can be exercised independently and also integrated safely into the legacy runtime.
7.3 Remaining limitations
The migration is not yet complete because the Product bounded context is still a shared dependency surface for several other domains. Orders, checkout flows, customer-facing product lists, and related read models still rely on the current Product contract, response shape, and lookup semantics. That means the old bridge cannot be removed until those consumers are migrated or proven compatible with the new slice.

In practical terms, the current state is a controlled coexistence model rather than a full retirement:

- the new `org.trebol.product` module is the target architecture,
- the legacy path remains available as a safety net for consumers that have not been migrated,
- read routing can move gradually, but only after confirming that all downstream consumers behave correctly against the new module,
- write-path retirement must wait until dependent workflows no longer require legacy persistence semantics.

The main remaining items are:
- validate production fallback metrics before disabling the legacy read bridge,
- complete adapter and consumer migration for dependent systems that still call the product context directly,
- add final boundary rules and CI gates so cross-domain dependencies are detected earlier,
- verify price mapping behavior where the persistence model uses integer storage,
- run final smoke tests with the dependent domains before removing the legacy code path.
7.4 Operational impact

The refactor reduced coupling between business logic and framework code. The Product domain is now easier to test in isolation, easier to evolve without touching the full monolith, and easier to validate before release.

The staged approach also reduced migration risk. Because the ACL bridge keeps the system reversible, the team can continue moving traffic gradually instead of committing to a single risky cutover.

7.5 Next steps

The next phase should focus on controlled retirement of the legacy path:

1. Confirm read fallback usage has dropped to zero or an acceptable threshold.
2. Disable the legacy read bridge in staging.
3. Run integration smoke tests against the staged configuration.
4. Remove the legacy bridge code once the staged rollout is stable.
5. Add monitoring and rollback checks for production rollout.

  COMPLETED PHASES & FUTURE EVOLUTION ROADMAP
========================================================================
   [ PHASE I: DESIGN & ANALYSIS ]       ──► Legacy Coupling Review & Boundary Definition (Complete)
                                                        │
                                                        ▼
   [ PHASE II: HYBRID INTEGRATION ]     ──► Product Slice Modular Isolation
                                            Active ACL Bridge Enabled
                                            Shared Local Storage Footprint (./data/trebol)
                                            Authoritative Greenfield Read Pathways (Complete)
                                                        │
                                                        ▼
   [ PHASE III: TOTAL OBSOLESCENCE ]    ──► Refactor Dependent Monolithic Subdomains (Future Work)
                                            (Orders, Checkout, Customer Profiles, Lists)
                                            Complete Removal of Legacy jpa/ Namespace Assets
========================================================================

7.6 Final conclusion

The Product migration achieved its main goal: it established a modular, testable Product slice while preserving compatibility with the existing monolith during the transition. The remaining work is operational rather than architectural. Once the fallback path is no longer needed and the dependent consumers are migrated, the legacy Product implementation can be retired safely.

Chapter 8: References / Bibliography (Literaturverzeichnis)

This appendix lists the foundational architectural references that informed the migration strategy and documentation.

1. Martin, Robert C. (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design.* Addison-Wesley.
2. Cockburn, Alistair. (2005). *Ports and Adapters (Hexagonal) Architecture.*
3. Fowler, Martin. (2004). *Strangler Fig Application.*

The implementation also relied on practical tooling and platform documentation for the Java/Spring stack, Maven build execution, and PlantUML/Mermaid diagram generation.

