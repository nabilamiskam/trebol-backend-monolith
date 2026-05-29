# Clean Architecture Testing Report

## Purpose

This report summarizes the tests currently in place for the new Clean Architecture product module, the additional tests that were added during the reengineering work, and a practical testing strategy for keeping the application safe during and after the migration.

The goal is not to test everything equally. The goal is to protect the new architecture boundary, preserve the legacy bridge while it still exists, and make sure refactors can happen without breaking product behavior.

## What Was Added

The following tests were added to support the migration from the legacy product flow to the new Clean Architecture flow:

- [src/test/java/org/trebol/api/adapters/legacy/ProductLookupAdapterTest.java](../src/test/java/org/trebol/api/adapters/legacy/ProductLookupAdapterTest.java)
  - Verifies the ACL adapter mapping from `ProductResult` into legacy `ProductPojo` and transient JPA `Product` instances.
  - Covers null service responses, barcode lookup, and transient entity mapping.

- [src/test/java/org/trebol/jpa/services/crud/impl/ProductsCrudServiceImplAclTest.java](../src/test/java/org/trebol/jpa/services/crud/impl/ProductsCrudServiceImplAclTest.java)
  - Verifies the legacy CRUD service prefers the new ACL read path when possible.
  - Covers ACL hit, ACL miss, and fallback to the legacy repository.

- [src/test/java/org/trebol/api/controllers/DataProductsControllerAclTest.java](../src/test/java/org/trebol/api/controllers/DataProductsControllerAclTest.java)
  - Verifies the legacy REST controller can still create products while the ACL bridge is active.
  - Covers ACL hit returning `400`, and ACL miss allowing `201`.

- [src/test/java/org/trebol/jpa/services/crud/impl/ProductCreateImageRollbackTest.java](../src/test/java/org/trebol/jpa/services/crud/impl/ProductCreateImageRollbackTest.java)
  - Verifies product creation remains transactional when image persistence fails.
  - Confirms no partial product is left behind after a downstream failure.

## Existing Tests Already Covering the New Architecture

These tests are the main baseline for the new product module itself:

- [src/test/java/org/trebol/product/domain/vo/ProductPriceTest.java](../src/test/java/org/trebol/product/domain/vo/ProductPriceTest.java)
- [src/test/java/org/trebol/product/domain/vo/ProductNameTest.java](../src/test/java/org/trebol/product/domain/vo/ProductNameTest.java)
- [src/test/java/org/trebol/product/domain/vo/ProductCodeTest.java](../src/test/java/org/trebol/product/domain/vo/ProductCodeTest.java)
- [src/test/java/org/trebol/product/domain/aggregate/ProductAggregateTest.java](../src/test/java/org/trebol/product/domain/aggregate/ProductAggregateTest.java)
- [src/test/java/org/trebol/product/domain/service/ProductDomainServiceTest.java](../src/test/java/org/trebol/product/domain/service/ProductDomainServiceTest.java)
- [src/test/java/org/trebol/product/application/service/ProductApplicationServiceTest.java](../src/test/java/org/trebol/product/application/service/ProductApplicationServiceTest.java)
- [src/test/java/org/trebol/product/adapter/inbound/web/ProductControllerTest.java](../src/test/java/org/trebol/product/adapter/inbound/web/ProductControllerTest.java)
- [src/test/java/org/trebol/product/adapter/outbound/persistence/ProductRepositoryAdapterTest.java](../src/test/java/org/trebol/product/adapter/outbound/persistence/ProductRepositoryAdapterTest.java)

### What they cover

- Domain value-object rules and invariants.
- Aggregate behavior and business rules.
- Use-case orchestration in the application layer.
- HTTP request/response mapping for the new product controller.
- Persistence adapter behavior and JPA round-tripping.

## Testing Strategy For The Reengineering Process

### 1. Protect the architecture boundary first

Start by testing the boundary between the old monolith and the new product module.

What to verify:

- The ACL adapter maps new product results back into legacy models correctly.
- The legacy CRUD service reads from the new product module when possible.
- The legacy code still falls back to the old repository where necessary.

Why this matters:

- This is the highest-risk area during migration.
- If this bridge fails, the application can show stale, missing, or malformed product data.

### 2. Keep the core Clean Architecture code fully tested

The new product module should remain the source of truth for product behavior.

What to verify:

- Domain objects enforce their own rules.
- Application services return the correct results and exceptions.
- Controller mapping works and returns the intended HTTP status codes.
- Persistence adapters persist and load the domain consistently.

Why this matters:

- It allows the new module to evolve independently from the legacy monolith.
- It reduces the chance of reintroducing JPA-centric logic into the business core.

### 3. Add one integration test per important flow

Keep integration coverage small but high value.

Recommended flows:

- Read product by id.
- Read product by barcode.
- Create product.
- Update product.
- Delete product.
- Create product with image linkage.
- Roll back when image persistence fails.

Why this matters:

- These tests catch wiring problems that unit tests cannot detect.
- They prove the application still works end-to-end after refactoring.

### 4. Add targeted negative tests where behavior is risky

Not every edge case needs a full integration test, but risky cases do.

Recommended negative scenarios:

- Unauthorized or forbidden access.
- Invalid request bodies.
- Duplicate code creation.
- Not-found conditions.
- Invalid pagination or sorting input.
- Transaction rollback on downstream failure.

Why this matters:

- Most regressions during reengineering show up as error-handling bugs.
- Negative tests prevent silent failures from leaking into production.

### 5. Run tests in layers during the migration

Suggested execution order:

1. Domain unit tests.
2. Application service tests.
3. Adapter tests.
4. Legacy ACL bridge tests.
5. Web MVC tests.
6. Selected SpringBoot or transaction tests.

Why this matters:

- Fast tests give quick feedback while the code is still changing.
- Slower integration tests are reserved for verifying behavior across boundaries.

## Which New Tests Still Need To Be Written

These are useful follow-ups, but they are not required for the bridge to work today:

- Controller security negative tests.
- DTO validation matrix tests for `ProductRequest`.
- Exception propagation / HTTP mapping tests.
- Concurrency duplicate-code race test.
- Bulk/pagination/sorting edge-case tests.
- Repository-adapter complex-field roundtrip tests.
- Migration/read-after-write reconciliation tests.

These can be added gradually after the main migration boundary is stable.

## Which Existing Tests Are Obsolete Or Can Be Deprioritized

At the moment, very few tests are truly obsolete. Most are still useful.

### Not obsolete yet

- `ProductsCrudServiceImplTest` is still useful because the legacy write path remains active.
- `DataProductsControllerTest`-style controller coverage is still useful while the `/data/products` endpoint exists.
- Repository adapter tests remain useful because the persistence layer is still real and can regress.

### Candidates for future deprecation

These tests can become obsolete only after the legacy path is fully retired:

- Legacy CRUD service tests for code that no longer executes in production.
- Legacy controller tests for endpoints that are removed or permanently replaced.
- Tests that only verify legacy JPA mapping after the JPA path is deleted.

### Practical rule

Do not delete tests just because a new module exists.
Delete them only when:

- the code path they cover is removed, or
- the new tests fully replace the old behavior with the same or better coverage.

## Recommended Coverage Priorities

If the team wants to keep the test suite focused, prioritize in this order:

1. Product domain and application tests.
2. ACL adapter and legacy bridge tests.
3. Controller integration tests for the new module.
4. Transaction rollback and persistence adapter tests.
5. Security, validation, concurrency, and edge-case tests.

## Bottom Line

The new Clean Architecture code needs strong unit coverage at the domain and application layers, plus a small set of integration tests that prove the bridge to the legacy monolith still works.

The four most important migration-safety tests are:

- ACL adapter tests.
- Legacy CRUD ACL bridge tests.
- Controller→ACL integration test.
- Transaction rollback test for product creation with images.

Everything else is useful, but it can be added later as the reengineering expands.
