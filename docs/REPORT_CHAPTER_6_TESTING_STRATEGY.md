# Chapter 6: Testing Strategy and Validation

The Product migration was validated with a layered test strategy. The objective was to confirm that the new Product bounded context behaves correctly in isolation, integrates with the Spring runtime, and remains compatible with the legacy monolith during the transition.

## 6.1 Testing approach

The verification strategy is organized into three levels:

1. Domain tests validate business rules in pure Java.
2. Application and adapter tests validate use cases, controllers, and persistence behavior.
3. End-to-end tests validate the full request flow through the running Spring Boot application.

This structure keeps fast checks at the bottom of the stack and reserves expensive integration tests for the places where wiring and runtime behavior matter.

## 6.2 Test inventory

### Domain tests

The domain test suite covers aggregate and value object behavior.

Examples include:

- `ProductAggregateTest`
- `ProductCodeTest`
- `ProductIdTest`
- `ProductNameTest`
- `ProductPriceTest`

These tests run without Spring, Mockito, or database dependencies. They verify input validation, invariant enforcement, equality behavior, and aggregate mutation rules.

### Application tests

`ProductApplicationServiceTest` exercises the application layer in isolation.

It uses mocked repository ports to verify:

- command and query handling,
- duplicate-code validation,
- result mapping,
- use-case orchestration.

This layer confirms that the business workflow is correct before any web or database concerns are involved.

### Controller tests

`ProductControllerTest` verifies the web ingress layer with a focused Spring MVC slice.

It checks:

- HTTP status codes,
- request routing,
- input validation,
- security annotations,
- JSON response structure.

The controller tests use mocked application services and mappers so that the web contract can be validated without loading the full application context.

### Persistence adapter tests

`ProductRepositoryAdapterTest` validates the outbound persistence adapter against the real file-based H2 test database.

It verifies:

- database round-trips,
- sorting and pagination,
- dynamic filtering,
- entity-to-domain mapping,
- domain-to-entity mapping.

These tests use the active test database configuration under `./data/trebol` and do not rely on mocks for persistence behavior.

## 6.3 ACL and coexistence testing

The migration remains reversible through the ACL bridge. The tests show that legacy services can read from the new Product module while writes still follow the legacy repository path.

Key checks include:

- `ProductLookupAdapterTest` verifies that `ProductResult` values are translated into legacy `ProductPojo` and transient JPA representations.
- `ProductsCrudServiceImplAclTest` verifies that legacy read paths prefer the ACL bridge and fall back to the repository when the ACL does not return a match.
- `ProductsCrudServiceImplTest` confirms that legacy create and update flows continue to use the legacy repository and converter path.

These tests show that read migration can proceed independently from write migration.

## 6.4 Full-context verification

`ProductControllerE2ETest` validates the complete Product request lifecycle in a running Spring Boot context.

This test covers:

- dependency injection wiring,
- Spring Security filters,
- controller-to-database execution flow,
- transactional behavior,
- request/response continuity through MockMvc.

The end-to-end test exercises create, list, read, update, and delete operations against the real application runtime.

## 6.5 Build integration

The testing strategy is tied directly into Maven so engineers can run the relevant scope from the command line.

Examples:

```bash
mvn -Dtest=org.trebol.product.adapter.inbound.web.ProductControllerTest test
```

```bash
mvn "-Dtest=org.trebol.product.adapter.inbound.web.ProductControllerE2ETest" test
```

```bash
mvn -Dtest=org.trebol.product.** test
```

This makes it possible to run quick feedback loops during development while still supporting full validation before promotion.

## 6.6 Validation outcome

The test structure demonstrates that the Product migration is verifiable at each layer. Domain rules are checked quickly, adapter behavior is validated against a real database, and the full stack is exercised without leaving the application process.

This gives the migration a clear validation path and reduces the risk of introducing regressions while the legacy and modernized paths coexist.
