# Product Domain Clean Architecture Baseline And Migration Playbook

This document consolidates the agreed target architecture, migration strategy, contract-freeze rules, and baseline tests for product-domain reengineering.

Date: 2026-04-02

## 1. Target Structure

### Product module root
- `src/main/java/org/trebol/product`

### Domain layer
- Folder: `src/main/java/org/trebol/product/domain/model`
  - `ProductAggregate.java`
  - `ProductId.java`
  - `ProductName.java`
  - `ProductPrice.java`
  - `ProductStatus.java`
- Folder: `src/main/java/org/trebol/product/domain/service`
  - `ProductDomainService.java`
- Folder: `src/main/java/org/trebol/product/domain/port`
  - `ProductRepository.java`
- Folder: `src/main/java/org/trebol/product/domain/exception`
  - `ProductValidationException.java`
  - `ProductNotFoundException.java`
- Folder: `src/main/java/org/trebol/product/domain/event`
  - `ProductCreatedEvent.java`
  - `ProductUpdatedEvent.java`

### Application layer
- Folder: `src/main/java/org/trebol/product/application/usecase/create`
  - `CreateProductUseCase.java`
  - `CreateProductCommand.java`
  - `CreateProductResult.java`
- Folder: `src/main/java/org/trebol/product/application/usecase/update`
  - `UpdateProductUseCase.java`
  - `UpdateProductCommand.java`
  - `UpdateProductResult.java`
- Folder: `src/main/java/org/trebol/product/application/usecase/get`
  - `GetProductUseCase.java`
  - `GetProductQuery.java`
  - `GetProductResult.java`
- Folder: `src/main/java/org/trebol/product/application/usecase/list`
  - `ListProductsUseCase.java`
  - `ListProductsQuery.java`
  - `ListProductsResult.java`
- Folder: `src/main/java/org/trebol/product/application/usecase/delete`
  - `DeleteProductUseCase.java`
  - `DeleteProductCommand.java`
- Folder: `src/main/java/org/trebol/product/application/service`
  - `ProductApplicationService.java`
- Folder: `src/main/java/org/trebol/product/application/mapper`
  - `ProductApplicationMapper.java`

### Adapters layer
- Inbound HTTP folder: `src/main/java/org/trebol/product/adapter/in/web`
  - `ProductController.java`
- Inbound DTO folder: `src/main/java/org/trebol/product/adapter/in/web/dto`
  - `ProductRequest.java`
  - `ProductResponse.java`
  - `ProductDetailResponse.java`
- Inbound mapper folder: `src/main/java/org/trebol/product/adapter/in/web/mapper`
  - `ProductWebMapper.java`
- Outbound persistence folder: `src/main/java/org/trebol/product/adapter/out/persistence/jpa`
  - `ProductJpaEntity.java`
  - `ProductJpaRepository.java`
  - `ProductRepositoryAdapter.java`
- Outbound mapper folder: `src/main/java/org/trebol/product/adapter/out/persistence/mapper`
  - `ProductPersistenceMapper.java`
- Outbound query folder: `src/main/java/org/trebol/product/adapter/out/persistence/query`
  - `ProductQuerySpecification.java`

### Infrastructure layer
- Folder: `src/main/java/org/trebol/product/infrastructure/config`
  - `ProductModuleConfiguration.java`
- Folder: `src/main/java/org/trebol/product/infrastructure/transaction`
  - `TransactionManagerAdapter.java`

### Tests
- Folder: `src/test/java/org/trebol/product/domain`
  - `ProductAggregateTest.java`
  - `ProductDomainServiceTest.java`
- Folder: `src/test/java/org/trebol/product/application`
  - `CreateProductUseCaseTest.java`
  - `UpdateProductUseCaseTest.java`
- Folder: `src/test/java/org/trebol/product/adapter/in/web`
  - `ProductControllerTest.java`
- Folder: `src/test/java/org/trebol/product/adapter/out/persistence`
  - `ProductRepositoryAdapterTest.java`

## 2. Responsibility Rules

1. Domain contains business rules only.
2. Application orchestrates use cases.
3. Adapters translate in and out.
4. Infrastructure wires framework concerns.

Dependency direction must stay inward:

- adapters -> application -> domain
- infrastructure -> application and domain
- domain -> no framework dependency

## 3. Bit-by-Bit Migration Strategy

Use a vertical-slice migration, not a big-bang package move.

Recommended order:

1. Get product.
2. List products.
3. Create product.
4. Update product.
5. Delete product.
6. Shared concerns: events, mapping, transaction boundaries.
7. Remove replaced legacy code.

Per-slice recipe:

1. Add use case contracts and models.
2. Implement use case using ports only.
3. Wire repository port implementation in adapter layer.
4. Map HTTP DTOs to application models.
5. Keep endpoint paths and payloads unchanged.
6. Add or update tests.
7. Merge only when tests are green.

## 4. What Freeze External Contract Means

Freeze external contract first means lock what clients observe before refactoring internals.

Contract includes:

1. HTTP paths and methods.
2. Query params and request body shape.
3. Response JSON shape.
4. Status codes.
5. Error code payload behavior.

During migration, internals may change, but external behavior must not change.

## 5. Current Contract Baseline

### Product API

Path: `GET /data/products`
- Returns HTTP 200.
- Returns paged shape: `items`, `pageIndex`, `totalCount`, `pageSize`.

Path: `POST /data/products`
- Valid body returns HTTP 201.

Path: `PUT /data/products`
- With filters returns HTTP 204.
- Without filters returns HTTP 400 and code `REJECTED_01`.

Path: `DELETE /data/products`
- With filters returns HTTP 204.

### Product categories API

Path: `GET /data/product_categories`
- Returns HTTP 200 with paged shape.

Path: `POST /data/product_categories`
- Valid body returns HTTP 201.

Path: `PUT /data/product_categories`
- With filters returns HTTP 204.
- Without filters returns HTTP 400 and code `REJECTED_01`.

Path: `DELETE /data/product_categories`
- With filters returns HTTP 204.

### Product list contents API

Path: `GET /data/product_list_contents?listCode=...`
- Returns HTTP 200 with paged shape.

Path: `POST /data/product_list_contents?listCode=...`
- Valid body returns HTTP 201.

Path: `PUT /data/product_list_contents?listCode=...`
- Valid body returns HTTP 204.

Path: `DELETE /data/product_list_contents?listCode=...`
- Valid request returns HTTP 204.

Path: `GET /data/product_list_contents` without `listCode`
- Returns HTTP 400 and code `REJECTED_01`.

### Error mapping reference

Defined by `src/main/java/org/trebol/api/ExceptionsControllerAdvice.java`:

- `EntityNotFoundException` -> 404, code `NOTFOUND_01`
- `EntityExistsException` -> 400, code `EXISTS_01`
- `BadInputException` -> 400, code `REJECTED_01`
- `MethodArgumentNotValidException` -> 400, code `REJECTED_02`

## 6. Contract Tests Added

These tests are now the migration guardrails:

1. `src/test/java/org/trebol/api/controllers/DataProductsControllerContractTest.java`
2. `src/test/java/org/trebol/api/controllers/DataProductCategoriesControllerContractTest.java`
3. `src/test/java/org/trebol/api/controllers/DataProductListContentsControllerContractTest.java`

Most recent targeted test run for the last two files: 12 passed, 0 failed.

## 7. Pull Request Rules For Safe Migration

1. One vertical slice per PR.
2. Keep external contract unchanged until explicitly versioned.
3. Keep database schema unchanged unless planned separately.
4. Merge only with green contract tests.
5. Remove legacy code only after replacement slice is proven.
