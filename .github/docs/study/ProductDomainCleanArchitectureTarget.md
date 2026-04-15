# Product Domain Clean Architecture Target

This document captures the proposed target structure for the Product domain refactor.

## Goals

- Separate domain, application, adapters, and infrastructure concerns.
- Make each endpoint slice explicit and testable.
- Keep repository access behind a port/adapter boundary.
- Preserve the external HTTP contract while refactoring internally.

## Proposed Package Structure

```text
src/main/java/org/trebol/product/
├── domain/
│   ├── model/
│   │   ├── ProductAggregate.java
│   │   ├── ProductId.java
│   │   ├── ProductName.java
│   │   ├── ProductPrice.java
│   │   └── ProductStatus.java
│   ├── service/
│   │   └── ProductDomainService.java
│   ├── port/
│   │   └── ProductRepository.java
│   ├── exception/
│   │   ├── ProductValidationException.java
│   │   └── ProductNotFoundException.java
│   └── event/
│       ├── ProductCreatedEvent.java
│       └── ProductUpdatedEvent.java
├── application/
│   ├── usecase/
│   │   ├── create/
│   │   │   ├── CreateProductUseCase.java
│   │   │   ├── CreateProductCommand.java
│   │   │   └── CreateProductResult.java
│   │   ├── update/
│   │   │   ├── UpdateProductUseCase.java
│   │   │   ├── UpdateProductCommand.java
│   │   │   └── UpdateProductResult.java
│   │   ├── get/
│   │   │   ├── GetProductUseCase.java
│   │   │   ├── GetProductQuery.java
│   │   │   └── GetProductResult.java
│   │   ├── list/
│   │   │   ├── ListProductsUseCase.java
│   │   │   ├── ListProductsQuery.java
│   │   │   └── ListProductsResult.java
│   │   └── delete/
│   │       ├── DeleteProductUseCase.java
│   │       └── DeleteProductCommand.java
│   ├── service/
│   │   └── ProductApplicationService.java
│   └── mapper/
│       └── ProductApplicationMapper.java
├── adapter/
│   ├── in/
│   │   └── web/
│   │       ├── ProductController.java
│   │       ├── dto/
│   │       │   ├── ProductRequest.java
│   │       │   ├── ProductResponse.java
│   │       │   └── ProductDetailResponse.java
│   │       └── mapper/
│   │           └── ProductWebMapper.java
│   └── out/
│       └── persistence/
│           ├── jpa/
│           │   ├── ProductJpaEntity.java
│           │   ├── ProductJpaRepository.java
│           │   └── ProductRepositoryAdapter.java
│           ├── mapper/
│           │   └── ProductPersistenceMapper.java
│           └── query/
│               └── ProductQuerySpecification.java
└── infrastructure/
    ├── config/
    │   └── ProductModuleConfiguration.java
    └── transaction/
        └── TransactionManagerAdapter.java
```

## Recommended Slice Order

1. GET one product.
2. GET list products.
3. POST create product.
4. PUT/PATCH update product.
5. DELETE product.

## Keep, Move, or Defer

### Keep for now

- `ProductRepository.java` in `domain/port`.
- `ProductApplicationService.java` until all use cases are split and stable.
- `ProductQuerySpecification.java` in the outbound persistence adapter if filtering remains JPA-specific.

### Move now

- Domain value objects and aggregate into `domain/model`.
- Use case interfaces and request/result DTOs into `application/usecase/*`.
- HTTP controller and web DTOs into `adapter/in/web/*`.
- JPA entity, JPA repository, adapter, and persistence mapper into `adapter/out/persistence/*`.
- Module wiring into `infrastructure/config`.

### Defer unless needed

- Domain events, until event publication is real.
- `ProductNotFoundException`, if the application layer is the better home for that error in your implementation.

## Read Path Flow

```text
HTTP GET /data/products
  -> ProductController
  -> ListProductsQuery
  -> ListProductsUseCase
  -> ProductApplicationService
  -> ProductRepository (port)
  -> ProductRepositoryAdapter
  -> ProductJpaRepository / Specification
  -> ProductJpaEntity
  -> PagedProductResult
  -> ProductResponse / DataPagePojo
```

## Notes

- Keep controller endpoints stable while refactoring internals.
- Prefer one slice at a time so tests can prove behavior before the next move.
- If a class becomes a catch-all, split it before the next slice.
