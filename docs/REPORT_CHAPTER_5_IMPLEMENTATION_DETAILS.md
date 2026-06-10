# Chapter 5: Concrete Implementation of the Product Bounded Context

This chapter describes how the target Product bounded context is implemented in production-grade Java code. It shows the request flow through the refactored module and explains how the Anti-Corruption Layer (ACL) preserves compatibility with legacy services during the transition.

## 5.1 Scope

The implementation focuses on two things: the new Clean Architecture request flow inside `org.trebol.product`, and the ACL bridge used by legacy services to read from the new module. The goal is to keep the domain core insulated while still supporting the existing monolithic application during migration.

## 5.2 Single-product read flow

The clearest example of the refactored module is the single-product read path.

The request flow is straightforward:

1. A client sends a `GET` request to the product controller.
2. The controller validates the input and maps it to an application query.
3. The application service coordinates the use case.
4. The repository port is called through the outbound persistence adapter.
5. The adapter queries the database and maps the result back to the domain model.
6. The controller returns the HTTP response.

This flow keeps transport, orchestration, and persistence separate. Each layer has a single responsibility, which makes the path easier to test and easier to change.

## 5.3 Filtering and pagination

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

## 5.4 Public API contracts

The modernized `ProductController` exposes read endpoints with stable request and response shapes.

### Single product

`GET /product-module/{id}` retrieves one product by identifier.

Response behavior:

- `200 OK` when the product exists,
- `404 Not Found` when the identifier is missing from the data store.

Example response:

```json
{
  "id": 1,
  "code": "PROD-1",
  "name": "Product Name",
  "price": 99.99,
  "isActive": true
}
```

### Paged list

`GET /product-module` returns a paged product listing with optional filters.

Supported query parameters:

- `pageIndex` for the zero-based page number,
- `pageSize` for the number of items per page,
- `code` for exact code matching,
- `nameLike` for partial case-insensitive name matching,
- `barcode` for legacy-compatible barcode lookup,
- `sortBy` for sorting by `id`, `name`, `barcode`, or `price`,
- `order` for ascending or descending sort direction.

Example response:

```json
{
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
```

These contracts preserve backward compatibility for clients that already depend on the product read API.

## 5.5 ACL bridge and coexistence

The ACL allows the legacy product services and the new Product bounded context to run in parallel. `ProductLookupService` defines the read contract used by the legacy code, and `ProductLookupAdapter` implements that contract by delegating reads into the new module.

This arrangement lets the new bounded context become the preferred source for reads without forcing an immediate rewrite of the legacy service layer. If the ACL path does not resolve a result, the legacy implementation can still fall back to its original repository-based read logic.

### Legacy read flow

The compatibility path works as follows:

1. The legacy CRUD layer calls `ProductLookupService`.
2. `ProductLookupAdapter` forwards the request into the Product application layer.
3. The Product bounded context resolves the data.
4. If needed, the legacy service falls back to the original repository path.

This bridge keeps the migration reversible and avoids forcing all consumers to move at once.

## 5.6 Dependency contrast

The refactored code follows inward dependencies. The legacy code followed the opposite pattern, where controllers and services depended directly on persistence and framework details.

The practical difference is visible in the module structure:

- the legacy path couples web, service, and persistence layers tightly,
- the new path keeps the domain core independent,
- the adapter layer handles framework translation,
- the application layer coordinates use cases without technical leakage.

This structure makes the Product bounded context easier to maintain, easier to test, and safer to evolve.
