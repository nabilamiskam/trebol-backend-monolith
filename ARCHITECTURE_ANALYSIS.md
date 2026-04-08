# Products Domain: Current vs. Clean/Hexagonal Architecture

## PART 1: CURRENT STATE (3-LAYERED ARCHITECTURE)

### 1.1 Current Architecture Diagram


┌─────────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                             │
│                   (REST API Controllers)                            │
├─────────────────────────────────────────────────────────────────────┤
│  DataProductsController                                             │
│  ├─ GET /data/products        (read many)                           │
│  ├─ POST /data/products       (create)                              │
│  ├─ PUT /data/products        (update)                              │
│  ├─ PATCH /data/products      (partial update)                      │
│  └─ DELETE /data/products     (delete)                              │
│                                                                     │
│  Dependencies Injected:                                             │
│  ├─ ProductsCrudService       ◄── BUSINESS LOGIC                    │
│  ├─ ProductsPredicateService  ◄── BUSINESS LOGIC                    │
│  ├─ PaginationService         ◄── CROSS-CUTTING                     │
│  └─ SortSpecParserService     ◄── CROSS-CUTTING                     │
└─────────────────────────────────────────────────────────────────────┘
                                 ▲
                                 │ (uses)
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     BUSINESS LOGIC LAYER                            │
│                    (Service Implementations)                        │
├─────────────────────────────────────────────────────────────────────┤
│  ProductsCrudServiceImpl                                            │
│  ├─ create(ProductPojo)          ◄── Core Business Logic            │
│  ├─ readMany(filters, pagination)◄── Core Business Logic            │
│  ├─ update(ProductPojo)          ◄── Core Business Logic            │
│  ├─ partialUpdate(Map)           ◄── Core Business Logic            │
│  └─ delete(id)                   ◄── Core Business Logic            │
│                                                                     │
│  ProductsConverterServiceImpl                                       │
│  ├─ convertToPojo(Product)           ◄── Data Transformation        │
│  ├─ convertToNewEntity(ProductPojo)  ◄── Data Transformation        │
│  └─ convertImagesToPojo(...)         ◄── Data Transformation        │
│                                                                     │
│  ProductsPredicateServiceImpl                                       │
│  └─ parseMap(filters)            ◄── Query Building (QueryDSL)      │
│                                                                     │
│  ProductsPatchServiceImpl                                           │
│  └─ patch(entity, updates)       ◄── Partial Updates                │
│                                                                     │
│  Dependencies Injected:                                             │
│  ├─ ProductsRepository           ◄── DATA ACCESS                    │
│  ├─ ProductsConverterService     ◄── BUSINESS LOGIC                 │
│  ├─ ProductsPatchService         ◄── BUSINESS LOGIC                 │
│  ├─ ProductImagesRepository      ◄── DATA ACCESS                    │
│  ├─ ImagesCrudService            ◄── BUSINESS LOGIC (other domain)  │
│  └─ ProductsCategoriesRepository ◄── DATA ACCESS                    │
└─────────────────────────────────────────────────────────────────────┘
                                 ▲
                                 │ (uses)
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     DATA ACCESS LAYER                               │
│                 (Spring Data JPA Repositories)                      │
├─────────────────────────────────────────────────────────────────────┤
│  ProductsRepository                                                 │
│  ├─ deepReadAll(Pageable)                                           │
│  ├─ deepReadAll(Predicate, Pageable)                                │
│  ├─ findByBarcode(String)                                           │
│  ├─ setProductCategoryById(id, categoryId)                          │
│  └─ orphanizeByCategories(Collection)                               │
│                                                                     │
│  ProductImagesRepository                                            │
│  ├─ deepFindProductImagesByProductId(productId)                     │
│  ├─ deleteByProductId(productId)                                    │
│  └─ (standard CRUD)                                                 │
│                                                                     │
│  ProductsCategoriesRepository                                       │
│  ├─ findByCode(String)                                              │
│  └─ (standard CRUD)                                                 │
│                                                                     │
│  Spring Data Providers:                                             │
│  ├─ JPA/Hibernate      ◄── ORM Framework                            │
│  └─ QueryDSL           ◄── Dynamic Query Building                   │
└─────────────────────────────────────────────────────────────────────┘
                                 ▼
                         Database (MariaDB)
```

### 1.2 Current Issues & Problems

|    Issue                 |       Impact         |       Example            |
|--------------------------|----------------------|--------------------------|
| **High Framework Coupling** | Hard to test, tied to Spring Data JPA | Repositories expose Spring Data interfaces |
| **Business Logic Mixed with Infrastructure** | Difficult to extract business rules | CRUD service handles image relationships AND DB operations |
| **No Clear Domain Boundaries** | Difficult to identify core domain rules | Product entity is also a JPA @Entity |
| **Data Layer Leaks into Service Layer** | Predicate/filter logic is too technical | `ProductsPredicateService` exposes QueryDSL |
| **No Explicit Use Cases** | Hard to understand what system does | CRUD operations not explicitly named |
| **Tight Coupling Between Domains** | Changes in other domains break this | `ProductsCrudService` directly depends on `ImagesCrudService` |
| **No Anti-Corruption Layer** | External changes directly affect domain | External image representation = internal representation |

---

## PART 2: CLEAN ARCHITECTURE (PROPOSED)

### 2.1 Clean Architecture Principles

```
                    DEPENDENCIES POINT INWARD

┌────────────────────────────────────────────────────────────────┐
│                                                                │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                   ENTERPRISE RULES                        │ │
│  │           (Entities - Pure Business Objects)              │ │
│  │                                                           │ │
│  │  • Product (value object, no framework annotations)       │ │
│  │  • ProductCategory (value object)                         │ │
│  │  • ProductImage (value object)                            │ │
│  │  • ProductSpecification (domain logic)                    │ │
│  └───────────────────────────────────────────────────────────┘ │
│                         ▲                                      │
│                         │ (depends on)                         │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              APPLICATION BUSINESS RULES                   │ │
│  │         (Use Cases / Application Services)                │ │
│  │                                                           │ │
│  │  • CreateProductUseCase                                   │ │
│  │  • UpdateProductUseCase                                   │ │
│  │  • GetProductUseCase                                      │ │
│  │  • ListProductsUseCase                                    │ │
│  │  • DeleteProductUseCase                                   │ │
│  │                                                           │ │
│  │  Dependencies:                                            │ │
│  │  ├─ ProductRepository (interface)                         │ │
│  │  ├─ ProductFactory (interface)                            │ │
│  │  ├─ ImageService (interface)                              │ │
│  │  └─ ProductValidator (domain service)                     │ │
│  └───────────────────────────────────────────────────────────┘ │
│                         ▲                                      │
│                         │ (depends on)                         │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │            INTERFACE ADAPTERS                             │ │
│  │     (Controllers, Gateways, Presenters)                   │ │
│  │                                                           │ │
│  │  INPUT ADAPTERS (Controllers/REST):                       │ │
│  │  ├─ CreateProductController                               │ │
│  │  ├─ UpdateProductController                               │ │
│  │  ├─ GetProductController                                  │ │
│  │  ├─ ListProductsController                                │ │
│  │  └─ DeleteProductController                               │ │
│  │                                                           │ │
│  │  OUTPUT ADAPTERS (Presenters/DTOs):                       │ │
│  │  ├─ ProductPresenter (converts Product → ProductDTO)      │ │
│  │  ├─ ProductDTO (response model)                           │ │
│  │  └─ ProductRequest (input model)                          │ │
│  │                                                           │ │
│  │  Dependencies:                                            │ │
│  │  ├─ Use Cases (from inner ring)                           │ │
│  │  └─ Repository Implementation (outer ring)                │ │
│  └───────────────────────────────────────────────────────────┘ │
│                         ▲                                      │
│                         │ (depends on)                         │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │         FRAMEWORKS & DRIVERS (Externals)                  │ │
│  │                                                           │ │
│  │  • Spring Framework (DI, REST)                            │ │
│  │  • JPA/Hibernate (ORM)                                    │ │
│  │  • Database (MariaDB)                                     │ │
│  │  • External APIs (Image service)                          │ │
│  │                                                           │ │
│  │  Implementations:                                         │ │
│  │  ├─ ProductRepositoryJpaImpl                              │ │
│  │  │   (adapts ProductRepository interface to JPA)          │ │
│  │  │                                                        │ │
│  │  ├─ ProductFactoryImpl                                    │ │
│  │  │   (creates Product value objects)                      │ │
│  │  │                                                        │ │
│  │  └─ ImageServiceRemoteImpl                                │ │
│  │      (adapts external image service)                      │ │
│  │                                                           │ │
│  │  Technical Config:                                        │ │
│  │  ├─ Spring Boot Configuration                             │ │
│  │  ├─ JPA Entity Mapping (different from domain objects)    │ │
│  │  └─ Dependency Injection Setup                            │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                │
└────────────────────────────────────────────────────────────────┘

KEY PRINCIPLE: Inner layers DON'T depend on outer layers
```

### 2.2 Clean Architecture for Products Domain

```
PROJECT STRUCTURE:

src/main/java/org/trebol/products/
│
├── domain/
│   ├── entities/
│   │   ├── Product.java                    ◄── Pure Java class (NO @Entity)
│   │   ├── ProductCategory.java            ◄── Pure Java class (NO @Entity)
│   │   ├── ProductImage.java               ◄── Pure Java class (NO @Entity)
│   │   └── ProductId.java                  ◄── Value Object (ID)
│   │
│   ├── value_objects/
│   │   ├── ProductName.java                ◄── Encapsulates validation
│   │   ├── ProductBarcode.java             ◄── Encapsulates validation
│   │   ├── ProductPrice.java               ◄── Encapsulates business rules
│   │   ├── Stock.java                      ◄── Encapsulates stock logic
│   │   └── Dimensions.java                 ◄── Example: product dimensions
│   │
│   ├── services/
│   │   ├── ProductDomainService.java       ◄── Domain service interface
│   │   ├── ProductValidator.java           ◄── Validates business rules
│   │   └── StockAllocator.java             ◄── Allocates stock
│   │
│   └── repositories/
│       └── ProductRepository.java          ◄── Interface ONLY (contract)
│
├── application/
│   ├── dto/
│   │   ├── CreateProductRequest.java       ◄── Input DTO
│   │   ├── UpdateProductRequest.java       ◄── Input DTO
│   │   ├── ProductResponse.java            ◄── Output DTO
│   │   └── ProductListResponse.java        ◄── Output DTO
│   │
│   ├── use_cases/
│   │   ├── CreateProductUseCase.java       ◄── Use case interface
│   │   ├── CreateProductUseCaseImpl.java    ◄── Use case implementation
│   │   ├── UpdateProductUseCase.java
│   │   ├── GetProductUseCase.java
│   │   ├── ListProductsUseCase.java
│   │   ├── DeleteProductUseCase.java
│   │   └── SearchProductsUseCase.java
│   │
│   ├── mappers/
│   │   ├── ProductMapper.java              ◄── DTO ↔ Domain conversion
│   │   └── CreateProductRequestMapper.java
│   │
│   └── services/
│       ├── ProductApplicationService.java  ◄── Orchestrates use cases
│       └── ProductQueryService.java        ◄── Read operations
│
├── presentation/
│   ├── controllers/
│   │   ├── CreateProductController.java    ◄── Single Responsibility
│   │   ├── UpdateProductController.java
│   │   ├── GetProductController.java
│   │   ├── ListProductsController.java
│   │   ├── DeleteProductController.java
│   │   └── SearchProductsController.java
│   │
│   └── presenters/
│       ├── ProductPresenter.java
│       └── ProductListPresenter.java
│
└── infrastructure/
    ├── repositories/
    │   ├── ProductRepositoryJpaImpl.java    ◄── JPA implementation
    │   ├── ProductJpaEntity.java           ◄── JPA entity (separate)
    │   ├── ProductJpaRepository.java       ◄── Spring Data interface
    │   └── ProductJpaMapper.java           ◄── Entity ↔ Domain mapping
    │
    ├── config/
    │   └── ProductsModuleConfig.java       ◄── DI setup for this domain
    │
    ├── external/
    │   ├── ImageServiceClient.java         ◄── External API adapter
    │   └── ImageServiceAdapterImpl.java
    │
    └── persistence/
        └── queries/
            ├── ProductQueryBuilder.java
            └── ProductQueryExecutor.java
```

---

## PART 3: HEXAGONAL ARCHITECTURE (ALTERNATIVE CLEAN APPROACH)

### 3.1 Hexagonal Architecture Concept

```
Hexagonal Architecture = Ports & Adapters

              PRIMARY ADAPTERS (Input)
                     ▲    ▲
                     │    │
        Web UI   ───┘     └─── REST API
        Desktop  ───┐     ┌─── SOAP
                     │    │
           ┌─────────┴────┴─────────┐
           │                        │
           │   APPLICATION CORE     │
           │  (Business Logic)      │
           │                        │
           └────────┬──────┬────────┘
                    │      │
           PORTS (Interfaces)
                    │      │
    Database ──────┘      └────── Cache
    Filesystem            Message Queue
           
           SECONDARY ADAPTERS (Output)
```

### 3.2 Hexagonal Architecture for Products Domain

```
┌────────────────────────────────────────────────────────────────────┐
│                    PRIMARY ADAPTERS (Input)                        │
│                                                                    │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │ REST Controller  │  │CLI Controller    │  │GraphQL Handler   │  │
│  │                  │  │                  │  │                  │  │
│  │ POST /products   │  │product create    │  │query getProduct  │  │
│  │ PUT /products    │  │product update    │  │mutation ...      │  │
│  │ GET /products    │  │product delete    │  │                  │  │
│  │ DELETE /products │  │                  │  │                  │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘  │
│                         ▲                                          │
└────────────────┬────────┴──────────────────────────────────────────┘
                 │
        ┌────────▼────────┐
        │  INPUT PORTS    │
        │  (Interfaces)   │
        ├─────────────────┤
        │ CreateProduct   │
        │ UpdateProduct   │
        │ GetProduct      │
        │ ListProducts    │
        │ DeleteProduct   │
        │ SearchProducts  │
        └────────┬────────┘
                 │
        ┌────────▼──────────────────────────────────┐
        │                                           │
        │    ┌──────────────────────────────────┐   │
        │    │   APPLICATION / DOMAIN CORE      │   │
        │    │                                  │   │
        │    │  • Product (domain entity)       │   │
        │    │  • ProductFactory (creates)      │   │
        │    │  • ProductValidator (validates)  │   │
        │    │  • CreateProductUseCase          │   │
        │    │  • UpdateProductUseCase          │   │
        │    │  • GetProductUseCase             │   │
        │    │  • ListProductsUseCase           │   │
        │    │  • DeleteProductUseCase          │   │
        │    │  • SearchProductsUseCase         │   │
        │    │                                  │   │
        │    └──────────────────────────────────┘   │
        │                                           │
        └────────┬─────────────────────┬────────────┘
                 │                     │
        ┌────────▼───────┐    ┌────────▼──────────┐
        │ OUTPUT PORTS   │    │ OUTPUT PORTS      │
        │ (Interfaces)   │    │ (Interfaces)      │
        ├────────────────┤    ├───────────────────┤
        │ProductRepository│   │ImageService       │
        │ProductFactory  │    │CategoryService    │
        │NotifyService   │    │PricingService     │
        │LoggingService  │    │                   │
        └────────┬───────┘    └───────  ┬─────────┘
                 |                     |
                 │                     │
    ┌────────────▼─────┐     ┌─────────▼──────────────────┐
    │ SECONDARY         │     │ SECONDARY                 │
    │ ADAPTERS          │     │ ADAPTERS                  │
    │ (Output)          │     │ (Output)                  │
    │                   │     │                           │
    │ ┌───────────────┐ │     │ ┌─────────────────────┐  │
    │ │JPARepository  │ │     │ │RemoteImageService   │  │
    │ │ Implementation│ │     │ │ HTTPClient          │  │
    │ │               │ │     │ │                     │  │
    │ │Accesses:      │ │     │ │Calls:               │  │
    │ │ ProductEntity │ │     │ │ External Image API  │  │
    │ │  (JPA @Entity)│ │     │ │                     │  │
    │ │               │ │     │ │RemoteCategoryService│  │
    │ │HibernateConfig│ │     │ │                     │  │
    │ └───────────────┘ │     │ └─────────────────────┘  │
    │                   │     │                          │
    └───────────┬───────┘     └───────────┬──────────────┘
                │                         │
        ┌───────▼───────────┐    ┌────────▼──────────┐
        │  MariaDB          │    │ External Services │
        │  (Data Storage)   │    │ • Image API       │
        │                   │    │ • Category API    │
        └───────────────────┘    │ • Price Service   │
                                 └───────────────────┘
```

---

## PART 4: COMPARISON TABLE

| Aspect | Current (3-Layer) | Clean Architecture | Hexagonal |
|--------|-------------------|-------------------|-----------|
| **Domain Logic Location** | Mixed in services | Pure domain entities | Core (center) |
| **Framework Coupling** | HIGH (Spring Data JPA) | LOW (interfaces only) | LOW (interfaces) |
| **Testing** | Difficult (needs DB) | Easy (pure Java) | Easy (mock adapters) |
| **Domain Entities** | JPA @Entity | Pure Java classes | Pure Java classes |
| **Use Cases** | Implicit (CRUD ops) | Explicit (named use cases) | Explicit (named use cases) |
| **Dependency Direction** | Outward (depends on DB) | Inward (depends on abstraction) | Inward (ports & adapters) |
| **Scalability** | Medium | High | High |
| **Business Rule Isolation** | Poor | Excellent | Excellent |
| **Changing DB Framework** | Difficult | Easy | Easy |
| **Adding New Adapter (API, CLI, etc)** | Hard | Easy | Easy |
| **Testing Business Logic** | Requires mocking Spring | Just test domain objects | Just test core |

---

## PART 5: REFACTORING STEPS (Current → Clean)

### Step 1: Create Pure Domain Objects
```java
// BEFORE (Current)
@Entity
@Table(name = "products")
public class Product implements DBEntity { ... }

// AFTER (Clean)
public class Product {
    private final ProductId id;
    private final ProductName name;
    private final ProductBarcode barcode;
    private final ProductPrice price;
    private final Stock stock;
    private final ProductCategory category;
    
    // Pure business logic (no framework annotations)
    public boolean canReduceStock(int quantity) {
        return stock.canReduce(quantity);
    }
    
    public void reduceStock(int quantity) throws InsufficientStockException {
        stock.reduce(quantity);
    }
}
```

### Step 2: Create Value Objects
```java
// NEW: Value object for name
public class ProductName {
    private final String value;
    
    public ProductName(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidProductNameException("Name cannot be empty");
        }
        if (value.length() > 200) {
            throw new InvalidProductNameException("Name too long");
        }
        this.value = value;
    }
    
    public String getValue() { return value; }
}

// NEW: Value object for stock
public class Stock {
    private final int current;
    private final int critical;
    
    public boolean canReduce(int quantity) {
        return (current - quantity) >= critical;
    }
    
    public void reduce(int quantity) {
        if (!canReduce(quantity)) {
            throw new InsufficientStockException(...);
        }
        // Update stock
    }
}
```

### Step 3: Define Repository Interface (No JPA!)
```java
// NEW: Interface in domain layer
public interface ProductRepository {
    void save(Product product) throws DuplicateProductException;
    Optional<Product> findById(ProductId id);
    Optional<Product> findByBarcode(ProductBarcode barcode);
    Page<Product> findAll(ProductFilter filter, Pageable pageable);
    void delete(ProductId id);
}

// KEEP: JPA implementation in infrastructure layer
@Repository
public class ProductRepositoryJpaImpl implements ProductRepository {
    @Autowired
    private ProductJpaRepository jpaRepository;
    
    @Autowired
    private ProductJpaMapper mapper;
    
    @Override
    public void save(Product product) {
        ProductJpaEntity jpaEntity = mapper.toJpaEntity(product);
        jpaRepository.save(jpaEntity);
    }
    // ... implement other methods
}
```

### Step 4: Create Explicit Use Cases
```java
// NEW: Use case interface
public interface CreateProductUseCase {
    ProductResponse execute(CreateProductRequest request) 
        throws DuplicateProductException, CategoryNotFoundException;
}

// NEW: Use case implementation
@Service
public class CreateProductUseCaseImpl implements CreateProductUseCase {
    private final ProductRepository productRepository;
    private final ProductFactory productFactory;
    private final CategoryService categoryService;
    
    @Autowired
    public CreateProductUseCaseImpl(
        ProductRepository productRepository,
        ProductFactory productFactory,
        CategoryService categoryService
    ) {
        this.productRepository = productRepository;
        this.productFactory = productFactory;
        this.categoryService = categoryService;
    }
    
    @Override
    public ProductResponse execute(CreateProductRequest request) {
        // 1. Validate input
        if (request.getBarcode() == null || request.getBarcode().isBlank()) {
            throw new InvalidProductException("Barcode required");
        }
        
        // 2. Check for duplicates
        ProductBarcode barcode = new ProductBarcode(request.getBarcode());
        if (productRepository.findByBarcode(barcode).isPresent()) {
            throw new DuplicateProductException("Product already exists");
        }
        
        // 3. Load category (if specified)
        ProductCategory category = null;
        if (request.getCategoryCode() != null) {
            category = categoryService.getByCategoryCode(request.getCategoryCode())
                .orElseThrow(() -> new CategoryNotFoundException(...));
        }
        
        // 4. Create product using factory
        Product product = productFactory.create(
            new ProductName(request.getName()),
            barcode,
            new ProductPrice(request.getPrice()),
            category
        );
        
        // 5. Save to repository
        productRepository.save(product);
        
        // 6. Return response
        return new ProductResponse(
            product.getId().getValue(),
            product.getName().getValue(),
            product.getBarcode().getValue(),
            product.getPrice().getValue()
        );
    }
}
```

### Step 5: Create Focused Controllers
```java
// BEFORE (Current)
@RestController
@RequestMapping("/data/products")
public class DataProductsController {
    @PostMapping
    @PreAuthorize("hasAuthority('products:create')")
    public void create(@Valid @RequestBody ProductPojo input) {
        crudService.create(input);
    }
    // ... all 5 CRUD operations in one controller
}

// AFTER (Clean)
@RestController
@RequestMapping("/api/products")
public class CreateProductController {
    private final CreateProductUseCase createProductUseCase;
    
    @Autowired
    public CreateProductController(CreateProductUseCase createProductUseCase) {
        this.createProductUseCase = createProductUseCase;
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('products:create')")
    public ResponseEntity<ProductResponse> create(
        @Valid @RequestBody CreateProductRequest request
    ) {
        try {
            ProductResponse response = createProductUseCase.execute(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DuplicateProductException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (CategoryNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}

// NEW: Separate controller for updates
@RestController
@RequestMapping("/api/products")
public class UpdateProductController {
    private final UpdateProductUseCase updateProductUseCase;
    
    @Autowired
    public UpdateProductController(UpdateProductUseCase updateProductUseCase) {
        this.updateProductUseCase = updateProductUseCase;
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('products:update')")
    public ResponseEntity<ProductResponse> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductResponse response = updateProductUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }
}
```

### Step 6: Separate Entity Mapping
```java
// NEW: Mapper between domain Product and JPA ProductEntity
@Component
public class ProductJpaMapper {
    
    public ProductJpaEntity toJpaEntity(Product product) {
        return ProductJpaEntity.builder()
            .productId(product.getId().getValue())
            .productName(product.getName().getValue())
            .productCode(product.getBarcode().getValue())
            .productPrice(product.getPrice().getValue())
            .productStockCurrent(product.getStock().getCurrent())
            .productStockCritical(product.getStock().getCritical())
            .categoryId(product.getCategory() != null ? 
                product.getCategory().getId() : null)
            .build();
    }
    
    public Product toDomainEntity(ProductJpaEntity jpaEntity) {
        ProductCategory category = jpaEntity.getCategory() != null ?
            new ProductCategory(...) : null;
            
        return new Product(
            new ProductId(jpaEntity.getProductId()),
            new ProductName(jpaEntity.getProductName()),
            new ProductBarcode(jpaEntity.getProductCode()),
            new ProductPrice(jpaEntity.getProductPrice()),
            new Stock(jpaEntity.getProductStockCurrent(), 
                      jpaEntity.getProductStockCritical()),
            category
        );
    }
}
```

---

## PART 6: BENEFITS OF CLEAN/HEXAGONAL ARCHITECTURE

### For the Products Domain:

| Benefit | Current State | Clean Architecture |
|---------|---------------|-------------------|
| **Testing CRUD** | Need mock DB, Spring context | Pure JUnit test |
| **Add GraphQL API** | Modify existing controller | Add new adapter (no core changes) |
| **Change to MongoDB** | Rewrite repository, entity mappings | Only implement JpaImpl differently |
| **Implement caching** | Modify service layer | Add adapter wrapping repository |
| **Extract business rule** | Scattered in service | Single domain method |
| **Understand domain** | Read through Spring annotations | Read pure Java classes |
| **Reuse logic** | Must instantiate services (Spring) | Instantiate domain objects directly |
| **Verify stock rules** | Need DB setup for tests | Test `Stock` class directly |

---

## PART 7: MIGRATION ROADMAP

### Phase 1: Prepare (Week 1-2)
- [ /] Create domain package structure
- [ /] Create pure domain entities (Product, ProductCategory, ProductImage)
- [ ] Create value objects (ProductName, ProductBarcode, ProductPrice, Stock)
- [ ] Create domain exceptions

### Phase 2: Business Logic (Week 2-3)
- [ ] Create repository interface in domain
- [ ] Create use case interfaces
- [ ] Implement use cases
- [ ] Create domain services (ProductValidator, StockAllocator)

### Phase 3: Adapters (Week 3-4)
- [ ] Create input DTOs (CreateProductRequest, etc.)
- [ ] Create output DTOs (ProductResponse)
- [ ] Create mappers (DTO ↔ Domain)
- [ ] Implement JPA adapter (ProductRepositoryJpaImpl)
- [ ] Implement external service adapters

### Phase 4: Presentation (Week 4-5)
- [ ] Split monolithic controller into focused controllers
- [ ] Update controllers to use use cases
- [ ] Update error handling
- [ ] Update security annotations

### Phase 5: Testing (Week 5-6)
- [ ] Unit tests for domain entities
- [ ] Unit tests for use cases
- [ ] Integration tests for adapters
- [ ] Controller tests

### Phase 6: Refactor Other Domains (Week 6+)
- [ ] Apply same pattern to Categories
- [ ] Apply same pattern to Images
- [ ] Apply same pattern to Orders

---

## SUMMARY

| Aspect | Current | Clean/Hexagonal |
|--------|---------|-----------------|
| **Complexity** | Low entry, high maintenance | High entry, low maintenance |
| **Testability** | Medium | High |
| **Framework Independence** | Low | High |
| **Business Logic Visibility** | Hidden | Crystal clear |
| **Scalability** | Limited | Excellent |
| **Team Onboarding** | Fast initial, hard after** | Slower initial, faster after |

**The choice depends on:**
- Project complexity (start simple → grow to clean)
- Team experience (experienced teams → clean)
- Long-term maintenance goals
- Business domain complexity
