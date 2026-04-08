# Product Domain Endpoint-Ordered Migration Checklist

Migrate one vertical slice (endpoint) at a time. Each slice includes all domain, application, adapter, and infrastructure classes needed for that endpoint.

Date: 2026-04-02

## How to use this checklist

1. Pick one endpoint slice below.
2. For each class row, move/rename and update imports only.
3. Keep behavior unchanged.
4. Run the corresponding contract test after the slice is done.
5. Commit the slice as one PR.
6. Move to next endpoint.

Suggested status markers:

- [ ] Not started
- [~] In progress
- [x] Done

---

## Slice 1: Get Product

**Endpoint**: `GET /data/products`  
**Contract Test**: `DataProductsControllerContractTest`  
**ReadMany**:  Implement logic to fetch paginated products

### Classes to move/rename

| Status | Current Class | Target Class |
|---|---|---|
| [ ] | domain/vo/ProductId.java | domain/model/ProductId.java |
| [ ] | domain/vo/ProductCode.java | domain/model/ProductCode.java |
| [ ] | domain/vo/ProductName.java | domain/model/ProductName.java |
| [ ] | domain/vo/ProductPrice.java | domain/model/ProductPrice.java |
| [ ] | domain/vo/ProductStatus.java | domain/model/ProductStatus.java |
| [ ] | domain/aggregate/ProductAggregate.java | domain/model/ProductAggregate.java |
| [ ] | domain/port/ProductRepository.java | domain/port/ProductRepository.java ✓ (no move) |
| [ ] | application/query/GetProductQuery.java | application/usecase/get/GetProductQuery.java |
| [ ] | application/result/ProductResult.java | application/usecase/get/GetProductResult.java |
| [ ] | application/usecase/GetProductUseCase.java | application/usecase/get/GetProductUseCase.java |
| [ ] | adapter/inbound/web/ProductController.java | adapter/in/web/ProductController.java |
| [ ] | adapter/inbound/web/ProductWebMapper.java | adapter/in/web/mapper/ProductWebMapper.java |
| [ ] | adapter/inbound/dto/ProductResponse.java | adapter/in/web/dto/ProductResponse.java |
| [ ] | adapter/outbound/persistence/ProductJpaEntity.java | adapter/out/persistence/jpa/ProductJpaEntity.java |
| [ ] | adapter/outbound/persistence/ProductJpaRepository.java | adapter/out/persistence/jpa/ProductJpaRepository.java |
| [ ] | adapter/outbound/persistence/ProductRepositoryAdapter.java | adapter/out/persistence/jpa/ProductRepositoryAdapter.java |
| [ ] | adapter/outbound/persistence/ProductPersistenceMapper.java | adapter/out/persistence/mapper/ProductPersistenceMapper.java |
| [ ] | infrastructure/ProductModuleConfiguration.java | infrastructure/config/ProductModuleConfiguration.java |

### Execution notes

- Implement GetProductUseCase to call repository port and return domain aggregate.
- Update ProductApplicationService to delegate to GetProductUseCase.
- Update ProductController readMany() to call use case.
- Wire in ProductModuleConfiguration.

### Verify

```bash
mvn -Dtest=DataProductsControllerContractTest#get_products_returns_ok_with_paged_shape test
```

---

## Slice 2: List Products

**Endpoint**: `GET /data/products` (with pagination)  
**Contract Test**: `DataProductsControllerContractTest`

### Classes to move/rename

| Status | Current Class | Target Class |
|---|---|---|
| [ ] | (already moved from Slice 1) | (domain/model package already in place) |
| [ ] | application/query/ListProductsQuery.java | application/usecase/list/ListProductsQuery.java |
| [ ] | application/result/PagedProductResult.java | application/usecase/list/ListProductsResult.java |
| [ ] | application/usecase/ListProductsUseCase.java | application/usecase/list/ListProductsUseCase.java |

### Execution notes

- ListProductsUseCase orchestrates pagination and filtering via repository port.
- Reuse existing adapter, mapper, and persistence classes from Slice 1.
- Update ProductApplicationService to implement ListProductsUseCase.
- Update ProductController readMany() if not done in Slice 1.

### Verify

```bash
mvn -Dtest=DataProductsControllerContractTest#get_products_returns_ok_with_paged_shape test
```

---

## Slice 3: Create Product

**Endpoint**: `POST /data/products`  
**Contract Test**: `DataProductsControllerContractTest#post_products_with_valid_body_returns_created`

### Classes to move/rename

| Status | Current Class | Target Class |
|---|---|---|
| [ ] | (already moved) | (domain/model package already in place) |
| [ ] | domain/service/ProductDomainService.java | domain/service/ProductDomainService.java ✓ (no move) |
| [ ] | domain/exception/ProductCodeAlreadyExistsException.java | domain/exception/ProductCodeAlreadyExistsException.java ✓ (or keep) |
| [ ] | domain/event/ProductCreatedEvent.java | domain/event/ProductCreatedEvent.java ✓ (no move) |
| [ ] | application/command/CreateProductCommand.java | application/usecase/create/CreateProductCommand.java |
| [ ] | application/result/ProductResult.java | application/usecase/create/CreateProductResult.java |
| [ ] | application/usecase/CreateProductUseCase.java | application/usecase/create/CreateProductUseCase.java |
| [ ] | adapter/inbound/dto/ProductRequest.java | adapter/in/web/dto/ProductRequest.java |

### Execution notes

- CreateProductUseCase validates input, creates ProductAggregate, delegates to repository port.
- Call ProductDomainService.ensureCodeAvailable() to validate uniqueness.
- Raise ProductCreatedEvent on success.
- Update ProductWebMapper to map ProductRequest → CreateProductCommand.
- Update ProductController create() to call use case.
- Update ProductApplicationService to implement CreateProductUseCase.

### Verify

```bash
mvn -Dtest=DataProductsControllerContractTest#post_products_with_valid_body_returns_created test
```

---

## Slice 4: Update Product

**Endpoint**: `PUT /data/products`  
**Contract Test**: `DataProductsControllerContractTest#put_products_with_filters_returns_no_content`

### Classes to move/rename

| Status | Current Class | Target Class |
|---|---|---|
| [ ] | (already moved) | (reuse existing model+command packages) |
| [ ] | application/command/UpdateProductCommand.java | application/usecase/update/UpdateProductCommand.java |
| [ ] | application/result/ProductResult.java | application/usecase/update/UpdateProductResult.java |
| [ ] | application/usecase/UpdateProductUseCase.java | application/usecase/update/UpdateProductUseCase.java |
| [ ] | domain/event/ProductUpdatedEvent.java | domain/event/ProductUpdatedEvent.java ✓ (no move) |

### Execution notes

- UpdateProductUseCase loads aggregate via repository, applies updates, persists.
- Raise ProductUpdatedEvent on success.
- Require filter parameters (query params) to identify product.
- Update ProductController update() and partialUpdate() to call use case.
- Update ProductApplicationService to implement UpdateProductUseCase.

### Verify

```bash
mvn -Dtest=DataProductsControllerContractTest#put_products_with_filters_returns_no_content test
```

---

## Slice 5: Delete Product

**Endpoint**: `DELETE /data/products`  
**Contract Test**: `DataProductsControllerContractTest#delete_products_with_filters_returns_no_content`

### Classes to move/rename

| Status | Current Class | Target Class |
|---|---|---|
| [ ] | (already moved) | (reuse existing model packages) |
| [ ] | application/command/DeleteProductCommand.java | application/usecase/delete/DeleteProductCommand.java |
| [ ] | application/usecase/DeleteProductUseCase.java | application/usecase/delete/DeleteProductUseCase.java |

### Execution notes

- DeleteProductUseCase calls repository port to remove product by id.
- Require filter parameters (query params) to identify product.
- Update ProductController delete() to call use case.
- Update ProductApplicationService to implement DeleteProductUseCase.

### Verify

```bash
mvn -Dtest=DataProductsControllerContractTest#delete_products_with_filters_returns_no_content test
```

---

## Slice 6: Shared Concerns (After all endpoints)

**Classes to move/rename**

| Status | Current Class | Target Class |
|---|---|---|
| [ ] | application/service/ProductApplicationService.java | application/service/ProductApplicationService.java ✓ (update only) |
| [ ] | application/service/ProductApplicationMapper.java | application/mapper/ProductApplicationMapper.java |
| [ ] | infrastructure/TransactionManagerAdapter.java | infrastructure/transaction/TransactionManagerAdapter.java |

### Execution notes

- Verify all use cases are now implemented and wired.
- Clean up old application/command, application/query, application/result directories.
- Run full test suite.

---

## Verification Commands

**Per-slice tests**:

```bash
mvn -Dtest=DataProductsControllerContractTest test
mvn -Dtest=DataProductCategoriesControllerContractTest test
mvn -Dtest=DataProductListContentsControllerContractTest test
```

**Full verification before final merge**:

```bash
mvn test
```

---

## Notes

- Each slice is treated as one feature branch and one PR.
- Behavior is preserved throughout migration.
- Once all vertical slices are done, you can remove legacy old directories.
- ProductResult can remain shared across use cases until you're confident to split.
- Import updates are mechanical; use IDE refactoring if available.
        