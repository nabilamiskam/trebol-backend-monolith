# Reengineering Strategy & Testing Plan
## Clean Architecture Refactoring of Trébol Backend

---

## TABLE OF CONTENTS

1. [Part 1: Package & Class Structure](#part-1-package--class-structure)
2. [Part 2: Class Separation & Merging Rules](#part-2-class-separation--merging-rules)
3. [Part 3: Refactoring Prioritization & Sequencing](#part-3-refactoring-prioritization--sequencing)
4. [Part 4: Testing Strategy](#part-4-testing-strategy)
5. [Part 5: Test Transition Plan](#part-5-test-transition-plan)
6. [Part 6: Quality Assurance During Refactoring](#part-6-quality-assurance-during-refactoring)

---

# PART 1: PACKAGE & CLASS STRUCTURE

## 1.1 Clean Architecture Package Organization

### Directory Tree

```
src/main/java/org/trebol/
│
├── product/                          ◄── DOMAIN MODULE (Clean Architecture)
│   ├── domain/
│   │   ├── aggregate/
│   │   │   ├── ProductAggregate.java
│   │   │   └── ProductAggregateFactory.java
│   │   │
│   │   ├── vo/                       ◄── Value Objects (Business Rules)
│   │   │   ├── ProductId.java
│   │   │   ├── ProductCode.java
│   │   │   ├── ProductName.java
│   │   │   ├── ProductPrice.java
│   │   │   ├── ProductStatus.java
│   │   │   └── ...
│   │   │
│   │   ├── exception/                ◄── Domain Exceptions
│   │   │   ├── InvalidProductException.java
│   │   │   ├── DuplicateProductException.java
│   │   │   ├── ProductNotFoundException.java
│   │   │   └── ...
│   │   │
│   │   └── port/                     ◄── Repository Port (Interface Only)
│   │       └── ProductRepository.java    (NO IMPLEMENTATION)
│   │
│   ├── application/
│   │   ├── query/                    ◄── Query Objects (Use Case Input)
│   │   │   ├── ListProductsQuery.java
│   │   │   ├── GetProductQuery.java
│   │   │   ├── SearchProductsQuery.java
│   │   │   └── ...
│   │   │
│   │   ├── result/                   ◄── Result Objects (Use Case Output)
│   │   │   ├── ProductResult.java
│   │   │   ├── PagedProductResult.java
│   │   │   └── ...
│   │   │
│   │   ├── use_case/                 ◄── Use Case Implementations
│   │   │   ├── ListProductsUseCase.java (interface)
│   │   │   ├── ListProductsUseCaseImpl.java (implementation)
│   │   │   ├── GetProductUseCase.java
│   │   │   ├── GetProductUseCaseImpl.java
│   │   │   ├── CreateProductUseCase.java
│   │   │   ├── CreateProductUseCaseImpl.java
│   │   │   ├── UpdateProductUseCase.java
│   │   │   ├── UpdateProductUseCaseImpl.java
│   │   │   ├── DeleteProductUseCase.java
│   │   │   ├── DeleteProductUseCaseImpl.java
│   │   │   └── ...
│   │   │
│   │   ├── service/                  ◄── Application Services (Orchestration)
│   │   │   ├── ProductApplicationService.java
│   │   │   └── ProductApplicationMapper.java
│   │   │
│   │   └── dto/                      ◄── Data Transfer Objects
│   │       ├── CreateProductRequest.java
│   │       ├── UpdateProductRequest.java
│   │       ├── ProductResponse.java
│   │       └── ...
│   │
│   ├── adapter/
│   │   │
│   │   ├── inbound/                  ◄── INPUT ADAPTERS (Controllers)
│   │   │   ├── web/
│   │   │   │   ├── ProductController.java       (delegator to use cases)
│   │   │   │   ├── ProductWebMapper.java        (HTTP DTOs → Application DTOs)
│   │   │   │   └── ProductWebRequest.java       (HTTP models)
│   │   │   │
│   │   │   └── cli/                  (future)
│   │   │       └── ProductCliHandler.java
│   │   │
│   │   └── outbound/                 ◄── OUTPUT ADAPTERS (Persistence)
│   │       ├── persistence/
│   │       │   ├── ProductRepositoryAdapter.java    (implements port)
│   │       │   ├── ProductJpaRepository.java        (Spring Data interface)
│   │       │   ├── ProductJpaEntity.java            (JPA @Entity)
│   │       │   ├── ProductPersistenceMapper.java    (JPA Entity ↔ Aggregate)
│   │       │   └── ProductQueryBuilder.java         (Query construction)
│   │       │
│   │       └── external/              (future)
│   │           └── ImageServiceAdapter.java
│   │
│   └── infrastructure/
│       ├── config/
│       │   └── ProductModuleConfiguration.java  ◄── Spring Bean Wiring
│       │
│       └── exception/
│           └── ProductExceptionHandler.java    ◄── Global Exception Mapping
│
├── api/                              ◄── OLD ARCHITECTURE (Gradually Deprecated)
│   ├── controllers/
│   │   ├── DataProductsController.java      (DELEGATES to new use cases)
│   │   ├── DataCrudGenericController.java   (will be removed)
│   │   └── ...
│   │
│   └── models/
│       ├── ProductPojo.java              (STILL USED for backward compatibility)
│       └── ...
│
├── jpa/                              ◄── OLD ARCHITECTURE (Gradually Deprecated)
│   ├── services/
│   │   ├── crud/
│   │   │   └── ProductsCrudServiceImpl.java  (ONLY for POST/PUT/DELETE)
│   │   ├── conversion/
│   │   │   └── ProductsConverterService.java  (DEPRECATED)
│   │   ├── patch/
│   │   │   └── ProductsPatchService.java     (DEPRECATED)
│   │   └── predicates/
│   │       └── ProductsPredicateService.java (DEPRECATED)
│   │
│   ├── entities/
│   │   ├── Product.java              (DEPRECATED - use ProductAggregate)
│   │   └── ...
│   │
│   └── repositories/
│       └── ProductsRepository.java    (DEPRECATED - use ProductRepositoryAdapter)
│
├── common/                           ◄── CROSS-CUTTING CONCERNS
│   ├── annotations/
│   ├── converters/
│   ├── exceptions/
│   └── services/
│
└── config/                           ◄── GLOBAL CONFIG
    └── ...
```

---

## 1.2 Layer Responsibilities & Class Location

| Layer | Package | Responsibility | Key Classes | Framework Use | Dependencies |
|-------|---------|-----------------|------------|---|---|
| **Enterprise Rules** | `product/domain/aggregate` | Pure business objects | `ProductAggregate`, `ProductAggregateFactory` | NONE | None |
| **Enterprise Rules** | `product/domain/vo` | Business constraints & validation | `ProductName`, `ProductPrice`, `ProductCode` | NONE | None |
| **Enterprise Rules** | `product/domain/exception` | Domain exceptions | `InvalidProductException` | NONE | None |
| **Interface to Rules** | `product/domain/port` | Repository interface (contract only) | `ProductRepository` (interface) | NONE | Domain only |
| **Application Rules** | `product/application/query` | Input models (what to fetch) | `ListProductsQuery`, `GetProductQuery` | NONE | Domain only |
| **Application Rules** | `product/application/result` | Output models (what to return) | `ProductResult`, `PagedProductResult` | NONE | Domain only |
| **Application Rules** | `product/application/use_case` | Orchestration of business flow | `ListProductsUseCaseImpl` | Spring DI | Domain + port |
| **Application Rules** | `product/application/service` | Use case aggregation | `ProductApplicationService` | Spring | Use cases + mappers |
| **Interface Adapters** | `product/adapter/inbound/web` | HTTP handling | `ProductController` | Spring MVC | Use cases |
| **Interface Adapters** | `product/adapter/inbound/web` | Request/Response mapping | `ProductWebMapper` | Spring/Jackson | DTOs |
| **Interface Adapters** | `product/adapter/outbound/persistence` | JPA handling | `ProductRepositoryAdapter` | Spring Data JPA | Port + JPA entity |
| **Interface Adapters** | `product/adapter/outbound/persistence` | ORM mapping | `ProductJpaEntity`, `ProductPersistenceMapper` | JPA/Hibernate | Spring Data |
| **Frameworks & Drivers** | `product/infrastructure/config` | Dependency injection | `ProductModuleConfiguration` | Spring | All layers |

---

## 1.3 Cross-Layer Dependencies (Dependency Inward Rule)

```
DEPENDENCY DIRECTION (✓ = allowed, ✗ = forbidden)

                          ┌─────────────────────┐
                          │  Controllers        │
                          │  (HTTP Adapters)    │
                          └──────────┬──────────┘
                                     │ ✓ depends on
                                     ▼
                          ┌─────────────────────┐
                          │  Use Cases          │
                          │  (Application)      │
                          └──────────┬──────────┘
                                     │ ✓ depends on
                                     ▼
                          ┌─────────────────────┐
                          │  Repository Port    │
                          │  (Domain Interface) │
                          └──────────┬──────────┘
                                     │ ✓ depends on
                                     ▼
                          ┌─────────────────────┐
                          │  Domain Objects     │
                          │  (Aggregates, VOs)  │
                          └─────────────────────┘

        ✗ Domain depends on Use Cases
        ✗ Domain depends on Adapters
        ✗ Use Cases depend on specific Adapters
        ✓ Only Adapters depend on Frameworks (Spring, JPA)

```

---

## 1.4 File Location Reference Table

| Entity | Old Location | New Location | Status |
|--------|---|---|---|
| ProductPojo | `org.trebol.api.models` | `org.trebol.product.adapter.inbound.web` (WebRequest) | Refactor |
| Product JPA Entity | `org.trebol.jpa.entities` | `org.trebol.product.adapter.outbound.persistence` | Keep separate |
| ProductAggregate | N/A | `org.trebol.product.domain.aggregate` | **New** |
| ProductRepository (interface) | `org.trebol.jpa.repositories` | `org.trebol.product.domain.port` | **Move** |
| ProductRepositoryImpl | N/A | `org.trebol.product.adapter.outbound.persistence` | **New** |
| ProductsCrudService | `org.trebol.jpa.services.crud` | DELETE (logic moved to use cases) | **Remove** |
| ListProductsUseCase | N/A | `org.trebol.product.application.use_case` | **New** |
| ProductController | `org.trebol.api.controllers` | Keep as delegator to use cases | Refactor |
| ProductValue Objects | N/A | `org.trebol.product.domain.vo` | **New** |

---

# PART 2: CLASS SEPARATION & MERGING RULES

## 2.1 When to SEPARATE Classes

### Rule 1: Single Responsibility Principle
**Separate when:** A class has more than one reason to change

**Example: OLD (Mixed)**
```java
// ❌ BAD: Two reasons to change
public class ProductsCrudServiceImpl implements ProductsCrudService {
    // Reason 1: Business logic changes (validation, rules)
    public void create(ProductPojo input) { ... }
    
    // Reason 2: Data access changes (repository interface changes)
    public void readMany(filters, pagination) { ... }
}
```

**Example: NEW (Separated)**
```java
// ✓ GOOD: One reason to change (business logic)
public class ListProductsUseCaseImpl implements ListProductsUseCase {
    public PagedProductResult execute(ListProductsQuery query) { ... }
}

// ✓ GOOD: One reason to change (persistence implementation)
public class ProductRepositoryAdapter implements ProductRepository {
    public Page<ProductAggregate> findAll(PageRequest pageRequest) { ... }
}
```

---

### Rule 2: Framework Independence
**Separate when:** Business logic is mixed with framework concerns

**Example: OLD (Mixed)**
```java
// ❌ BAD: Business + Spring/JPA concerns mixed
@Service
public class ProductsCrudServiceImpl {
    @Autowired ProductsRepository repo;
    @Transactional
    public void create(ProductPojo input) {
        // Business: validate barcode
        // Framework: call Spring Data
        // Framework: manage transaction
    }
}
```

**Example: NEW (Separated)**
```java
// ✓ GOOD: Pure business logic
@Service
public class CreateProductUseCaseImpl implements CreateProductUseCase {
    private final ProductRepository repo;  // Interface, not Spring Data
    private final ProductFactory factory;
    
    public ProductResponse execute(CreateProductRequest request) {
        // Pure business logic only
        Product product = factory.create(...);
        repo.save(product);
        return mapper.toResponse(product);
    }
}

// ✓ GOOD: Framework concerns isolated
@Repository
public class ProductRepositoryAdapter implements ProductRepository {
    @Autowired private ProductJpaRepository jpaRepo;
    
    public void save(Product product) {
        ProductJpaEntity entity = mapper.toJpaEntity(product);
        jpaRepo.save(entity);
    }
}
```

---

### Rule 3: Layer Crossing
**Separate when:** A class crosses architectural boundaries

**Example: OLD (Mixed)**
```java
// ❌ BAD: Violates layer boundary
public class ProductsPatchServiceImpl {  // Service Layer
    public void patch(Product entity, Map<String, Object> updates) {
        // This should be a domain method, not service
        entity.setPrice(updates.get("price"));  // Direct field access
    }
}
```

**Example: NEW (Separated)**
```java
// ✓ GOOD: Domain logic in domain layer
public class ProductAggregate {
    public void updatePrice(ProductPrice newPrice) {
        if (!newPrice.isValid()) throw new InvalidPriceException();
        this.price = newPrice;
    }
}

// ✓ GOOD: Use case delegates to domain
public class UpdateProductUseCaseImpl {
    public void execute(UpdateProductRequest request) {
        Product product = repo.findById(id);
        product.updatePrice(new ProductPrice(request.getPrice()));
        repo.save(product);
    }
}
```

---

### Rule 4: Converter/Mapper Explosion
**Separate when:** Mappers have diverging concerns

**Example: OLD (Mixed)**
```java
// ❌ BAD: Multiple conversion concerns mixed
public class ProductsConverterServiceImpl {
    // Concern 1: JPA Entity → API DTO
    public ProductPojo toPojo(Product jpaEntity) { ... }
    
    // Concern 2: API DTO → Domain
    public Product toEntity(ProductPojo pojo) { ... }
    
    // Concern 3: Image conversion
    public List<ImageDto> toImageDtos(List<ProductImage> images) { ... }
}
```

**Example: NEW (Separated)**
```java
// ✓ GOOD: Concern 1 isolated
@Component
public class ProductPersistenceMapper {
    public ProductAggregate toDomain(ProductJpaEntity jpaEntity) { ... }
    public ProductJpaEntity toJpaEntity(ProductAggregate aggregate) { ... }
}

// ✓ GOOD: Concern 2 isolated
@Component
public class ProductApplicationMapper {
    public ProductResult toResult(ProductAggregate aggregate) { ... }
}

// ✓ GOOD: Concern 3 isolated (different domain)
@Component
public class ImageMapper {
    public ImageDto toDto(ProductImage image) { ... }
}
```

---

## 2.2 When to MERGE Classes

### Rule 1: Cohesion Principle
**Merge when:** Two classes are always used together and have high cohesion

**Example: Keep Separate (High Cohesion = Separate)**
```java
// ✓ GOOD: Separate because they serve different purposes
public interface ListProductsUseCase { ... }  // Input port
public class ListProductsUseCaseImpl { ... }   // Implementation

// ✓ GOOD: Keep separate (always used together, but represent different concepts)
public interface ProductRepository { ... }    // Output port
public class ProductRepositoryAdapter { ... } // Implementation
```

**Example: Can Merge (Low Cohesion = Merge)**
```java
// ❌ SEPARATED (Low Cohesion - should be merged)
public interface ListProductsUseCase { }  // Trivial interface
public class ListProductsUseCaseImpl implements ListProductsUseCase {
    // Only 2 methods, simple logic
    public PagedProductResult execute(ListProductsQuery query) { ... }
}

// ✓ GOOD: Merge for simplicity
@Service
public class ListProductsUseCase {
    // Simple, direct, no need for interface
    public PagedProductResult execute(ListProductsQuery query) { ... }
}
```

---

### Rule 2: Query vs Command Objects
**Merge when:** Query/result objects are trivial value holders

**Example: Separate if Complex**
```java
// ✓ GOOD: Separate (complex logic)
public class ListProductsQuery {
    private final int pageIndex;
    private final int pageSize;
    private final Map<String, String> filters;
    private final List<SortField> sortFields;
    
    public List<String> getValidationErrors() { ... }
    public PageRequest toPageRequest() { ... }
}
```

**Example: Merge if Trivial**
```java
// ✓ GOOD: Merge (trivial record)
public record ListProductsQuery(int pageIndex, int pageSize) { }

// Or use Lombok
@Data
@AllArgsConstructor
public class ListProductsQuery {
    private int pageIndex;
    private int pageSize;
}
```

---

### Rule 3: Configuration Beans
**Merge when:** Small, focused configuration classes can consolidate related beans

**Example: Separate if Large**
```java
// ✓ GOOD: Separate (many beans)
@Configuration
public class ProductPersistenceConfig { ... }  // 5+ beans

@Configuration
public class ProductApplicationConfig { ... }  // 5+ beans
```

**Example: Merge if Small**
```java
// ✓ GOOD: Merge (few related beans)
@Configuration
public class ProductModuleConfiguration {
    @Bean
    public ProductApplicationService applicationService(...) { ... }
    
    @Bean
    public ProductRepository repository(...) { ... }
    
    @Bean
    public ProductPersistenceMapper persistenceMapper() { ... }
}
```

---

## 2.3 Decision Matrix: Separate or Merge?

| Scenario | Decision | Reason |
|----------|----------|--------|
| Business logic + Spring annotations in same class | **SEPARATE** | Framework coupling |
| Business logic + JPA annotations in same class | **SEPARATE** | Framework coupling |
| CRUD + Conversion + Patch in same service | **SEPARATE** | SRP (multiple reasons to change) |
| JPA entity + Domain aggregate in same class | **SEPARATE** | Different purposes (ORM vs business) |
| Repository interface + Adapter implementation | **SEPARATE** | Inversion of control (port & adapter) |
| Multiple converters in one service | **SEPARATE** | SRP (each mapper has one reason) |
| Use case interface + implementation (complex logic) | **SEPARATE** | Dependency inversion, testability |
| Use case interface + implementation (trivial logic) | **MERGE** | Simplicity over formality |
| Query object + Result object (trivial) | **MERGE** | Reduce class count |
| Mapper (JPA → DTO) only used in one place | **MERGE** with caller | Inline simple mapping |
| Multiple small configs (few beans each) | **MERGE** | Single responsibility still achievable |

---

# PART 3: REFACTORING PRIORITIZATION & SEQUENCING

## 3.1 Vertical Slice Approach (Recommended)

### Why Vertical Slices?

```
OLD APPROACH (Horizontal Layers): ✗ RISKY
┌─────────────────────────────────────┐
│ Refactor ALL Controllers            │  High risk: affects all endpoints
├─────────────────────────────────────┤
│ Refactor ALL Services               │  High risk: multiple use cases
├─────────────────────────────────────┤
│ Refactor ALL Repositories           │  High risk: all data access
└─────────────────────────────────────┘

NEW APPROACH (Vertical Slices): ✓ SAFE
┌──────────┐  ┌──────────┐  ┌──────────┐
│ Slice 1  │  │ Slice 2  │  │ Slice 3  │
│ GET      │  │ List     │  │ Create   │
│ endpoint │  │ endpoint │  │ endpoint │
└──────────┘  └──────────┘  └──────────┘

Each slice is independent → can test/rollback individually
```

---

## 3.2 Slice Sequence for Products Domain

### Slice 1: GET Single Product (✓ COMPLETE)

**Scope:**
- Endpoint: `GET /data/products/{id}`
- Classes to refactor:
  - `DataProductsController.get()` → delegate to use case
  - `ProductsCrudService.read()` → move logic to use case
  - `ProductsRepository.findById()` → create adapter

**New Classes to Create:**
- `GetProductQuery.java` (query object)
- `ProductResult.java` (result object)
- `GetProductUseCase.java` (interface)
- `GetProductUseCaseImpl.java` (implementation)
- `ProductRepositoryAdapter.java` (implements port)
- `ProductAggregate.java` (domain object)
- `ProductId.java`, `ProductCode.java`, etc. (value objects)

**Why First:** Simplest endpoint (no filters, no pagination) - proof of concept

---

### Slice 2: List Products (IN PROGRESS)

**Scope:**
- Endpoint: `GET /data/products?pageIndex=0&pageSize=10&filters=...`
- Classes to refactor:
  - `DataProductsController.readMany()` → delegate
  - `ProductsCrudService.readMany()` → move logic
  - `ProductsPredicateService.parseMap()` → move to query builder
  - `ProductsRepository.deepReadAll()` → extend adapter

**New Classes to Create:**
- `ListProductsQuery.java` (query with pagination + filters)
- `PagedProductResult.java` (paged results)
- `ListProductsUseCase.java` (interface)
- `ListProductsUseCaseImpl.java` (implementation)
- `ProductQueryBuilder.java` (build JPA predicates)
- Extend `ProductRepositoryAdapter` with `findAll()`

**Why Second:** Builds on Slice 1, introduces pagination/filtering

---

### Slice 3: Create Product

**Scope:**
- Endpoint: `POST /data/products`
- Classes to refactor:
  - `DataProductsController.create()` → delegate
  - `ProductsCrudService.create()` → move logic
  - `ProductsRepository.save()` → extend adapter

**New Classes to Create:**
- `CreateProductRequest.java` (HTTP input)
- `CreateProductCommand.java` (application input)
- `CreateProductUseCase.java` (interface)
- `CreateProductUseCaseImpl.java` (implementation)
- `ProductAggregateFactory.java` (create new aggregates)
- Extend `ProductRepositoryAdapter` with `save()`

**Why Third:** Introduces write operations, factory pattern

---

### Slice 4: Update Product

**Scope:**
- Endpoint: `PUT /data/products/{id}`
- Classes to refactor:
  - `DataProductsController.update()` → delegate
  - `ProductsCrudService.update()` → move logic
  - `ProductsPatchService.patch()` → move to domain methods

**New Classes to Create:**
- `UpdateProductRequest.java` (HTTP input)
- `UpdateProductCommand.java` (application input)
- `UpdateProductUseCase.java` (interface)
- `UpdateProductUseCaseImpl.java` (implementation)
- Add `update*()` methods to `ProductAggregate`

**Why Fourth:** Introduces state modification, domain methods

---

### Slice 5: Delete Product

**Scope:**
- Endpoint: `DELETE /data/products/{id}`
- Classes to refactor:
  - `DataProductsController.delete()` → delegate
  - `ProductsCrudService.delete()` → move logic

**New Classes to Create:**
- `DeleteProductCommand.java` (application input)
- `DeleteProductUseCase.java` (interface)
- `DeleteProductUseCaseImpl.java` (implementation)
- Extend `ProductRepositoryAdapter` with `delete()`

**Why Fifth:** Simplest write operation

---

## 3.3 Refactoring Order Within Each Slice

```
Step 1: Define Domain Layer (bottom-up)
  ↓
  • Create value objects (ProductId, ProductCode, ProductName, ProductPrice)
  • Create aggregate (ProductAggregate)
  • Create domain exceptions
  • Create repository port (interface)

Step 2: Define Application Layer (middle)
  ↓
  • Create query/command objects (input models)
  • Create result objects (output models)
  • Create use case interface
  • Create use case implementation

Step 3: Create Adapter Layer (outer)
  ↓
  • Create persistence mapper (JPA Entity ↔ Aggregate)
  • Create repository adapter (implements port, uses Spring Data)
  • Create web mapper (HTTP ↔ Application DTOs)

Step 4: Update Controller (integrate)
  ↓
  • Update controller to inject use case
  • Update controller to delegate to use case
  • Update controller error handling

Step 5: Update Tests (validate)
  ↓
  • Create unit tests for use case (mock repository)
  • Create integration tests for adapter (with TestContainers)
  • Update contract tests for controller

Step 6: Delete Old Code (cleanup)
  ↓
  • Mark old methods @Deprecated
  • Monitor logs for old code usage
  • Remove old methods after verification period
```

---

## 3.4 Execution Timeline

| Phase | Slices | Week | Effort | Risk |
|-------|--------|------|--------|------|
| **Setup** | Foundation | Week 1 | 2 days | LOW |
| | Base classes, module config | | | |
| **Slice 1** | GET single | Week 1-2 | 2 days | LOW |
| | Proof of concept | | | |
| **Slice 2** | List with filters | Week 2-3 | 3 days | LOW |
| | Query building | | | |
| **Slice 3** | Create | Week 3 | 2 days | MEDIUM |
| | Factory pattern | | | |
| **Slice 4** | Update | Week 4 | 2 days | MEDIUM |
| | Domain methods | | | |
| **Slice 5** | Delete | Week 4 | 1 day | LOW |
| | Cleanup | | | |
| **Testing** | All layers | Week 5 | 3 days | MEDIUM |
| **Other Domains** | Categories, etc. | Week 6+ | TBD | TBD |

---

# PART 4: TESTING STRATEGY

## 4.1 Testing Pyramid

```
                        ▲
                       / \
                      /   \  Integration Tests (10-20%)
                     /     \  
                    /───────\
                   /         \
                  /           \  Unit Tests (60-70%)
                 /             \
                /───────────────\
               /                 \
              /  Contract Tests   \  (10-15%)
             /___________________\
```

---

## 4.2 Old Testing Strategy (3-Layer)

| Test Type | Current Approach | Problem |
|-----------|---|---|
| **Unit Tests** | Mock repository, use Spring context | Slow (need Spring boot context) |
| **Integration Tests** | Use real DB with H2 | Hard to maintain (schema changes) |
| **Contract Tests** | Mock services, verify HTTP shape | Tied to monolithic controller |
| **E2E Tests** | Thunder Client, manual | Slow, hard to automate |

**Pain Points:**
- Can't test domain logic without Spring
- Can't test service logic without repository
- Tests tightly coupled to infrastructure

---

## 4.3 New Testing Strategy (Clean Architecture)

### Level 1: Domain Unit Tests (NO Framework, NO DB)

**Purpose:** Verify business rules in isolation

**Example Test:**
```java
@DisplayName("ProductAggregate Tests")
public class ProductAggregateTest {
    
    @Test
    void canCreateProductWithValidData() {
        // Arrange
        ProductId id = new ProductId(1L);
        ProductCode code = new ProductCode("PROD-001");
        ProductName name = new ProductName("Widget");
        ProductPrice price = new ProductPrice(BigDecimal.valueOf(19.99));
        
        // Act
        ProductAggregate product = new ProductAggregate(id, code, name, price);
        
        // Assert
        assertThat(product.getId()).isEqualTo(id);
        assertThat(product.getCode()).isEqualTo(code);
    }
    
    @Test
    void throwsExceptionWhenPriceIsNegative() {
        // Assert
        assertThatThrownBy(() -> new ProductPrice(BigDecimal.valueOf(-10)))
            .isInstanceOf(InvalidProductPriceException.class);
    }
    
    @Test
    void canUpdateProductPrice() {
        // Arrange
        ProductAggregate product = createTestProduct();
        ProductPrice newPrice = new ProductPrice(BigDecimal.valueOf(29.99));
        
        // Act
        product.updatePrice(newPrice);
        
        // Assert
        assertThat(product.getPrice()).isEqualTo(newPrice);
    }
}
```

**Characteristics:**
- ✓ No Spring needed
- ✓ No database
- ✓ Very fast (microseconds)
- ✓ Can run 1000s in seconds
- ✓ Test pure business logic only

---

### Level 2: Value Object Tests (NO Framework, NO DB)

**Purpose:** Verify value object constraints

**Example Test:**
```java
public class ProductNameTest {
    
    @Test
    void acceptsValidName() {
        ProductName name = new ProductName("Widget");
        assertThat(name.getValue()).isEqualTo("Widget");
    }
    
    @Test
    void throwsExceptionForEmptyName() {
        assertThatThrownBy(() -> new ProductName(""))
            .isInstanceOf(InvalidProductNameException.class)
            .hasMessageContaining("cannot be empty");
    }
    
    @Test
    void throwsExceptionForNameTooLong() {
        String tooLong = "A".repeat(201);
        assertThatThrownBy(() -> new ProductName(tooLong))
            .isInstanceOf(InvalidProductNameException.class)
            .hasMessageContaining("too long");
    }
    
    @Test
    void valueObjectsAreEqual() {
        ProductName name1 = new ProductName("Widget");
        ProductName name2 = new ProductName("Widget");
        assertThat(name1).isEqualTo(name2);
    }
}
```

**Characteristics:**
- ✓ No Spring
- ✓ No database
- ✓ Extremely fast
- ✓ Tests business constraints

---

### Level 3: Use Case Tests (Mock Repository, NO DB)

**Purpose:** Verify application logic without touching database

**Example Test:**
```java
@DisplayName("ListProductsUseCase Tests")
public class ListProductsUseCaseImplTest {
    
    private ListProductsUseCase useCase;
    private ProductRepository mockRepository;
    private ProductApplicationMapper mockMapper;
    
    @BeforeEach
    void setUp() {
        mockRepository = mock(ProductRepository.class);
        mockMapper = mock(ProductApplicationMapper.class);
        useCase = new ListProductsUseCaseImpl(mockRepository, mockMapper);
    }
    
    @Test
    void returnsPagedProductsWhenRepositoryHasData() {
        // Arrange
        ListProductsQuery query = new ListProductsQuery(0, 10);
        ProductAggregate aggregate = createTestAggregate();
        PagedProductResult mockResult = mock(PagedProductResult.class);
        
        when(mockRepository.findAll(any()))
            .thenReturn(new Page<>(List.of(aggregate), 1, 10));
        when(mockMapper.toResult(aggregate))
            .thenReturn(new ProductResult(...));
        
        // Act
        PagedProductResult result = useCase.execute(query);
        
        // Assert
        assertThat(result.getItems()).hasSize(1);
        verify(mockRepository).findAll(any());
    }
    
    @Test
    void returnsEmptyPageWhenNoProductsFound() {
        // Arrange
        ListProductsQuery query = new ListProductsQuery(0, 10);
        when(mockRepository.findAll(any()))
            .thenReturn(new Page<>(List.of(), 0, 10));
        
        // Act
        PagedProductResult result = useCase.execute(query);
        
        // Assert
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalCount()).isZero();
    }
}
```

**Characteristics:**
- ✓ No database
- ✓ Uses mocks for repository
- ✓ Fast (milliseconds)
- ✓ Tests orchestration logic
- ✓ Repository still injected as interface

---

### Level 4: Repository Adapter Tests (TestContainers)

**Purpose:** Verify persistence adapter works with real DB

**Example Test:**
```java
@DisplayName("ProductRepositoryAdapter Integration Tests")
@DataJpaTest  // Only load JPA context
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class ProductRepositoryAdapterIntegrationTest {
    
    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>(
        DockerImageName.parse("mariadb:latest")
    );
    
    @Autowired
    private ProductRepositoryAdapter adapter;
    
    @Autowired
    private ProductJpaRepository jpaRepository;
    
    @Test
    void canSaveAndRetrieveProduct() {
        // Arrange
        ProductAggregate aggregate = createTestAggregate();
        
        // Act
        adapter.save(aggregate);
        Optional<ProductAggregate> retrieved = adapter.findById(aggregate.getId());
        
        // Assert
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCode()).isEqualTo(aggregate.getCode());
    }
    
    @Test
    void returnsEmptyWhenProductNotFound() {
        // Act
        Optional<ProductAggregate> result = adapter.findById(new ProductId(999L));
        
        // Assert
        assertThat(result).isEmpty();
    }
    
    @Test
    void returnsPagedResults() {
        // Arrange
        for (int i = 0; i < 25; i++) {
            adapter.save(createTestAggregate());
        }
        Pageable pageable = PageRequest.of(0, 10);
        
        // Act
        Page<ProductAggregate> result = adapter.findAll(pageable);
        
        // Assert
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(25);
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
    }
}
```

**Characteristics:**
- ✓ Tests with real database (TestContainers)
- ✓ Slower (seconds, not milliseconds)
- ✓ Verifies ORM mapping
- ✓ Verifies query building
- ✓ Verifies schema alignment

---

### Level 5: Controller Contract Tests

**Purpose:** Verify HTTP API contract

**Example Test:**
```java
@DisplayName("ProductController Contract Tests")
@WebMvcTest(ProductController.class)
public class ProductControllerContractTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ListProductsUseCase listUseCase;
    
    @Test
    void getProductsReturnsOkWithPagedShape() throws Exception {
        // Arrange
        ProductResult product = new ProductResult(1L, "PROD-001", "Widget", 19.99);
        PagedProductResult result = new PagedProductResult(
            List.of(product), 1
        );
        when(listUseCase.execute(any()))
            .thenReturn(result);
        
        // Act & Assert
        mockMvc.perform(get("/data/products")
                .param("pageIndex", "0")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].id", is(1)))
            .andExpect(jsonPath("$.totalCount", is(1)));
    }
    
    @Test
    void getProductsReturnsUnauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(get("/data/products"))
            .andExpect(status().isUnauthorized());
    }
}
```

**Characteristics:**
- ✓ Tests HTTP layer only
- ✓ Mocks use cases
- ✓ Verifies response shape
- ✓ Verifies status codes
- ✓ Verifies error handling

---

## 4.4 Test Coverage Matrix

| Layer | Class Type | Test Type | Framework | DB | Speed | Count |
|-------|-----------|-----------|-----------|-----|-------|-------|
| **Domain** | Aggregate | Unit | None | No | <1ms | 10-15 |
| **Domain** | Value Object | Unit | None | No | <1ms | 20-30 |
| **Application** | Use Case | Unit (mock repo) | Mock | No | ~5ms | 5-10 per use case |
| **Adapter** | Repository | Integration | TestContainers | Yes | 100-500ms | 5-10 |
| **Adapter** | Web Mapper | Unit | None | No | <1ms | 3-5 |
| **Controller** | REST API | Contract | MockMvc | No | ~20ms | 5-10 per endpoint |
| **E2E** | Full flow | E2E | Real stack | Yes | 1000ms+ | 3-5 critical paths |

---

# PART 5: TEST TRANSITION PLAN

## 5.1 Old Tests → New Tests Mapping

### Old: ProductsCrudServiceImplTest (DELETE)

```java
// ❌ OLD TEST (DEPRECATED - use case tests instead)
@RunWith(MockitoJUnitRunner.class)
public class ProductsCrudServiceImplTest {
    
    @Mock private ProductsRepository repository;
    @Mock private ProductsConverterService converter;
    
    private ProductsCrudServiceImpl service;
    
    @Before
    public void setUp() {
        service = new ProductsCrudServiceImpl(repository, converter);
    }
    
    @Test
    public void testReadMany() {
        // Mock Spring Data JPA
        List<Product> mockEntities = ...;
        when(repository.deepReadAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(mockEntities));
        
        // Mock converter
        when(converter.convertToPojo(any()))
            .thenReturn(new ProductPojo());
        
        // Execute
        Page<ProductPojo> result = service.readMany(...);
        
        // Verify
        assertThat(result.getContent()).hasSize(1);
    }
}
```

### New: ListProductsUseCaseImplTest (USE INSTEAD)

```java
// ✓ NEW TEST (domain-driven, cleaner)
@DisplayName("ListProductsUseCase Tests")
public class ListProductsUseCaseImplTest {
    
    private ListProductsUseCase useCase;
    private ProductRepository mockRepository;
    
    @BeforeEach
    void setUp() {
        mockRepository = mock(ProductRepository.class);
        useCase = new ListProductsUseCaseImpl(mockRepository, ...);
    }
    
    @Test
    void returnsPagedProductsWhenRepositoryHasData() {
        // Cleaner: work with domain objects
        ProductAggregate aggregate = createTestAggregate();
        when(mockRepository.findAll(any()))
            .thenReturn(new Page<>(List.of(aggregate), 1, 10));
        
        PagedProductResult result = useCase.execute(
            new ListProductsQuery(0, 10)
        );
        
        assertThat(result.getItems()).hasSize(1);
    }
}
```

---

### Old: ProductsRepositoryTest (REPLACE)

```java
// ❌ OLD TEST (tied to Spring Data JPA)
@RunWith(SpringRunner.class)
@DataJpaTest
public class ProductsRepositoryTest {
    
    @Autowired private ProductsRepository repository;
    
    @Test
    public void testFindByBarcode() {
        // Requires Spring Data interface specifics
        Optional<Product> result = repository.findByBarcode("BARCODE-123");
        assertThat(result).isPresent();
    }
}
```

### New: ProductRepositoryAdapterIntegrationTest (USE INSTEAD)

```java
// ✓ NEW TEST (domain-driven, framework-agnostic)
@DisplayName("ProductRepository Integration Tests")
@Testcontainers
public class ProductRepositoryAdapterIntegrationTest {
    
    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>(...);
    
    private ProductRepositoryAdapter adapter;
    
    @Test
    void canFindProductByCode() {
        // Work with domain aggregate
        ProductAggregate product = createTestAggregate();
        adapter.save(product);
        
        Optional<ProductAggregate> result = 
            adapter.findByCode(product.getCode());
        
        assertThat(result).contains(product);
    }
}
```

---

## 5.2 Test Obsolescence Decision Tree

```
                    Is this test still needed?
                              |
                 _____________|______________
                |                            |
            Is it testing    Is it testing
            old CRUD service?    old entity?
                |                    |
               YES                  YES
                |                    |
              DELETE            REPLACE
              (use case test     (domain
               instead)        test instead)
                                    |
                            (Use Value Object test)
                                    
                NO - Tests new component?
                |
           YES - MIGRATE
           (refactor for new structure)
           
           NO - Generic utility?
           |
           YES - KEEP
           (reuse in new tests)
```

---

## 5.3 Test Refactoring Checklist (Per Slice)

### For Each Slice, Apply This Checklist:

```
□ Step 1: Create new domain tests
  □ Value object tests (ProductId, ProductCode, ProductPrice, etc.)
  □ Aggregate tests (ProductAggregate behavior)
  □ Domain exception tests

□ Step 2: Create new application tests
  □ Use case tests with mocked repository
  □ Query builder tests
  □ Application mapper tests

□ Step 3: Create new adapter tests
  □ Repository adapter integration tests (TestContainers)
  □ Persistence mapper tests (JPA Entity ↔ Aggregate)
  □ Web mapper tests (HTTP ↔ Application DTOs)

□ Step 4: Create new controller tests
  □ Contract tests (HTTP shape, status codes)
  □ Error handling tests
  □ Authorization tests

□ Step 5: Deprecate old tests
  □ Mark old test file @Deprecated
  □ Move to separate "deprecated" package
  □ Update CI/CD to warn on deprecated tests

□ Step 6: Update CI/CD
  □ Run new tests in pipeline
  □ Verify new tests pass
  □ Collect coverage metrics
  □ Compare with old coverage

□ Step 7: Cleanup (after verification)
  □ Delete old test files
  □ Delete old implementation classes
  □ Remove old test data factories
  □ Update documentation
```

---

# PART 6: QUALITY ASSURANCE DURING REFACTORING

## 6.1 Continuous Validation Strategy

### Phase 1: Before Refactoring (Baseline)

```bash
Step 1: Establish baseline
  ✓ Run all existing tests
  ✓ Measure code coverage (old architecture)
  ✓ Run SonarQube / code quality checks
  ✓ Document current behavior via Thunder Client
  ✓ Take screenshot of test results

Metrics to capture:
  - Total test count
  - Code coverage %
  - Cyclomatic complexity
  - Code smell count
  - Security issues count
```

---

### Phase 2: During Refactoring (Per Slice)

```bash
For each slice:

Step 1: Implement new code (domain → use case → adapter)
Step 2: Write new tests
  ✓ Domain unit tests pass
  ✓ Use case tests pass (mocked)
  ✓ Adapter integration tests pass (TestContainers)
  ✓ Controller contract tests pass

Step 3: Verify backward compatibility
  ✓ Old endpoint still works
  ✓ Old tests still pass
  ✓ Old code not deleted (only delegated)

Step 4: Run full test suite
  ✓ No regressions
  ✓ New tests don't break old tests

Step 5: Code review checkpoint
  ✓ Review domain objects (no framework)
  ✓ Review use case (orchestration only)
  ✓ Review adapter (framework concerns)
  ✓ Review controller (delegation only)

Step 6: Deploy to staging
  ✓ Both old and new code active
  ✓ Controller delegates to new, fallback to old if needed
  ✓ Monitor logs for errors
```

---

### Phase 3: After Refactoring (Per Slice)

```bash
Step 1: Verify metrics
  ✓ Code coverage maintained or improved
  ✓ Cyclomatic complexity reduced
  ✓ No new code smells introduced
  ✓ No new security issues

Step 2: Performance testing
  ✓ New endpoint response time ≤ old endpoint
  ✓ New tests run fast enough for CI/CD

Step 3: Production monitoring
  ✓ Error rate same or lower
  ✓ Response times same or faster
  ✓ No new exceptions

Step 4: Cleanup
  ✓ Mark old code @Deprecated
  ✓ Monitor for old code usage
  ✓ After 1-2 weeks, delete old code
```

---

## 6.2 CI/CD Integration

### GitHub Actions / GitLab CI Pipeline

```yaml
# Before each commit
on: [push, pull_request]

jobs:
  test-new-architecture:
    runs-on: ubuntu-latest
    services:
      mariadb:
        image: mariadb:latest
        env:
          MYSQL_ROOT_PASSWORD: root
    
    steps:
      - uses: actions/checkout@v2
      
      # Old tests must still pass (backward compat)
      - name: Run old tests
        run: mvn test -Dtest=*OldTest
      
      # New tests must pass
      - name: Run new domain tests
        run: mvn test -Dtest=*Domain*Test
      
      - name: Run new use case tests
        run: mvn test -Dtest=*UseCase*Test
      
      # Integration tests with TestContainers
      - name: Run integration tests
        run: mvn test -Dtest=*Integration*Test
      
      # Contract tests
      - name: Run contract tests
        run: mvn test -Dtest=*Contract*Test
      
      # Code quality
      - name: SonarQube scan
        run: mvn sonar:sonar -Dsonar.projectKey=trebol
      
      # Coverage report
      - name: Generate coverage
        run: mvn jacoco:report
      
      # Report to PR
      - name: Comment PR with test results
        uses: actions/github-script@v6
        with:
          script: |
            // Post test results to PR comment
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: `✓ All tests passed\n
                Coverage: ${coverage}%\n
                New tests: ${newTestCount}\n
                Old tests: ${oldTestCount}`
            })
```

---

## 6.3 Test Execution Checklist (Before Production Deploy)

```
BEFORE DEPLOYING A SLICE TO PRODUCTION:

□ Unit Tests
  □ All domain tests pass
  □ All value object tests pass
  □ All use case tests pass (mocked)
  □ Coverage ≥ 80%

□ Integration Tests
  □ Repository adapter tests pass (TestContainers)
  □ Persistence mapper tests pass
  □ Web mapper tests pass
  □ No timeout issues

□ Contract Tests
  □ HTTP endpoint returns correct status
  □ Response JSON matches schema
  □ Error cases handled correctly
  □ Authorization checks work

□ Backward Compatibility
  □ Old tests still pass
  □ Old endpoint still works
  □ Old code not broken
  □ Graceful fallback if new code fails

□ Performance
  □ New endpoint response ≤ old endpoint
  □ No memory leaks detected
  □ No n+1 queries detected
  □ Database connection pooling works

□ Code Quality
  □ SonarQube: ≥ Quality Gate score
  □ No new code smells
  □ No new security vulnerabilities
  □ Cyclomatic complexity ≤ 10 per method

□ Documentation
  □ Domain objects documented
  □ Use cases documented
  □ API contract documented
  □ Migration notes updated

□ Team Review
  □ Code review approved by 2+ reviewers
  □ Architecture review approved by lead
  □ Test coverage reviewed
  □ Performance reviewed
```

---

## 6.4 Risk Mitigation Strategy

| Risk | Mitigation | Detection |
|------|-----------|-----------|
| Breaking old API | Keep old controller, delegate only | Old tests still pass |
| Database errors | Use TestContainers for integration tests | Integration test results |
| Memory leaks | Profile in integration tests | Memory monitoring |
| N+1 queries | Use query builder tests | Hibernate query logging |
| Mapper bugs | Test all mappings (domain↔JPA↔HTTP) | Mapper unit tests |
| Missing business rules | Domain tests verify all rules | Code review of aggregates |
| Thread safety issues | Run concurrent tests | Stress testing |
| Schema misalignment | TestContainers verify schema | Integration test schema check |

---

## 6.5 Rollback Procedure

```
IF NEW CODE FAILS IN PRODUCTION:

Immediate (< 5 minutes):
  1. Revert the commit
  2. Redeploy previous version
  3. Alert team

Investigation (1-2 hours):
  1. Review error logs
  2. Run failed test in local environment
  3. Create reproduction case
  4. Fix and add regression test

Prevention:
  1. Add scenario to test suite
  2. Run full suite again
  3. Code review to find similar issues
  4. Update monitoring/alerting
```

---

## Summary Table: Test Strategy by Architecture

| Aspect | Old (3-Layer) | New (Clean) | Benefit |
|--------|---|---|---|
| **Domain Tests** | None (logic in service) | Extensive (pure Java) | ✓ Faster, clearer intent |
| **Service Tests** | Mock DB, Spring context | Use case tests (mock repo) | ✓ No context, faster |
| **Repo Tests** | Spring Data specifics | TestContainers (real DB) | ✓ Real integration, portable |
| **Controller Tests** | Mock services | Mock use cases | ✓ Cleaner, smaller mock surface |
| **Test Speed** | Slow (needs Spring) | Fast (most no framework) | ✓ Faster feedback |
| **Test Isolation** | Mixed concerns | Separated concerns | ✓ Easier to debug |
| **Test Reuse** | Hard (tight coupling) | Easy (loose coupling) | ✓ Less duplicate tests |
| **Maintenance** | High (many mocks) | Low (clear structure) | ✓ Tests are docs |

---

## Conclusion: Testing is Key to Safe Refactoring

✓ **Domain tests** verify business logic independently
✓ **Use case tests** verify orchestration independently  
✓ **Integration tests** verify database mapping
✓ **Contract tests** verify HTTP API
✓ **Backward compat tests** ensure old code still works

This multi-layer approach ensures quality throughout the refactoring process.
