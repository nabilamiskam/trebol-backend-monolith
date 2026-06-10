# Product Domain — API Reference

Base path: `/product-module`

Common response object (ProductResponse)
- `id`: Long
- `code`: String (immutable identifier)
- `name`: String
- `price`: Decimal
- `isActive`: Boolean

Errors (common)
- `400 Bad Request` — validation or malformed input
- `401 Unauthorized` — missing/invalid authentication
- `404 Not Found` — resource or target set not found
- `409 Conflict` — duplicate `code` on create
- `500 Internal Server Error` — unexpected runtime error

1) Retrieve single product
- Method: GET
- Path: `/product-module/{id}`
- Auth: none (public)
- Example request:

  GET /product-module/195

- Success: `200 OK`
  ```json
  {
    "id": 195,
    "code": "PROD-195",
    "name": "Example product",
    "price": 49.99,
    "isActive": true
  }
  ```

2) List / search products (paged)
- Method: GET
- Path: `/product-module`
- Query params:
  - `pageIndex` (default 0)
  - `pageSize` (default 10)
  - Filtering params (any of): `id`, `code`, `barcode`, `name`, `price`, `barcodeLike`, `nameLike`
  - `sortBy` (optional, e.g. `name:asc` or `price:desc` depending on resolver)
- Example request:

  GET /product-module?pageIndex=0&pageSize=20&nameLike=shirt

- Success: `200 OK` (PagedProductResponse)
  ```json
  {
    "items": [ { /* ProductResponse objects */ } ],
    "totalCount": 154
  }
  ```

3) Create product
- Method: POST
- Path: `/product-module`
- Auth: requires authority `products:create`
- Request body (JSON):
  ```json
  {
    "code": "PROD-123",
    "name": "New Product",
    "price": 129.99,
    "isActive": true
  }
  ```
- Success: `201 Created` — Location header `/product-module/{id}` and body `ProductResponse`.
- Errors: `409 Conflict` if `code` already exists, `400 Bad Request` for invalid fields.

4) Update single product (replace/update full object by id)
- Method: PUT
- Path: `/product-module/{id}`
- Auth: requires authority `products:update`
- Request body (JSON): same shape as create; server applies provided fields and enforces invariants
  ```json
  {
    "name": "Updated name",
    "price": 139.50,
    "isActive": false
  }
  ```
- Success: `200 OK` with updated `ProductResponse`.
- Errors: `404 Not Found` if id missing; `400` for validation.

5) Bulk partial update (PATCH)
- Method: PATCH
- Path: `/product-module`
- Auth: requires authority `products:update`
- Semantics: Bulk update of products matching query filters; body contains only fields to change (partial map). The operation runs inside a single transaction; all target entities are updated and saved.
- Required: At least one query filter must be present to select targets (to avoid accidental full-table updates).
- Allowed patch fields (body keys): `name` (string), `price` (decimal), `isActive` (boolean)
- Forbidden in patch body: `code` (immutable) — sending `code` will produce `400 Bad Request`.
- Filtering params available (same as list): `id`, `code`, `barcode`, `name`, `price`, `barcodeLike`, `nameLike`.

- Example request (single-filter):
  ```http
  PATCH /product-module?code=PROD-1
  Content-Type: application/json

  { "name": "New batch name", "price": 99.95 }
  ```

- Example request (multi-filter):
  ```http
  PATCH /product-module?nameLike=shirt&price=19.99
  Content-Type: application/json

  { "isActive": true }
  ```

- Responses:
  - `200 OK` — body `BulkPatchProductResponse`:
    ```json
    {
      "items": [ /* updated ProductResponse objects */ ],
      "updatedCount": 12
    }
    ```
  - `400 Bad Request` — if no query filter provided, if body empty, if body contains unsupported fields, or if types are invalid.
  - `404 Not Found` — if filters produce no targets (implementation throws `ProductNotFoundException`).

6) Delete single product
- Method: DELETE
- Path: `/product-module/{id}`
- Auth: requires authority `products:delete`
- Success: `204 No Content`
- Error: `404 Not Found` if id missing.

Notes on filters and types
- `id`: numeric (Long)
- `price`: numeric (decimal); filters compare equality
- `nameLike`, `barcodeLike`: case-insensitive substring match

Security / Auth
- Create/Update/Delete endpoints are protected by authorities:
  - `products:create` → POST
  - `products:update` → PUT, PATCH
  - `products:delete` → DELETE
- GET endpoints are public in the current controller implementation.

Coupling points with other domains
----------------------------------
- Security: product mutations depend on the shared security configuration and authority names such as `products:create`, `products:update`, and `products:delete`.
- Common infrastructure: product controllers reuse shared error handling and cross-cutting utilities rather than exposing persistence details directly.
- JPA / persistence: product queries and bulk patching depend on the shared Spring Data JPA stack, Specifications, and the database schema.
- Domain boundaries: the product module should only depend on other domains when there is a real business rule or data contract, not just because they live in the same repository.
- Shared identifiers and contracts: product `id` and `code` may be consumed by other domains, so those fields are the most common integration touchpoints.
- Coupling rule of thumb: inbound adapters should talk to the product use cases, not directly to repositories or other domains; outbound adapters should isolate infrastructure so changes stay local.

Recommended examples (curl)
---------------------------
- Patch by code (with bearer token):

```bash
curl -X PATCH 'http://localhost:8080/product-module?code=PROD-1' \
  -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Clearance - 50%", "price": 19.99}'
```

- Create new product:

```bash
curl -X POST 'http://localhost:8080/product-module' \
  -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"code":"PROD-345","name":"Widget","price":29.9,"isActive":true}'
```

Where to find implementation
- Controller: `src/main/java/org/trebol/product/adapter/inbound/web/ProductController.java`
- Commands: `src/main/java/org/trebol/product/application/command/` *(e.g., BulkPatchProductCommand.java)*
- Service implementation (use cases): `src/main/java/org/trebol/product/application/service/ProductApplicationService.java`
- Repository port and adapter: `src/main/java/org/trebol/product/domain/port/ProductRepository.java` and `src/main/java/org/trebol/product/adapter/outbound/persistence/ProductRepositoryAdapter.java`

If you want, I can now:
- Add a Thunder Client collection file with these examples.
- Create an integration test that boots the app with an in-memory DB and validates the PATCH flow end-to-end.
