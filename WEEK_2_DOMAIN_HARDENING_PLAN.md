# Week 2: Domain Hardening

## Objective
Finish the pure domain model around the aggregate, value objects, and domain exceptions, and ensure the domain layer has no Spring or JPA concerns.

### Exit Criteria
- The product domain can be unit tested with no framework bootstrapping
- Invariants live in the domain instead of the web or persistence layers
- Persistence mapping preserves all domain state exactly
- All domain tests pass without Spring context

---

## Current Gaps To Fix First

1. **Domain tests are placeholders**
   - `src/test/java/org/trebol/product/domain/aggregate/ProductAggregateTest.java`
   - `src/test/java/org/trebol/product/domain/service/ProductDomainServiceTest.java`

2. **Aggregate status is not persisted**
   - `ProductPersistenceMapper` does not map status
   - `ProductJpaEntity` has no status field
   - Status can be lost on round-trip

3. **Domain behavior is thin**
   - `ProductAggregate` only has passive setters
   - Invariants are not clearly centralized
   - No explicit behavior methods

4. **Application flows incomplete**
   - Create/update/delete not implemented in `ProductApplicationService`
   - Blocks proving invariant flow end-to-end

---

## Step-by-Step Execution

### Step 1: Define Invariant Catalog for Product

**Action:** Write a short list of domain invariants in a note or document.

**Invariants to enforce:**
- Name is non-empty (max 255 chars)
- Code is non-empty and unique
- Price >= 0
- ID > 0
- Status transitions are valid (ACTIVE ↔ INACTIVE)
- Code uniqueness must be checked at domain service level

**Ownership mapping:**
| Invariant | Owner | Location |
|-----------|-------|----------|
| Name non-empty, max 255 chars | Value Object | `ProductName` |
| Code non-empty | Value Object | `ProductCode` |
| Price >= 0 | Value Object | `ProductPrice` |
| ID > 0 | Value Object | `ProductId` |
| Status lifecycle | Aggregate | `ProductAggregate` |
| Code uniqueness | Domain Service | `ProductDomainService` |

---

### Step 2: Harden Value Objects (First Guardrail)

**Target files:**
- `src/main/java/org/trebol/product/domain/vo/ProductCode.java`
- `src/main/java/org/trebol/product/domain/vo/ProductName.java`
- `src/main/java/org/trebol/product/domain/vo/ProductPrice.java`
- `src/main/java/org/trebol/product/domain/vo/ProductId.java`

**Actions:**
1. ✓ Review and keep constraints in all value objects (already implemented)
2. Consider replacing generic `IllegalArgumentException` with domain-specific exceptions where stable error semantics are needed:
   - Enhanced `ProductValidationException` or subtypes for clarity
   - Optional: keep `IllegalArgumentException` for constructor validation (acceptable pattern)
3. Verify no framework/JPA annotations in value objects (currently clean)
4. Keep all value objects immutable (already records)

**Example pattern (optional enhancement):**
```java
public record ProductName(String value) {
    public ProductName {
        Objects.requireNonNull(value, "Product name cannot be null");
        if (value.isBlank()) {
            throw new ProductValidationException("Product name cannot be blank");
        }
        if (value.length() > 255) {
            throw new ProductValidationException("Product name cannot exceed 255 characters");
        }
    }
}
```

---

### Step 3: Move Business Behavior Into Aggregate

**Target file:** `src/main/java/org/trebol/product/domain/aggregate/ProductAggregate.java`

**Actions:**
1. Replace passive setters with explicit behavior methods:
   - `rename(ProductName newName)` → validates and updates name
   - `reprice(ProductPrice newPrice)` → validates and updates price
   - `activate()` → transitions to ACTIVE (idempotent)
   - `deactivate()` → transitions to INACTIVE (idempotent)

2. Add factory method for creation:
   - `static ProductAggregate create(ProductId id, ProductCode code, ProductName name, ProductPrice price)`
   - Ensures all invariants are met at creation time

3. Enforce transition invariants:
   - Document allowed state transitions
   - Example: prevent invalid operations on inactive products (if business rule)

4. Keep constructor invariants strict:
   - Avoid silent defaults like `status = ProductStatus.ACTIVE`
   - Require explicit status on hydration from persistence

**Example behavior method:**
```java
public void rename(ProductName newName) {
    this.name = Objects.requireNonNull(newName, "Name cannot be null");
}

public void activate() {
    this.status = ProductStatus.ACTIVE;
}

public void deactivate() {
    this.status = ProductStatus.INACTIVE;
}

public static ProductAggregate create(ProductId id, ProductCode code, ProductName name, ProductPrice price) {
    return new ProductAggregate(id, code, name, price);
}
```

---

### Step 4: Fix Persistence Mapping (State Preservation)

**Target files:**
- `src/main/java/org/trebol/product/adapter/outbound/persistence/ProductJpaEntity.java`
- `src/main/java/org/trebol/product/adapter/outbound/persistence/ProductPersistenceMapper.java`

**Actions:**
1. Add status storage to `ProductJpaEntity`:
   - Add field: `private String status;` (or enum, or boolean - choose one pattern)
   - Add getter/setter
   - Add `@Column` annotation if needed

2. Update `ProductPersistenceMapper`:
   - Map status **both directions** (to entity AND from entity)
   - Example: `entity.setStatus(aggregate.getStatus().toString());`
   - Example: `fromBoolean(entity.getStatus())`

3. **Critical:** Stop relying on aggregate constructor defaulting status to ACTIVE
   - All status must come from persistence or be explicitly set
   - Constructor should accept status as parameter OR aggregate should be reconstructed with status

4. Result:
   - Persistence adapter becomes translation only
   - No business decisions in mapper
   - Domain state round-trips exactly

**Example mapper change:**
```java
public ProductJpaEntity toEntity(ProductAggregate aggregate) {
    ProductJpaEntity entity = new ProductJpaEntity();
    entity.setId(aggregate.getId().value());
    entity.setCode(aggregate.getCode().value());
    entity.setName(aggregate.getName().value());
    entity.setPrice(aggregate.getPrice().value().intValue());
    entity.setStatus(aggregate.getStatus().asBoolean()); // ADD THIS
    return entity;
}

public ProductAggregate toAggregate(ProductJpaEntity entity) {
    ProductAggregate aggregate = new ProductAggregate(
        new ProductId(entity.getId()),
        new ProductCode(entity.getCode()),
        new ProductName(entity.getName()),
        new ProductPrice(BigDecimal.valueOf(entity.getPrice()))
    );
    // Restore status from persistence
    if (entity.getStatus() != null) {
        aggregate.updateStatus(ProductStatus.fromBoolean(entity.getStatus()));
    }
    return aggregate;
}
```

---

### Step 5: Keep Application Layer Orchestration-Only

**Target file:** `src/main/java/org/trebol/product/application/service/ProductApplicationService.java`

**Actions:**
1. Implement `CreateProductUseCase.execute(CreateProductCommand)`:
   - Build value objects from command data (catches VO validation errors)
   - Call `domainService.ensureCodeAvailable(code)` (catches uniqueness)
   - Call aggregate factory: `ProductAggregate.create(...)`
   - Persist via `productRepository.save(aggregate)`
   - Publish or record `ProductCreatedEvent`
   - Return mapped result

2. Implement `UpdateProductUseCase.execute(UpdateProductCommand)`:
   - Load aggregate from `productRepository.findById(...)`
   - Build VOs from command data
   - Call aggregate behavior: `aggregate.rename(...)`, `aggregate.reprice(...)`
   - Persist via `productRepository.save(aggregate)`
   - Publish `ProductUpdatedEvent`
   - Return mapped result

3. Implement `DeleteProductUseCase.execute(DeleteProductCommand)`:
   - Load aggregate
   - Call `productRepository.deleteById(...)`
   - Optionally publish `ProductDeletedEvent`

4. **Key rule:** Do not place business validation in application service
   - Only translate command to domain objects
   - Let domain objects throw their own exceptions
   - Catch and convert to application/HTTP results if needed

**Example implementation pattern:**
```java
@Override
public ProductResult execute(CreateProductCommand command) {
    try {
        ProductCode code = new ProductCode(command.code());
        ProductName name = new ProductName(command.name());
        ProductPrice price = new ProductPrice(BigDecimal.valueOf(command.price()));
        
        // Domain service checks uniqueness
        domainService.ensureCodeAvailable(code);
        
        // Aggregate factory
        ProductAggregate aggregate = ProductAggregate.create(
            null, // TBD: how to generate ID
            code, name, price
        );
        
        // Persist
        ProductAggregate saved = productRepository.save(aggregate);
        
        return mapper.toResult(saved);
    } catch (ProductValidationException e) {
        // Handle domain error
        throw e;
    }
}
```

---

### Step 6: Keep Web Adapter DTO Mapping-Only

**Target files:**
- `src/main/java/org/trebol/product/adapter/inbound/web/ProductWebMapper.java`
- `src/main/java/org/trebol/product/adapter/inbound/web/ProductController.java`

**Actions:**
1. Keep `ProductWebMapper` focused on DTO ↔ command/result translation
2. Do **not** duplicate domain invariants in DTOs (no `@NotNull`, `@NotBlank` validation on transfer objects is OK, but keep domain rules in domain)
3. Controller should:
   - Parse request
   - Call application service
   - Translate result/exception to HTTP response
   - NOT check business rules
4. Example: if a domain exception is thrown, controller translates to 400/409 HTTP status

---

### Step 7: Strengthen Domain Exception Model

**Target files:**
- `src/main/java/org/trebol/product/domain/exception/ProductNotFoundException.java`
- `src/main/java/org/trebol/product/domain/exception/ProductCodeAlreadyExistsException.java`
- `src/main/java/org/trebol/product/domain/exception/ProductValidationException.java`

**Actions:**
1. Keep these as domain language (ubiquitous language)
2. Extend/refine exception hierarchy if needed:
   - `ProductDomainException` (base)
     - `ProductInvalidException` (invariant violation)
     - `ProductNotFoundException` (not found)
     - `ProductCodeAlreadyExistsException` (uniqueness)
3. Application/web layers translate domain exceptions to HTTP/API responses
4. **Key:** domain exceptions should **not** be caught in domain layer itself
   - They are part of the domain's public contract

---

### Step 8: Write Pure Unit Tests for Domain Only (No Framework)

**Target test files:**
- `src/test/java/org/trebol/product/domain/aggregate/ProductAggregateTest.java`
- `src/test/java/org/trebol/product/domain/service/ProductDomainServiceTest.java`

**Actions:**

#### Test ProductAggregate
```java
class ProductAggregateTest {
    
    @Test
    void shouldCreateAggregateWithValidData() {
        ProductId id = new ProductId(1L);
        ProductCode code = new ProductCode("SKU001");
        ProductName name = new ProductName("Test Product");
        ProductPrice price = new ProductPrice(BigDecimal.valueOf(99.99));
        
        ProductAggregate aggregate = ProductAggregate.create(id, code, name, price);
        
        assertThat(aggregate.getId()).isEqualTo(id);
        assertThat(aggregate.getCode()).isEqualTo(code);
        assertThat(aggregate.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }
    
    @Test
    void shouldRenamProduct() {
        ProductAggregate aggregate = createTestAggregate();
        ProductName newName = new ProductName("Updated Name");
        
        aggregate.rename(newName);
        
        assertThat(aggregate.getName()).isEqualTo(newName);
    }
    
    @Test
    void shouldThrowWhenRenamingWithNull() {
        ProductAggregate aggregate = createTestAggregate();
        
        assertThatThrownBy(() -> aggregate.rename(null))
            .isInstanceOf(NullPointerException.class);
    }
    
    @Test
    void shouldActivateProduct() {
        ProductAggregate aggregate = createTestAggregate();
        aggregate.deactivate();
        
        aggregate.activate();
        
        assertThat(aggregate.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }
    
    @Test
    void shouldDeactivateProduct() {
        ProductAggregate aggregate = createTestAggregate();
        
        aggregate.deactivate();
        
        assertThat(aggregate.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }
    
    private ProductAggregate createTestAggregate() {
        return ProductAggregate.create(
            new ProductId(1L),
            new ProductCode("TEST"),
            new ProductName("Test"),
            new ProductPrice(BigDecimal.TEN)
        );
    }
}
```

#### Test Value Objects
```java
class ProductNameTest {
    
    @Test
    void shouldCreateWithValidName() {
        ProductName name = new ProductName("Valid Name");
        assertThat(name.value()).isEqualTo("Valid Name");
    }
    
    @Test
    void shouldThrowWhenNameIsBlank() {
        assertThatThrownBy(() -> new ProductName("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be blank");
    }
    
    @Test
    void shouldThrowWhenNameExceeds255Chars() {
        String longName = "a".repeat(256);
        assertThatThrownBy(() -> new ProductName(longName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("255");
    }
}
```

#### Test Domain Service
```java
class ProductDomainServiceTest {
    
    private ProductDomainService service;
    private InMemoryProductRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
        service = new ProductDomainService(repository);
    }
    
    @Test
    void shouldAllowNewCode() {
        ProductCode code = new ProductCode("NEWCODE");
        
        assertThatCode(() -> service.ensureCodeAvailable(code))
            .doesNotThrowAnyException();
    }
    
    @Test
    void shouldRejectExistingCode() {
        ProductCode code = new ProductCode("EXISTING");
        repository.addExisting(code);
        
        assertThatThrownBy(() -> service.ensureCodeAvailable(code))
            .isInstanceOf(ProductCodeAlreadyExistsException.class);
    }
}

// Test double (in-memory, no mocking framework)
class InMemoryProductRepository implements ProductRepository {
    private final Set<String> codes = new HashSet<>();
    
    void addExisting(ProductCode code) {
        codes.add(code.value());
    }
    
    @Override
    public Optional<ProductAggregate> findByCode(ProductCode code) {
        return codes.contains(code.value()) 
            ? Optional.of(mock(ProductAggregate.class)) 
            : Optional.empty();
    }
    
    // Other methods...
}
```

**Test Checklist:**
- [ ] All value objects tested for valid and invalid construction
- [ ] Aggregate state transitions tested
- [ ] Domain service uniqueness check tested
- [ ] **No Spring annotations in tests**
- [ ] **No `@SpringBootTest` or context loading**
- [ ] **Use test doubles or simple mocks only if needed**

---

### Step 9: Add Architectural Guard Against Framework Leakage

**Action:** Create an architecture test to prevent Spring/JPA imports in domain layer.

**Implementation option 1 (ArchUnit):**
```java
class DomainArchitectureTest {
    
    private static final JavaClasses DOMAIN_CLASSES = 
        new ClassFileImporter()
            .importPackages("org.trebol.product.domain");
    
    @Test
    void domainShouldNotDependOnSpring() {
        noClasses()
            .that().resideInAnyPackage("org.trebol.product.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .check(DOMAIN_CLASSES);
    }
    
    @Test
    void domainShouldNotDependOnJpa() {
        noClasses()
            .that().resideInAnyPackage("org.trebol.product.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "javax.persistence..")
            .check(DOMAIN_CLASSES);
    }
}
```

**Implementation option 2 (simple check):**
- Use IDE inspection or `grep` to verify no `@Entity`, `@Autowired`, `@Service` annotations in domain layer

---

### Step 10: Verify Exit Criteria Explicitly

**Checklist:**

- [ ] **1. Domain tests run without Spring context**
  ```bash
  mvn test -Dtest=ProductAggregateTest,ProductDomainServiceTest
  ```
  - Should complete in < 2 seconds
  - No Spring context warnings

- [ ] **2. Full product module tests pass**
  ```bash
  mvn test -Dtest=ProductDomainServiceTest,ProductAggregateTest,ProductApplicationServiceTest
  ```

- [ ] **3. Invariants are enforced in domain layer**
  - [ ] `ProductCode` rejects blank codes
  - [ ] `ProductName` rejects blank, long names
  - [ ] `ProductPrice` rejects negative prices
  - [ ] `ProductId` rejects non-positive IDs
  - [ ] `ProductAggregate` enforces state transitions
  - [ ] `ProductDomainService` rejects duplicate codes

- [ ] **4. Persistence preserves all state**
  - [ ] Test: create aggregate → save → load → assert all fields match
  - [ ] Including status

- [ ] **5. No framework in domain**
  - [ ] Run ArchUnit test (or grep)
  - [ ] Zero Spring/JPA imports in `org.trebol.product.domain`

- [ ] **6. Application layer is orchestration-only**
  - [ ] Create/update/delete use cases delegate to domain
  - [ ] No duplicate business logic in application service

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Domain unit tests (no framework) | 20+ tests, all < 100ms each |
| Domain test coverage | ≥ 80% |
| Invariant locations | 1 owner per invariant (Value Object or Aggregate or Domain Service) |
| Persistence round-trip | 100% state preservation |
| Framework dependencies in domain | 0 (verified by ArchUnit) |
| Application service business logic | 0% (pure orchestration) |

---

## Implementation Order (Recommended)

**High-impact first:**
1. Step 3: Add behavior methods to aggregate
2. Step 4: Fix persistence mapping + status storage
3. Step 8: Write domain unit tests
4. Step 9: Add architecture guard
5. Step 10: Verify exit criteria

**Then polish:**
6. Step 2: Refine VO exceptions (optional)
7. Step 5: Implement use cases
8. Step 6: Finalize controller/mapper

---

## Files Modified

- `ProductAggregate.java` — add behavior methods, factory
- `ProductJpaEntity.java` — add status field
- `ProductPersistenceMapper.java` — map status both directions
- `ProductAggregateTest.java` — implement comprehensive tests
- `ProductDomainServiceTest.java` — implement uniqueness tests
- `ProductApplicationService.java` — implement create/update/delete
- `ProductValidationException.java` (optional) — add subtypes
- New: `DomainArchitectureTest.java` — ArchUnit rules

---

## Notes

- Keep domain objects **immutable** where possible (use records for VOs, careful with aggregate mutators)
- Domain exceptions are **part of the contract**; don't catch them in domain
- Application layer translates domain exceptions to HTTP/API results
- Tests should **never** bootstrap Spring; use test doubles instead
- Run domain tests in isolation frequently to catch framework leakage early
