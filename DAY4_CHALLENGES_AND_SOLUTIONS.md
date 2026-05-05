# Day 4: UPDATE & DELETE Endpoints - Challenges & Solutions

## Challenge 1: Partial Updates (Nullable Command Fields)

**Problem:** The PUT endpoint needs to support partial updates where clients can modify only specific fields (name, price, isActive) while leaving others unchanged.

**Solution:** 
- UpdateProductCommand fields are nullable: `String name`, `BigDecimal price`, `boolean isActive`
- Application service checks each field: `if (command.name() != null) { product.updateName(...) }`
- Only provided fields are mutated; absent fields retain existing values
- Product aggregate getters return current state before mutations

**Validation Concern:** The UpdateProductCommand compact constructor validates:
- id must be positive (never null)
- name if provided must be non-blank
- price if provided must be non-negative
- This allows any combination: update name only, price only, status only, or combinations

---

## Challenge 2: Ensuring Business Rules Stay in Domain

**Problem:** Where should business logic live? Repository layer? Service layer? Domain layer?

**Solution:** 
- **Repository adapter** (outbound): Only persists aggregates; no business logic
- **Application service** (orchestrator): Checks existence (ProductNotFoundException), applies mutations
- **Domain aggregate** (ProductAggregate): Pure business logic, immutability enforced via updateXXX() methods
- **Value objects**: Validation happens at construction (ProductName, ProductPrice, ProductStatus)

**Example:** Code uniqueness check for CREATE
- ❌ NOT in adapter (persistence-specific)
- ✅ IN application service (orchestration logic: check → create → save)
- Domain doesn't need to check uniqueness; that's a use-case rule, not a domain invariant

---

## Challenge 3: Transaction Safety (Read-Mutate-Persist)

**Problem:** Between checking existence and saving, could the product be deleted by another request?

**Solution & Pattern:**
```
1. LOAD: productRepository.findById(id) → Optional<aggregate>
        └─ Check exists: .orElseThrow(ProductNotFoundException)
        
2. MUTATE: product.updateName(...), product.updatePrice(...)
           └─ In-memory, no DB calls
           
3. PERSIST: productRepository.save(product)
            └─ JPA handles UPDATE statement
            └─ If product was deleted by concurrent request, this would fail
```

**Real-world transaction guarantee:**
- Spring @Transactional wraps execute() method
- Database transaction ensures save succeeds or fails atomically
- If product was deleted, Hibernate would either:
  - Update 0 rows (silent), or
  - Throw OptimisticLockException (if @Version field exists)
- **Note:** Current implementation doesn't handle concurrent deletes; could add @Version for optimistic locking

---

## Challenge 4: Void Methods in MockMvc Tests

**Problem:** `execute(DeleteProductCommand)` returns void, so `when().thenThrow()` doesn't work with Mockito

**Solution:**
```java
// ❌ WRONG - void returns nothing
when(service.execute(anyDeleteCommand))
    .thenThrow(exception);

// ✅ CORRECT - use doThrow() for void methods
doThrow(new ProductNotFoundException(...))
    .when(service).execute(any(DeleteProductCommand.class));

// ✅ Also acceptable for success case
doNothing().when(service).execute(any(DeleteProductCommand.class));
```

---

## Challenge 5: HTTP Semantics for DELETE

**Problem:** What HTTP status should DELETE return?

**Solution:**
- **204 No Content** (preferred): Resource deleted, no response body to send
  - Client receives empty body
  - Controller returns `ResponseEntity.noContent().build()`
- **200 OK** with deleted entity: More data but unnecessary
- **404 Not Found**: If product doesn't exist before deletion

---

## Test Coverage for Day 4

**UPDATE Tests (3):**
1. ✅ shouldUpdateProductSuccessfully() - Updates name/price/status, returns 200 OK
2. shouldReturn404OnUpdateWhenProductNotFound() - Tries to update missing product, returns 404
3. (Future) shouldReturn400OnInvalidUpdateData() - Invalid price range, returns 400

**DELETE Tests (2):**
1. shouldDeleteProductSuccessfully() - Deletes valid product, returns 204 No Content
2. shouldReturn404OnDeleteWhenProductNotFound() - Tries to delete missing product, returns 404

---

## Architecture Pattern Summary (All CRUD Operations)

**Request Flow for UPDATE/DELETE:**
```
HTTP Request (PUT /product-module/{id} or DELETE /product-module/{id})
  ↓
ProductController.updateProduct() / deleteProduct()
  ↓ (catch exceptions, map responses)
ProductWebMapper.toUpdateCommand() / toDeleteCommand()
  ↓ (convert HTTP DTOs to domain commands)
ProductApplicationService.execute(UpdateProductCommand/DeleteProductCommand)
  ↓ (orchestrate use case)
[1. Load] ProductRepository.findById(id) → ProductAggregate or throw
[2. Mutate] aggregate.updateXXX() for UPDATE
[3. Persist] ProductRepository.save(aggregate)
  ↓ (delegate to persistence)
ProductRepositoryAdapter (JPA implementation)
  ↓ (domain→entity conversion)
ProductPersistenceMapper / ProductJpaEntity
  ↓ (ORM magic)
MariaDB UPDATE/DELETE statement
  ↓
HTTP Response (200 OK or 204 No Content, or 404 Not Found)
```

**Key Principle:** Each layer owns its responsibility
- Controller: HTTP concerns (status codes, headers)
- Mapper: Data transformation (DTO ↔ Domain)
- Service: Use case orchestration (rules, exceptions)
- Repository: Persistence (ORM, queries)
- Aggregate: Domain logic (mutations, invariants)
