# 3-Week Clean Architecture Refactoring Completion Plan
## Product Domain: Isolating Business Rules from Framework & Persistence

**Project:** Trébol Backend Clean Architecture Migration  
**Scope:** Product Domain (GET, LIST, CREATE, UPDATE, DELETE)  
**Timeline:** 3 weeks  
**Target Deliverables:** Presentation (5-7 min), Report (5-8 pages), Thesis (10-15 pages)  
**Focus Areas:** Implementations, Challenges, Design Decisions, Solutions, Testability  

---

## OVERVIEW: 3-Week Structure

| Week | Phase | Primary Focus | Key Deliverable |
|------|-------|---------------|-----------------|
| **Week 1** | **Implementation Sprint** | Complete controller wiring, implement write operations | All 5 HTTP endpoints working (GET/LIST/CREATE/UPDATE/DELETE) |
| **Week 2** | **Testing Sprint** | Build comprehensive test suite (50-75 tests) | All tests passing, coverage metrics ready |
| **Week 3** | **Documentation Sprint** | Create presentation, report, thesis sections | All 3 documents complete and polished |

---

## WEEK 1: Implementation Sprint (Days 1-5)

### Goal
Wire controllers, implement all use cases, populate command objects. **Result:** All 5 HTTP endpoints functional with new architecture.

### Day 1: Wire GET Single Endpoint (CRITICAL PATH)
**Tasks:**
1. Open [ProductController.java](src/main/java/org/trebol/product/adapter/inbound/web/ProductController.java)
2. Add `@GetMapping("/{id}")` endpoint:
   ```java
   @GetMapping("/{id}")
   public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
       ProductResult result = productApplicationService.execute(new GetProductQuery(id));
       return ResponseEntity.ok(productWebMapper.toResponse(result));
   }
   ```
3. Ensure `@Autowired` for `ProductApplicationService` and `ProductWebMapper`
4. Test via Thunder Client: `GET http://localhost:8080/product-module/1`
5. Verify response format matches `ProductResponse` DTO

**Validation:**
- [ ] Single product fetch returns 200 with correct data
- [ ] 404 returned for nonexistent product
- [ ] Response structure: `{ id, code, name, price, status }`

**Challenge to Document:**
- How to convert domain value objects to JSON response (mapping layer)
- Error handling (ProductNotFoundException → 404)

---

### Day 2: Wire GET List Endpoint + Complete ProductWebMapper (CRITICAL PATH)
**Tasks:**
1. Implement [ProductWebMapper.java](src/main/java/org/trebol/product/adapter/inbound/web/ProductWebMapper.java):
   ```java
   public ListProductsQuery toQuery(Map<String, String> requestParams, int pageIndex, int pageSize) {
       return new ListProductsQuery(pageIndex, pageSize, requestParams);
   }
   
   public ProductResponse toResponse(ProductResult result) {
       return new ProductResponse(result.id(), result.code(), result.name(), 
                                  result.price(), result.status());
   }
   
   public PagedProductsResponse toPagedResponse(PagedProductResult pagedResult) {
       List<ProductResponse> items = pagedResult.items().stream()
           .map(this::toResponse)
           .collect(Collectors.toList());
       return new PagedProductsResponse(items, pagedResult.totalCount());
   }
   ```
2. Add `@GetMapping` (LIST) endpoint to ProductController:
   ```java
   @GetMapping
   public ResponseEntity<PagedProductsResponse> listProducts(
           @RequestParam(defaultValue = "0") int pageIndex,
           @RequestParam(defaultValue = "10") int pageSize,
           @RequestParam Map<String, String> requestParams) {
       PagedProductResult result = productApplicationService.execute(
           productWebMapper.toQuery(requestParams, pageIndex, pageSize));
       return ResponseEntity.ok(productWebMapper.toPagedResponse(result));
   }
   ```
3. Test via Thunder Client:
   - `GET http://localhost:8080/product-module?pageIndex=0&pageSize=10`
   - `GET http://localhost:8080/product-module?pageIndex=0&pageSize=10&name=Laptop`
   - `GET http://localhost:8080/product-module?pageIndex=0&pageSize=10&name=Laptop&sort=price:asc`
4. Verify pagination, filtering, sorting all work

**Validation:**
- [ ] List returns paginated results with total count
- [ ] Filtering by name/code/barcode works
- [ ] Sorting by id/name/price works
- [ ] Default page size and index applied correctly

**Challenge to Document:**
- Dynamic query building from request parameters
- Handling optional filter parameters
- Sorting field name mapping (request param → JPA property)

---

### Day 3: Populate Command Objects + Implement CreateProductUseCase
**Tasks:**
1. Populate [CreateProductCommand.java](src/main/java/org/trebol/product/application/command/CreateProductCommand.java):
   ```java
   public record CreateProductCommand(
       String code,
       String name,
       BigDecimal price,
       boolean isActive
   ) {
       public CreateProductCommand {
           if (code == null || code.isBlank()) {
               throw new IllegalArgumentException("Code cannot be blank");
           }
           if (name == null || name.isBlank()) {
               throw new IllegalArgumentException("Name cannot be blank");
           }
           if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
               throw new IllegalArgumentException("Price must be non-negative");
           }
       }
   }
   ```

2. Populate [UpdateProductCommand.java](src/main/java/org/trebol/product/application/command/UpdateProductCommand.java):
   ```java
   public record UpdateProductCommand(
       Long id,
       String name,
       BigDecimal price,
       boolean isActive
   ) {
       public UpdateProductCommand {
           if (id == null || id <= 0) {
               throw new IllegalArgumentException("ID must be positive");
           }
           if (name != null && name.isBlank()) {
               throw new IllegalArgumentException("Name cannot be blank");
           }
           if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
               throw new IllegalArgumentException("Price must be non-negative");
           }
       }
   }
   ```

3. Populate [DeleteProductCommand.java](src/main/java/org/trebol/product/application/command/DeleteProductCommand.java):
   ```java
   public record DeleteProductCommand(Long id) {
       public DeleteProductCommand {
           if (id == null || id <= 0) {
               throw new IllegalArgumentException("ID must be positive");
           }
       }
   }
   ```

4. Implement `execute(CreateProductCommand)` in [ProductApplicationService.java](src/main/java/org/trebol/product/application/ProductApplicationService.java):
   ```java
   @Override
   public ProductResult execute(CreateProductCommand command) {
       // 1. Check if code already exists
       Optional<ProductAggregate> existingByCode = 
           productRepository.findByCode(command.code());
       if (existingByCode.isPresent()) {
           throw new ProductCodeAlreadyExistsException(command.code());
       }
       
       // 2. Create aggregate with value objects
       ProductAggregate product = new ProductAggregate(
           null, // ID assigned by DB
           new ProductCode(command.code()),
           new ProductName(command.name()),
           new ProductPrice(command.price()),
           ProductStatus.fromBoolean(command.isActive())
       );
       
       // 3. Save via port
       ProductAggregate saved = productRepository.save(product);
       
       // 4. Return result
       return productApplicationMapper.toResult(saved);
   }
   ```

**Validation:**
- [ ] CreateProductCommand validates all fields in compact constructor
- [ ] Duplicate code check works (throws ProductCodeAlreadyExistsException)
- [ ] New product saved to database
- [ ] Returned result has ID assigned from DB

**Challenge to Document:**
- Value object wrapping (command primitives → domain value objects)
- Business rule: code uniqueness (moved from DB constraint to domain logic)
- Result mapping after database persistence

---

### Day 4: Implement UpdateProductUseCase + DELETE
**Tasks:**
1. Implement `execute(UpdateProductCommand)` in ProductApplicationService:
   ```java
   @Override
   public ProductResult execute(UpdateProductCommand command) {
       // 1. Load existing aggregate
       ProductAggregate product = productRepository.findById(command.id())
           .orElseThrow(() -> new ProductNotFoundException(command.id()));
       
       // 2. Apply mutations (if field provided)
       if (command.name() != null) {
           product.updateName(new ProductName(command.name()));
       }
       if (command.price() != null) {
           product.updatePrice(new ProductPrice(command.price()));
       }
       product.updateStatus(ProductStatus.fromBoolean(command.isActive()));
       
       // 3. Save via port
       ProductAggregate updated = productRepository.save(product);
       
       // 4. Return result
       return productApplicationMapper.toResult(updated);
   }
   ```

2. Implement `execute(DeleteProductCommand)` in ProductApplicationService:
   ```java
   @Override
   public void execute(DeleteProductCommand command) {
       // 1. Verify product exists
       productRepository.findById(command.id())
           .orElseThrow(() -> new ProductNotFoundException(command.id()));
       
       // 2. Delete
       productRepository.deleteById(command.id());
   }
   ```

3. Add command object conversions to ProductWebMapper:
   ```java
   public CreateProductCommand toCreateCommand(ProductRequest request) {
       return new CreateProductCommand(
           request.code(),
           request.name(),
           request.price(),
           request.isActive()
       );
   }
   
   public UpdateProductCommand toUpdateCommand(Long id, ProductRequest request) {
       return new UpdateProductCommand(
           id,
           request.name(),
           request.price(),
           request.isActive()
       );
   }
   
   public DeleteProductCommand toDeleteCommand(Long id) {
       return new DeleteProductCommand(id);
   }
   ```

**Validation:**
- [ ] UpdateProductCommand validates nullable fields correctly
- [ ] Update applies only provided fields (partial updates)
- [ ] ProductNotFoundException thrown for missing product
- [ ] Delete succeeds and product no longer queryable

**Challenge to Document:**
- Partial updates (nullable command fields)
- Ensuring business rules stay in domain (not controller)
- Transaction safety (read, mutate, persist)

---

### Day 5: Wire All 5 Controller Endpoints + Manual Testing
**Tasks:**
1. Complete [ProductController.java](src/main/java/org/trebol/product/adapter/inbound/web/ProductController.java) with all endpoints:
   ```java
   @PostMapping
   public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest request) {
       CreateProductCommand command = productWebMapper.toCreateCommand(request);
       ProductResult result = productApplicationService.execute(command);
       return ResponseEntity.status(201).body(productWebMapper.toResponse(result));
   }
   
   @PutMapping("/{id}")
   public ResponseEntity<ProductResponse> updateProduct(
           @PathVariable Long id, 
           @RequestBody ProductRequest request) {
       UpdateProductCommand command = productWebMapper.toUpdateCommand(id, request);
       ProductResult result = productApplicationService.execute(command);
       return ResponseEntity.ok(productWebMapper.toResponse(result));
   }
   
   @DeleteMapping("/{id}")
   public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
       DeleteProductCommand command = productWebMapper.toDeleteCommand(id);
       productApplicationService.execute(command);
       return ResponseEntity.noContent().build();
   }
   ```

2. Add exception handlers to [ProductController.java](src/main/java/org/trebol/product/adapter/inbound/web/ProductController.java):
   ```java
   @ExceptionHandler(ProductNotFoundException.class)
   public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException e) {
       return ResponseEntity.status(404)
           .body(new ErrorResponse("Product not found: " + e.getMessage()));
   }
   
   @ExceptionHandler(ProductCodeAlreadyExistsException.class)
   public ResponseEntity<ErrorResponse> handleCodeExists(ProductCodeAlreadyExistsException e) {
       return ResponseEntity.status(409)
           .body(new ErrorResponse("Code already exists: " + e.getMessage()));
   }
   
   @ExceptionHandler(IllegalArgumentException.class)
   public ResponseEntity<ErrorResponse> handleValidation(IllegalArgumentException e) {
       return ResponseEntity.status(400)
           .body(new ErrorResponse("Validation error: " + e.getMessage()));
   }
   ```

3. Create Thunder Client collection with test requests:
   - POST /product-module (create)
   - GET /product-module/{id} (read)
   - GET /product-module?pageIndex=0&pageSize=10&name=test (list)
   - PUT /product-module/{id} (update)
   - DELETE /product-module/{id} (delete)

4. Test end-to-end flows:
   - Create product → Get it → Update it → Delete it
   - List products with filters
   - Verify error responses (404, 409, 400)

5. Build project: `mvn clean install`

**Validation:**
- [ ] All 5 endpoints return correct HTTP status codes
- [ ] Request/response bodies match DTO structure
- [ ] Exception handlers return proper error responses
- [ ] Build succeeds with no errors

**Week 1 Checkpoint:**
- ✅ All 5 HTTP endpoints implemented and tested
- ✅ Web mapper complete (request→command, result→response)
- ✅ All 5 use cases implemented (GET, LIST, CREATE, UPDATE, DELETE)
- ✅ Command objects populated with validation
- ✅ Exception handling in place
- ✅ Manual testing confirms all flows work

---

## WEEK 2: Testing Sprint (Days 6-10)

### Goal
Build comprehensive test suite (~60-75 tests) across 4 layers. **Result:** All tests passing, coverage metrics ready, regression detection enabled.

### Test Pyramid Target
```
                    👑
                Controller Tests (5-10 tests)
                        |
                  Adapter Tests (15-20 tests)
                    /         \
            Application Tests  Database Tests
            (10-15 tests)      (with TestContainers)
                    |
            Domain Tests (25-30 tests)
         (Value Objects + Aggregate)
```

### Day 6: Domain Layer Tests (Pure Java, No Framework)

**Target:** 25-30 tests for value objects and aggregate  
**Files to Create:**
- `src/test/java/org/trebol/product/domain/vo/ProductCodeTest.java`
- `src/test/java/org/trebol/product/domain/vo/ProductNameTest.java`
- `src/test/java/org/trebol/product/domain/vo/ProductPriceTest.java`
- `src/test/java/org/trebol/product/domain/vo/ProductIdTest.java`
- `src/test/java/org/trebol/product/domain/ProductAggregateTest.java`

**Sample Test Structure (ProductNameTest):**
```java
class ProductNameTest {
    @Test
    void shouldCreateValidProductName() {
        ProductName name = new ProductName("Laptop");
        assertEquals("Laptop", name.value());
    }
    
    @Test
    void shouldThrowOnBlankName() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ProductName("   "));
    }
    
    @Test
    void shouldThrowOnNullName() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ProductName(null));
    }
    
    @Test
    void shouldThrowOnExcessiveLength() {
        String tooLong = "a".repeat(256);
        assertThrows(IllegalArgumentException.class, 
            () -> new ProductName(tooLong));
    }
    
    @Test
    void shouldTrimWhitespace() {
        ProductName name = new ProductName("  Laptop  ");
        assertEquals("Laptop", name.value());
    }
}
```

**Sample Test Structure (ProductAggregateTest):**
```java
class ProductAggregateTest {
    @Test
    void shouldCreateProductAggregate() {
        ProductAggregate product = new ProductAggregate(
            1L, 
            new ProductCode("LAP001"),
            new ProductName("Laptop"),
            new ProductPrice(new BigDecimal("999.99")),
            ProductStatus.ACTIVE
        );
        assertEquals(1L, product.getId());
        assertEquals("LAP001", product.getCode().value());
    }
    
    @Test
    void shouldUpdateProductName() {
        ProductAggregate product = createDefaultProduct();
        product.updateName(new ProductName("Premium Laptop"));
        assertEquals("Premium Laptop", product.getName().value());
    }
    
    @Test
    void shouldUpdateProductPrice() {
        ProductAggregate product = createDefaultProduct();
        product.updatePrice(new ProductPrice(new BigDecimal("1299.99")));
        assertEquals(new BigDecimal("1299.99"), product.getPrice().value());
    }
    
    @Test
    void shouldUpdateProductStatus() {
        ProductAggregate product = createDefaultProduct();
        product.updateStatus(ProductStatus.INACTIVE);
        assertEquals(ProductStatus.INACTIVE, product.getStatus());
    }
    
    private ProductAggregate createDefaultProduct() {
        return new ProductAggregate(
            1L,
            new ProductCode("LAP001"),
            new ProductName("Laptop"),
            new ProductPrice(new BigDecimal("999.99")),
            ProductStatus.ACTIVE
        );
    }
}
```

**What to Test:**
- ✅ Value object construction (valid cases)
- ✅ Value object validation (invalid inputs)
- ✅ Value object equality and hashing
- ✅ Aggregate creation
- ✅ Aggregate mutations (updateX methods)
- ✅ Business rules enforcement (e.g., negative price rejected)

**Validation:**
- [ ] All value object tests pass (20 tests)
- [ ] All aggregate tests pass (5-10 tests)
- [ ] No framework dependencies needed

---

### Day 7: Application Layer Tests (Use Cases with Mocks)

**Target:** 10-15 tests for application service  
**File to Create:** `src/test/java/org/trebol/product/application/ProductApplicationServiceTest.java`

**Sample Test Structure:**
```java
class ProductApplicationServiceTest {
    
    @Mock
    ProductRepository productRepository;
    
    @Mock
    ProductApplicationMapper mapper;
    
    ProductApplicationService service;
    
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new ProductApplicationService(productRepository, mapper);
    }
    
    // GET TESTS
    @Test
    void shouldGetProductById() {
        // Given
        Long productId = 1L;
        ProductAggregate aggregate = createDefaultAggregate();
        ProductResult expectedResult = new ProductResult(1L, "LAP001", "Laptop", 
                                                         new BigDecimal("999.99"), "ACTIVE");
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(aggregate));
        when(mapper.toResult(aggregate)).thenReturn(expectedResult);
        
        // When
        ProductResult result = service.execute(new GetProductQuery(productId));
        
        // Then
        assertEquals(expectedResult, result);
        verify(productRepository).findById(productId);
    }
    
    @Test
    void shouldThrowWhenProductNotFound() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(ProductNotFoundException.class, 
            () -> service.execute(new GetProductQuery(999L)));
    }
    
    // LIST TESTS
    @Test
    void shouldListProductsWithPagination() {
        // Given
        List<ProductAggregate> aggregates = List.of(createDefaultAggregate());
        Page<ProductAggregate> page = new PageImpl<>(aggregates);
        List<ProductResult> results = List.of(
            new ProductResult(1L, "LAP001", "Laptop", new BigDecimal("999.99"), "ACTIVE")
        );
        PagedProductResult expectedResult = new PagedProductResult(results, 1L);
        
        when(productRepository.findAll(any())).thenReturn(page);
        when(mapper.toResult(any())).thenReturn(results.get(0));
        
        // When
        PagedProductResult result = service.execute(
            new ListProductsQuery(0, 10, Map.of()));
        
        // Then
        assertEquals(1, result.items().size());
        assertEquals(1L, result.totalCount());
    }
    
    // CREATE TESTS
    @Test
    void shouldCreateProduct() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
            "LAP002", "Gaming Laptop", new BigDecimal("1499.99"), true
        );
        ProductAggregate savedAggregate = createDefaultAggregate();
        ProductResult expectedResult = new ProductResult(2L, "LAP002", "Gaming Laptop", 
                                                         new BigDecimal("1499.99"), "ACTIVE");
        
        when(productRepository.findByCode("LAP002")).thenReturn(Optional.empty());
        when(productRepository.save(any())).thenReturn(savedAggregate);
        when(mapper.toResult(savedAggregate)).thenReturn(expectedResult);
        
        // When
        ProductResult result = service.execute(command);
        
        // Then
        assertEquals(expectedResult, result);
        verify(productRepository).save(any());
    }
    
    @Test
    void shouldThrowWhenCodeAlreadyExists() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
            "LAP001", "Laptop", new BigDecimal("999.99"), true
        );
        ProductAggregate existing = createDefaultAggregate();
        
        when(productRepository.findByCode("LAP001")).thenReturn(Optional.of(existing));
        
        // When & Then
        assertThrows(ProductCodeAlreadyExistsException.class, 
            () -> service.execute(command));
    }
    
    // UPDATE TESTS
    @Test
    void shouldUpdateProduct() {
        // Given
        UpdateProductCommand command = new UpdateProductCommand(
            1L, "Premium Laptop", new BigDecimal("1199.99"), true
        );
        ProductAggregate existing = createDefaultAggregate();
        ProductAggregate updated = createDefaultAggregate();
        ProductResult expectedResult = new ProductResult(1L, "LAP001", "Premium Laptop", 
                                                         new BigDecimal("1199.99"), "ACTIVE");
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(updated);
        when(mapper.toResult(updated)).thenReturn(expectedResult);
        
        // When
        ProductResult result = service.execute(command);
        
        // Then
        assertEquals(expectedResult, result);
        verify(productRepository).save(any());
    }
    
    // DELETE TESTS
    @Test
    void shouldDeleteProduct() {
        // Given
        DeleteProductCommand command = new DeleteProductCommand(1L);
        ProductAggregate existing = createDefaultAggregate();
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        
        // When
        service.execute(command);
        
        // Then
        verify(productRepository).deleteById(1L);
    }
    
    @Test
    void shouldThrowWhenDeletingNonexistent() {
        // Given
        DeleteProductCommand command = new DeleteProductCommand(999L);
        
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(ProductNotFoundException.class, 
            () -> service.execute(command));
    }
    
    private ProductAggregate createDefaultAggregate() {
        return new ProductAggregate(
            1L,
            new ProductCode("LAP001"),
            new ProductName("Laptop"),
            new ProductPrice(new BigDecimal("999.99")),
            ProductStatus.ACTIVE
        );
    }
}
```

**What to Test:**
- ✅ GET by ID (success and not found)
- ✅ LIST with pagination
- ✅ CREATE (success and code already exists)
- ✅ UPDATE (success and not found)
- ✅ DELETE (success and not found)
- ✅ Business rule enforcement at application level

**Validation:**
- [ ] All application tests pass (10-15 tests)
- [ ] All mocks properly configured
- [ ] Business rules tested at service level

---

### Day 8: Adapter Persistence Tests (TestContainers + Real MariaDB)

**Target:** 15-20 tests for ProductRepositoryAdapter  
**File to Create:** `src/test/java/org/trebol/product/adapter/outbound/persistence/ProductRepositoryAdapterTest.java`

**Setup (with TestContainers):**
```java
@SpringBootTest
@Testcontainers
class ProductRepositoryAdapterTest {
    
    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>(DockerImageName.parse("mariadb:latest"));
    
    @DynamicPropertySource
    static void mariadbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
    }
    
    @Autowired
    ProductRepositoryAdapter productRepository;
    
    @Autowired
    ProductJpaRepository jpaRepository;
    
    @BeforeEach
    void cleanup() {
        jpaRepository.deleteAll();
    }
    
    // SAVE TESTS
    @Test
    void shouldSaveNewProduct() {
        // Given
        ProductAggregate product = new ProductAggregate(
            null,
            new ProductCode("LAP001"),
            new ProductName("Laptop"),
            new ProductPrice(new BigDecimal("999.99")),
            ProductStatus.ACTIVE
        );
        
        // When
        ProductAggregate saved = productRepository.save(product);
        
        // Then
        assertNotNull(saved.getId());
        assertTrue(jpaRepository.existsById(saved.getId()));
    }
    
    // FIND BY ID TESTS
    @Test
    void shouldFindProductById() {
        // Given
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setCode("LAP001");
        entity.setName("Laptop");
        entity.setPrice(new BigDecimal("999.99"));
        entity.setActive(true);
        ProductJpaEntity saved = jpaRepository.save(entity);
        
        // When
        Optional<ProductAggregate> found = productRepository.findById(saved.getId());
        
        // Then
        assertTrue(found.isPresent());
        assertEquals("LAP001", found.get().getCode().value());
    }
    
    @Test
    void shouldReturnEmptyWhenNotFound() {
        // When
        Optional<ProductAggregate> found = productRepository.findById(999L);
        
        // Then
        assertTrue(found.isEmpty());
    }
    
    // FIND BY CODE TESTS
    @Test
    void shouldFindProductByCode() {
        // Given
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setCode("LAP001");
        entity.setName("Laptop");
        entity.setPrice(new BigDecimal("999.99"));
        entity.setActive(true);
        jpaRepository.save(entity);
        
        // When
        Optional<ProductAggregate> found = productRepository.findByCode("LAP001");
        
        // Then
        assertTrue(found.isPresent());
        assertEquals("Laptop", found.get().getName().value());
    }
    
    // FIND ALL WITH FILTERING TESTS
    @Test
    void shouldFindAllWithoutFilters() {
        // Given
        createTestProducts(3);
        
        // When
        Page<ProductAggregate> page = productRepository.findAll(
            new ListProductsQuery(0, 10, Map.of()));
        
        // Then
        assertEquals(3, page.getTotalElements());
    }
    
    @Test
    void shouldFindAllWithPagination() {
        // Given
        createTestProducts(25);
        
        // When
        Page<ProductAggregate> page = productRepository.findAll(
            new ListProductsQuery(0, 10, Map.of()));
        
        // Then
        assertEquals(25, page.getTotalElements());
        assertEquals(10, page.getContent().size());
        assertEquals(3, page.getTotalPages());
    }
    
    @Test
    void shouldFilterByName() {
        // Given
        createTestProduct("LAP001", "Gaming Laptop");
        createTestProduct("MON001", "Monitor");
        
        // When
        Page<ProductAggregate> page = productRepository.findAll(
            new ListProductsQuery(0, 10, Map.of("name", "Gaming")));
        
        // Then
        assertEquals(1, page.getTotalElements());
        assertEquals("Gaming Laptop", page.getContent().get(0).getName().value());
    }
    
    @Test
    void shouldSortByPrice() {
        // Given
        createTestProduct("LAP001", "Laptop", new BigDecimal("1000"));
        createTestProduct("LAP002", "Cheap Laptop", new BigDecimal("500"));
        createTestProduct("LAP003", "Premium Laptop", new BigDecimal("2000"));
        
        // When
        Page<ProductAggregate> page = productRepository.findAll(
            new ListProductsQuery(0, 10, Map.of("sort", "price:asc")));
        
        // Then
        assertEquals(3, page.getTotalElements());
        assertEquals(new BigDecimal("500"), page.getContent().get(0).getPrice().value());
        assertEquals(new BigDecimal("2000"), page.getContent().get(2).getPrice().value());
    }
    
    // DELETE TESTS
    @Test
    void shouldDeleteProduct() {
        // Given
        ProductAggregate product = createAndSaveProduct();
        Long id = product.getId();
        
        // When
        productRepository.deleteById(id);
        
        // Then
        assertFalse(jpaRepository.existsById(id));
    }
    
    // HELPER METHODS
    private void createTestProducts(int count) {
        for (int i = 0; i < count; i++) {
            createTestProduct("CODE" + i, "Product " + i);
        }
    }
    
    private void createTestProduct(String code, String name) {
        createTestProduct(code, name, new BigDecimal("100.00"));
    }
    
    private void createTestProduct(String code, String name, BigDecimal price) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setCode(code);
        entity.setName(name);
        entity.setPrice(price);
        entity.setActive(true);
        jpaRepository.save(entity);
    }
    
    private ProductAggregate createAndSaveProduct() {
        ProductAggregate product = new ProductAggregate(
            null,
            new ProductCode("TEST001"),
            new ProductName("Test Product"),
            new ProductPrice(new BigDecimal("99.99")),
            ProductStatus.ACTIVE
        );
        return productRepository.save(product);
    }
}
```

**What to Test:**
- ✅ Save new product (ID assignment)
- ✅ Find by ID (success and not found)
- ✅ Find by code (success and not found)
- ✅ Find all with pagination
- ✅ Filtering by name/code
- ✅ Sorting by various fields
- ✅ Delete product
- ✅ JPA entity ↔ aggregate mapping

**Maven Dependency (pom.xml):**
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.20.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mariadb</artifactId>
    <version>1.20.1</version>
    <scope>test</scope>
</dependency>
```

**Validation:**
- [ ] All adapter tests pass (15-20 tests)
- [ ] TestContainers properly configured
- [ ] No test data bleeds between tests (cleanup in @BeforeEach)

---

### Day 9: Controller Layer Tests (MockMvc)

**Target:** 5-10 tests for HTTP contract validation  
**File to Create:** `src/test/java/org/trebol/product/adapter/inbound/web/ProductControllerTest.java`

**Sample Test Structure:**
```java
@WebMvcTest(ProductController.class)
class ProductControllerTest {
    
    @Autowired
    MockMvc mockMvc;
    
    @MockBean
    ProductApplicationService productApplicationService;
    
    @MockBean
    ProductWebMapper webMapper;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    // GET SINGLE TESTS
    @Test
    void shouldGetProductById() throws Exception {
        // Given
        Long productId = 1L;
        ProductResult result = new ProductResult(1L, "LAP001", "Laptop", 
                                                 new BigDecimal("999.99"), "ACTIVE");
        ProductResponse response = new ProductResponse(1L, "LAP001", "Laptop", 
                                                       new BigDecimal("999.99"), "ACTIVE");
        
        when(productApplicationService.execute(any(GetProductQuery.class)))
            .thenReturn(result);
        when(webMapper.toResponse(result)).thenReturn(response);
        
        // When & Then
        mockMvc.perform(get("/product-module/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.code").value("LAP001"));
    }
    
    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        // Given
        when(productApplicationService.execute(any(GetProductQuery.class)))
            .thenThrow(new ProductNotFoundException(999L));
        
        // When & Then
        mockMvc.perform(get("/product-module/999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
    
    // GET LIST TESTS
    @Test
    void shouldListProducts() throws Exception {
        // Given
        List<ProductResult> items = List.of(
            new ProductResult(1L, "LAP001", "Laptop", new BigDecimal("999.99"), "ACTIVE")
        );
        PagedProductResult result = new PagedProductResult(items, 1L);
        PagedProductsResponse response = new PagedProductsResponse(
            items.stream().map(r -> new ProductResponse(r.id(), r.code(), r.name(), r.price(), r.status()))
                 .collect(Collectors.toList()),
            1L
        );
        
        when(productApplicationService.execute(any(ListProductsQuery.class)))
            .thenReturn(result);
        when(webMapper.toPagedResponse(result)).thenReturn(response);
        
        // When & Then
        mockMvc.perform(get("/product-module?pageIndex=0&pageSize=10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.totalCount").value(1));
    }
    
    // POST TESTS
    @Test
    void shouldCreateProduct() throws Exception {
        // Given
        ProductRequest request = new ProductRequest("LAP002", "Gaming Laptop", 
                                                    new BigDecimal("1499.99"), true);
        ProductResult result = new ProductResult(2L, "LAP002", "Gaming Laptop", 
                                                 new BigDecimal("1499.99"), "ACTIVE");
        ProductResponse response = new ProductResponse(2L, "LAP002", "Gaming Laptop", 
                                                       new BigDecimal("1499.99"), "ACTIVE");
        
        when(webMapper.toCreateCommand(any())).thenReturn(
            new CreateProductCommand("LAP002", "Gaming Laptop", 
                                    new BigDecimal("1499.99"), true)
        );
        when(productApplicationService.execute(any(CreateProductCommand.class)))
            .thenReturn(result);
        when(webMapper.toResponse(result)).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/product-module")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(2));
    }
    
    // PUT TESTS
    @Test
    void shouldUpdateProduct() throws Exception {
        // Given
        ProductRequest request = new ProductRequest(null, "Premium Laptop", 
                                                    new BigDecimal("1199.99"), true);
        ProductResult result = new ProductResult(1L, "LAP001", "Premium Laptop", 
                                                 new BigDecimal("1199.99"), "ACTIVE");
        ProductResponse response = new ProductResponse(1L, "LAP001", "Premium Laptop", 
                                                       new BigDecimal("1199.99"), "ACTIVE");
        
        when(webMapper.toUpdateCommand(eq(1L), any())).thenReturn(
            new UpdateProductCommand(1L, "Premium Laptop", 
                                    new BigDecimal("1199.99"), true)
        );
        when(productApplicationService.execute(any(UpdateProductCommand.class)))
            .thenReturn(result);
        when(webMapper.toResponse(result)).thenReturn(response);
        
        // When & Then
        mockMvc.perform(put("/product-module/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Premium Laptop"));
    }
    
    // DELETE TESTS
    @Test
    void shouldDeleteProduct() throws Exception {
        // Given
        when(webMapper.toDeleteCommand(1L)).thenReturn(new DeleteProductCommand(1L));
        doNothing().when(productApplicationService).execute(any(DeleteProductCommand.class));
        
        // When & Then
        mockMvc.perform(delete("/product-module/1"))
            .andExpect(status().isNoContent());
    }
    
    @Test
    void shouldReturn404WhenDeletingNonexistent() throws Exception {
        // Given
        when(webMapper.toDeleteCommand(999L)).thenReturn(new DeleteProductCommand(999L));
        doThrow(new ProductNotFoundException(999L))
            .when(productApplicationService)
            .execute(any(DeleteProductCommand.class));
        
        // When & Then
        mockMvc.perform(delete("/product-module/999"))
            .andExpect(status().isNotFound());
    }
}
```

**What to Test:**
- ✅ GET single product (200 and 404)
- ✅ GET list products (200 with pagination)
- ✅ POST create product (201 created)
- ✅ PUT update product (200 ok)
- ✅ DELETE product (204 no content)
- ✅ Error responses (400 validation, 404 not found, 409 conflict)
- ✅ Response structure matches DTOs

**Validation:**
- [ ] All controller tests pass (5-10 tests)
- [ ] HTTP status codes correct
- [ ] JSON response structure correct
- [ ] Error handling works

---

### Day 10: Test Execution + Coverage Analysis

**Tasks:**
1. Run all tests:
   ```bash
   mvn clean test
   ```

2. Generate coverage report:
   ```bash
   mvn clean test jacoco:report
   ```

3. View coverage at `target/site/jacoco/index.html`

4. Create test summary document:

**Week 2 Checkpoint:**
- ✅ 25-30 domain tests (value objects, aggregate)
- ✅ 10-15 application tests (use cases)
- ✅ 15-20 adapter tests (persistence, filtering, pagination)
- ✅ 5-10 controller tests (HTTP contract)
- ✅ **Total: 60-75 tests, all passing**
- ✅ Code coverage: minimum 70% for business logic

---

## WEEK 3: Documentation Sprint (Days 11-15)

### Goal
Create presentation, report, and thesis sections. **Result:** All 3 documents complete and presentation-ready.

### Day 11-12: Implementation & Challenges Documentation

**Files to Create:**
1. [IMPLEMENTATION_DETAILS.md](IMPLEMENTATION_DETAILS.md)
2. [CHALLENGES_AND_SOLUTIONS.md](CHALLENGES_AND_SOLUTIONS.md)

**IMPLEMENTATION_DETAILS.md Content Structure:**

```markdown
# Implementation Details: Product Domain Clean Architecture Refactoring

## Overview
- Problem statement (3-layer architecture pain points)
- Solution (Clean/Hexagonal Architecture)
- Scope (GET, LIST, CREATE, UPDATE, DELETE operations)

## Layer Breakdown

### Domain Layer
- Value objects (ProductCode, ProductName, ProductPrice, ProductStatus, ProductId)
  - Compact constructor validation
  - Code snippet: `public record ProductName(String value) { ... }`
  - Why records: Immutability, built-in equals/hashCode, concise syntax
- Aggregate (ProductAggregate)
  - Root entity for Product cluster
  - Business rules embedded (updateX methods)
  - No framework annotations (pure Java)
- Port interface (ProductRepository)
  - Domain-owned contract
  - Inversion of control (adapter implements domain interface)
- Exceptions (ProductNotFoundException, ProductCodeAlreadyExistsException)

### Application Layer
- Use cases (GetProductUseCase, ListProductsUseCase, CreateProductUseCase, UpdateProductUseCase, DeleteProductUseCase)
  - Orchestrators of business logic
  - Delegate to domain and port
  - Code snippet: `public ProductResult execute(GetProductQuery query) { ... }`
- Query objects (GetProductQuery, ListProductsQuery)
  - Immutable query parameters
  - Record-based for conciseness
- Command objects (CreateProductCommand, UpdateProductCommand, DeleteProductCommand)
  - Immutable command parameters with validation in compact constructor
- Application mapper (ProductAggregate → ProductResult)
  - Conversion at boundary
  - Prevents domain leakage to outer layers

### Adapter - Inbound HTTP
- Controller (ProductController)
  - All 5 endpoints (@GetMapping, @PostMapping, @PutMapping, @DeleteMapping)
  - Exception handlers for ProductNotFoundException, ProductCodeAlreadyExistsException
  - Delegates to application service
- Web mapper
  - Request → Query/Command conversion
  - Result → Response conversion
- DTOs (ProductRequest, ProductResponse)
  - Simple data carriers
  - No business logic

### Adapter - Outbound Persistence
- Repository adapter (ProductRepositoryAdapter)
  - Implements domain-owned ProductRepository port
  - Dynamic filtering via JPA Specification + CriteriaBuilder
  - Sorting and pagination support
  - Code snippet: `buildSpecification(requestParams)` for dynamic queries
- JPA entity (ProductJpaEntity)
  - ORM entity separate from domain aggregate
  - @Column annotations only (framework code isolated)
- Persistence mapper
  - JpaEntity ↔ ProductAggregate conversion
  - Null-safe field handling

### Infrastructure
- ProductModuleConfiguration
  - Spring @Configuration for dependency injection
  - Bean wiring (repository adapter, mappers, service, controller)

## Code Examples (Before/After)

### GET Single Product

**BEFORE (Old Architecture):**
```java
// DataProductsController
@GetMapping("/{id}")
public Product readOne(@PathVariable Long id) {
    Product entity = productsCrudService.readOne(id);
    return entity;
}

// ProductsCrudServiceImpl
public Product readOne(Long id) {
    QProduct product = QProduct.product;
    return queryFactory.selectFrom(product)
        .where(product.id.eq(id))
        .fetchOne();
}
```

**AFTER (Clean Architecture):**
```java
// ProductController
@GetMapping("/{id}")
public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
    ProductResult result = productApplicationService.execute(new GetProductQuery(id));
    return ResponseEntity.ok(productWebMapper.toResponse(result));
}

// ProductApplicationService
public ProductResult execute(GetProductQuery query) {
    ProductAggregate product = productRepository.findById(query.id())
        .orElseThrow(() -> new ProductNotFoundException(query.id()));
    return productApplicationMapper.toResult(product);
}
```

**Why Better:**
- Clear separation of concerns (controller, use case, domain, persistence)
- Business logic (not found handling) at application level
- Testable at each layer
- Framework not leaked into domain
- Explicit query object (clearer intent)

### LIST with Filtering/Sorting

**BEFORE (QueryDSL Predicates in Service):**
```java
public List<Product> readMany(Map<String, String> requestParams) {
    QProduct product = QProduct.product;
    BooleanBuilder predicates = new BooleanBuilder();
    
    if (requestParams.containsKey("name")) {
        predicates.and(product.name.like("%" + requestParams.get("name") + "%"));
    }
    if (requestParams.containsKey("code")) {
        predicates.and(product.code.eq(requestParams.get("code")));
    }
    
    return queryFactory.selectFrom(product)
        .where(predicates)
        .fetch();
}
```

**AFTER (Adapter with Specification Pattern):**
```java
public Page<ProductAggregate> findAll(ListProductsQuery query) {
    Specification<ProductJpaEntity> spec = buildSpecification(query.requestParams());
    Pageable pageable = PageRequest.of(query.pageIndex(), query.pageSize(), 
                                       resolveSort(query.requestParams()));
    Page<ProductJpaEntity> page = jpaRepository.findAll(spec, pageable);
    return page.map(productPersistenceMapper::toAggregate);
}

private Specification<ProductJpaEntity> buildSpecification(Map<String, String> params) {
    return (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        
        if (params.containsKey("name")) {
            predicates.add(cb.like(root.get("name"), "%" + params.get("name") + "%"));
        }
        if (params.containsKey("code")) {
            predicates.add(cb.equal(root.get("code"), params.get("code")));
        }
        
        return cb.and(predicates.toArray(new Predicate[0]));
    };
}
```

**Why Better:**
- Specification pattern is standard for dynamic JPA queries
- Sorting abstraction (field name mapping handled in adapter)
- Pagination first-class citizen
- Easily testable with TestContainers

### Value Object Usage (Validation)

**BEFORE (Validation in Service/Controller):**
```java
@PostMapping
public Product create(@RequestBody ProductRequest request) {
    if (request.name() == null || request.name().isBlank()) {
        throw new IllegalArgumentException("Name cannot be blank");
    }
    if (request.price().compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("Price must be non-negative");
    }
    // ... create product
}
```

**AFTER (Validation in Domain Value Object):**
```java
public record ProductName(String value) {
    public ProductName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (value.length() > 255) {
            throw new IllegalArgumentException("Name too long");
        }
        this.value = value.trim();
    }
}

// Usage
ProductName name = new ProductName(request.name()); // Validation happens here
```

**Why Better:**
- Business rule (validation) lives where it belongs (domain)
- Invalid state impossible to create (compile-time safety)
- Reusable across layers
- Self-documenting code

## Migration Pattern: Vertical Slices with Strangler

### Slice 1: GET Single Product
1. Domain layer complete (aggregate, value objects, port)
2. Application layer complete (GetProductUseCase, GetProductQuery)
3. Persistence adapter complete (ProductRepositoryAdapter)
4. Controller wired (@GetMapping("/{id}"))
5. Old code still active but not used for this flow

### Slice 2: LIST with Pagination/Filtering
1. Domain layer: same as Slice 1
2. Application layer: ListProductsUseCase with filtering/sorting
3. Persistence adapter: Specification-based filtering
4. Controller wired (@GetMapping)
5. Partial integration with old DataProductsController (calls ListProductsUseCase)

### Slice 3-5: CREATE/UPDATE/DELETE
Similar progression, building on Slice 1-2 foundation

**Why Vertical Slices:**
- Each slice is a complete business flow
- Can be tested and validated independently
- Old code can coexist during migration
- No risk of half-migrated features
- Strangler pattern prevents big-bang rewrite failures

```
OLD ARCHITECTURE          MIGRATION PERIOD           NEW ARCHITECTURE
┌─────────────────┐      ┌──────────────────┐       ┌────────────────┐
│ DataProducts    │      │ Slice 1 (GET)    │       │ ProductModule  │
│ Controller      │  →   │ + Slice 2 (LIST) │   →   │ (all slices)   │
│ (all endpoints) │      │ + old code       │       │                │
└─────────────────┘      └──────────────────┘       └────────────────┘
```

```

**CHALLENGES_AND_SOLUTIONS.md Content Structure:**

```markdown
# Challenges Encountered & Solutions Implemented

## Challenge 1: Framework Leakage (Annotations in Domain Layer)

**Problem:**
- Domain layer should be pure Java, no Spring/JPA annotations
- But JPA @Entity, @Column, @Table were creeping into domain classes
- Result: Domain logic became coupled to persistence framework
- Testing: Domain tests would need database/ORM

**Solution:**
- Separate domain aggregate (ProductAggregate.java) from JPA entity (ProductJpaEntity.java)
- ProductAggregate is pure Java record with no annotations
- ProductJpaEntity only in adapter layer with @Entity/@Column
- Persistence mapper converts between them
- Domain layer has ZERO framework dependencies

**Code Example:**
```java
// DOMAIN (Pure Java - No Annotations)
public record ProductAggregate(
    Long id,
    ProductCode code,
    ProductName name,
    ProductPrice price,
    ProductStatus status
) {
    public void updateName(ProductName newName) { ... }
    public void updatePrice(ProductPrice newPrice) { ... }
}

// ADAPTER (Framework Isolated)
@Entity
@Table(name = "products")
public class ProductJpaEntity {
    @Id @GeneratedValue
    private Long id;
    
    @Column(name = "product_code", unique = true)
    private String code;
    
    @Column(name = "product_name")
    private String name;
    // ...
}

// MAPPER (Boundary)
public ProductAggregate toAggregate(ProductJpaEntity entity) {
    return new ProductAggregate(
        entity.getId(),
        new ProductCode(entity.getCode()),
        new ProductName(entity.getName()),
        // ...
    );
}
```

**Validation:**
- Domain tests run without Spring context (@SpringBootTest not needed)
- No database required for domain tests
- Faster test execution

---

## Challenge 2: Dynamic Query Building (Filtering & Sorting)

**Problem:**
- Controller receives arbitrary filter parameters (name, code, price range, etc.)
- Need to dynamically build JPA queries based on provided filters
- Old code used QueryDSL predicates scattered throughout service layer
- Difficult to test dynamic query building

**Solution:**
- JPA Specification pattern for composable queries
- CriteriaBuilder for dynamic predicate creation in adapter layer
- Isolated in ProductRepositoryAdapter.buildSpecification()
- Tested with TestContainers (real database)

**Code Example:**
```java
public Page<ProductAggregate> findAll(ListProductsQuery query) {
    Specification<ProductJpaEntity> spec = buildSpecification(query.requestParams());
    Pageable pageable = PageRequest.of(
        query.pageIndex(),
        query.pageSize(),
        resolveSort(query.requestParams())
    );
    Page<ProductJpaEntity> page = jpaRepository.findAll(spec, pageable);
    return page.map(productPersistenceMapper::toAggregate);
}

private Specification<ProductJpaEntity> buildSpecification(Map<String, String> params) {
    return (root, queryObj, criteriaBuilder) -> {
        List<Predicate> predicates = new ArrayList<>();
        
        // Filter by name (like match)
        if (params.containsKey("name")) {
            String nameFilter = params.get("name");
            predicates.add(criteriaBuilder.like(
                root.get("name"),
                "%" + nameFilter + "%"
            ));
        }
        
        // Filter by code (exact match)
        if (params.containsKey("code")) {
            predicates.add(criteriaBuilder.equal(
                root.get("code"),
                params.get("code")
            ));
        }
        
        // Add more filters as needed
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
}

private Sort resolveSort(Map<String, String> params) {
    if (!params.containsKey("sort")) {
        return Sort.by("id").ascending();
    }
    
    String sortParam = params.get("sort"); // format: "field:asc" or "field:desc"
    String[] parts = sortParam.split(":");
    String field = parts[0];
    String direction = parts.length > 1 ? parts[1] : "asc";
    
    Sort.Direction sortDirection = "desc".equals(direction)
        ? Sort.Direction.DESC
        : Sort.Direction.ASC;
    
    return Sort.by(sortDirection, field);
}
```

**Validation:**
- Tested with TestContainers (real MariaDB)
- Filters work: name (like), code (exact)
- Sorting works: ascending/descending
- Pagination works: correct page offsets and total counts

---

## Challenge 3: Aggregate/Entity Mapping Complexity

**Problem:**
- Domain aggregate (ProductAggregate) uses value objects (ProductCode, ProductName, etc.)
- JPA entity (ProductJpaEntity) uses raw primitives (String code, String name)
- Conversion between them required in multiple places
- Null-safety: what if DB column is NULL?
- Type safety: Java record immutability vs JPA mutability

**Solution:**
- Two separate mappers:
  1. PersistenceMapper: ProductJpaEntity ↔ ProductAggregate (in adapter)
  2. ApplicationMapper: ProductAggregate → ProductResult (boundary)
- Null-safe field handling with Optional pattern
- Compact constructor in records for fail-fast validation

**Code Example:**
```java
// PERSISTENCE MAPPER (Adapter Layer)
public ProductAggregate toAggregate(ProductJpaEntity entity) {
    return new ProductAggregate(
        entity.getId(),
        new ProductCode(entity.getCode()),        // Wrap raw value
        new ProductName(entity.getName()),         // Unwrap only what's needed
        new ProductPrice(entity.getPrice()),
        ProductStatus.fromBoolean(entity.isActive())
    );
}

public ProductJpaEntity toEntity(ProductAggregate aggregate) {
    ProductJpaEntity entity = new ProductJpaEntity();
    entity.setId(aggregate.getId());
    entity.setCode(aggregate.getCode().value());   // Unwrap value object
    entity.setName(aggregate.getName().value());
    entity.setPrice(aggregate.getPrice().value());
    entity.setActive(aggregate.getStatus() == ProductStatus.ACTIVE);
    return entity;
}

// APPLICATION MAPPER (Boundary)
public ProductResult toResult(ProductAggregate aggregate) {
    return new ProductResult(
        aggregate.getId(),
        aggregate.getCode().value(),              // Extract value for DTO
        aggregate.getName().value(),
        aggregate.getPrice().value(),
        aggregate.getStatus().name()
    );
}
```

**Validation:**
- Persistence tests verify mapping correctness
- Value object construction validates fields
- No nulls escape from mapping layer

---

## Challenge 4: Coexistence of Old and New Code

**Problem:**
- DataProductsController still active at /data/products
- DataCrudGenericController uses QueryDSL predicates (old pattern)
- ProductsCrudServiceImpl uses old readOne() implementation
- Can't delete old code yet (might break something)
- But new architecture should eventually replace old code

**Solution:**
- Strangler pattern: migrate one business flow (slice) at a time
- Slice 1: GET single (new code, old code still there but unused)
- Slice 2: LIST (new code integrated, old DataProductsController partially redirected)
- Slices 3-5: CREATE/UPDATE/DELETE (migrate when ready)
- Tests prove new code works → old code can be safely deleted

**Timeline:**
```
Week 1: Implement all 5 endpoints (new)
Week 2: Write comprehensive tests (verify new code)
Week 3: Old code marked for deletion (tests prove replacement works)
```

**Validation:**
- HTTP endpoints return same data from old and new code
- Tests pass (proving new code correct)
- No behavioral differences observed

---

## Challenge 5: Testing Multiple Layers with Different Frameworks

**Problem:**
- Domain tests need no framework (pure Java)
- Application tests need mocking (Mockito)
- Adapter tests need real database (TestContainers)
- Controller tests need Spring MVC (MockMvc)
- Each layer has different test style and tooling

**Solution:**
- 4-layer testing pyramid:
  1. Domain tests: Pure JUnit5, no framework
  2. Application tests: JUnit5 + Mockito
  3. Adapter tests: JUnit5 + TestContainers + Spring
  4. Controller tests: JUnit5 + Spring MVC + MockMvc
- Each layer tests one concern
- Lower layers (domain) run fastest
- Higher layers (controller) run slowest but fewer tests

**Code Example:**
```java
// LAYER 1: Domain (Pure Java, No Framework)
class ProductNameTest {
    @Test
    void shouldValidateNameNotBlank() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ProductName(""));
    }
}

// LAYER 2: Application (Mockito, No Framework)
class ProductApplicationServiceTest {
    @Mock
    ProductRepository productRepository;
    
    @Test
    void shouldGetProductById() {
        ProductAggregate aggregate = mock(ProductAggregate.class);
        when(productRepository.findById(1L))
            .thenReturn(Optional.of(aggregate));
        
        ProductResult result = service.execute(new GetProductQuery(1L));
        assertNotNull(result);
    }
}

// LAYER 3: Adapter (TestContainers + Spring)
@SpringBootTest
@Testcontainers
class ProductRepositoryAdapterTest {
    @Container
    static MariaDBContainer<?> mariadb = 
        new MariaDBContainer<>(DockerImageName.parse("mariadb:latest"));
    
    @Test
    void shouldFindProductInRealDatabase() {
        // Creates real database via Docker
        ProductAggregate saved = adapter.save(product);
        assertTrue(saved.getId() > 0);
    }
}

// LAYER 4: Controller (MockMvc + Spring)
@WebMvcTest(ProductController.class)
class ProductControllerTest {
    @Autowired
    MockMvc mockMvc;
    
    @Test
    void shouldReturn200ForValidRequest() throws Exception {
        mockMvc.perform(get("/product-module/1"))
            .andExpect(status().isOk());
    }
}
```

**Test Execution Time:**
```
Layer 1 (Domain):      < 100ms (25-30 tests)
Layer 2 (Application): < 500ms (10-15 tests)
Layer 3 (Adapter):     10-30s  (15-20 tests, real DB)
Layer 4 (Controller):  < 5s    (5-10 tests)
────────────────────
TOTAL:                 ~45s    (60-75 tests)
```

**Validation:**
- All tests pass independently
- Can run Layer 1-2 locally without Docker
- Can run all layers in CI/CD pipeline
- Clear feedback for each failure

---

## Challenge 6: Handling Domain Exceptions at Controller Level

**Problem:**
- ProductNotFoundException thrown in application layer
- ProductCodeAlreadyExistsException thrown in application layer
- Controller needs to convert these to HTTP status codes
- How to avoid leaking domain exceptions to API?

**Solution:**
- Domain exceptions caught in controller @ExceptionHandler
- Mapped to appropriate HTTP status codes
- Response body contains user-friendly error message
- No domain exception details exposed to client

**Code Example:**
```java
@RestController
@RequestMapping("/product-module")
public class ProductController {
    
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException e) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse("Product not found"));
    }
    
    @ExceptionHandler(ProductCodeAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCodeExists(ProductCodeAlreadyExistsException e) {
        return ResponseEntity.status(409)
            .body(new ErrorResponse("Code already exists"));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidation(IllegalArgumentException e) {
        return ResponseEntity.status(400)
            .body(new ErrorResponse("Validation error: " + e.getMessage()));
    }
}

record ErrorResponse(String message) {}
```

**Validation:**
- ProductNotFoundException → 404
- ProductCodeAlreadyExistsException → 409
- IllegalArgumentException (validation) → 400
- HTTP contract tests verify mappings

---

## Challenge 7: Database Transaction Boundaries

**Problem:**
- CREATE/UPDATE operations span multiple layers
- Need atomicity: either entire operation succeeds or fails
- Where should @Transactional go?
- What if JPA changes are needed but domain logic has side effects?

**Solution:**
- @Transactional on application service (boundary between application and adapter layers)
- Adapter layer handles transactional persistence
- Domain layer remains transaction-agnostic

**Code Example:**
```java
@Service
@Transactional
public class ProductApplicationService {
    
    public ProductResult execute(CreateProductCommand command) {
        // All of this happens in one transaction
        
        // 1. Check uniqueness (SELECT)
        productRepository.findByCode(command.code())
            .ifPresent(p -> { throw new ProductCodeAlreadyExistsException(...); });
        
        // 2. Create aggregate
        ProductAggregate product = new ProductAggregate(...);
        
        // 3. Persist (INSERT)
        ProductAggregate saved = productRepository.save(product);
        
        // 4. Return result
        return mapper.toResult(saved);
        
        // If any step fails, entire transaction rolls back
    }
}
```

**Validation:**
- Duplicate code inserts fail (transaction rolled back)
- Partial failures don't corrupt data
- Tests verify ACID properties

```

---

### Day 13: Design Decisions Documentation

**File to Create:** [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md)

```markdown
# Design Decisions & Rationale

## Decision 1: Vertical Slice Migration Over Horizontal Layer Refactoring

**Decision:**
Migrate one complete business flow (GET/LIST/CREATE/UPDATE/DELETE) at a time, each with full domain→application→adapter→controller implementation.

**Why Not Horizontal?**
- Horizontal: Refactor all controller endpoints first, then all use cases, then all persistence
- Risk: Controllers wired to non-existent use cases, incomplete business logic
- Result: Code doesn't work until final layer done

**Why Vertical Slices?**
- Each slice is independently testable end-to-end
- Can deploy working GET endpoint while CREATE/UPDATE are being built
- Old code continues to work (strangler pattern)
- Risk limited to one business flow at a time
- Clear progress visibility

**Evidence:**
- GET endpoint fully working (testable) after Day 5
- LIST endpoint working (testable) after Day 5
- Can be demonstrated to professor independently

---

## Decision 2: Domain-Owned Repository Port (Inversion of Control)

**Decision:**
ProductRepository interface defined in domain layer, implemented by adapter (persistence layer).

**Why?**
- Domain defines what it needs (contract)
- Adapter provides implementation
- Dependency direction: Adapter → Domain (not Domain → Adapter)
- Domain remains independent of persistence technology

**Example:**
```java
// DOMAIN (Independent)
public interface ProductRepository {
    ProductAggregate save(ProductAggregate product);
    Optional<ProductAggregate> findById(Long id);
    // Domain defines what it needs
}

// ADAPTER (Depends on Domain)
@Repository
public class ProductRepositoryAdapter implements ProductRepository {
    @Override
    public ProductAggregate save(ProductAggregate product) {
        // Persistence implementation
    }
}
```

**Alternative (Wrong Direction):**
```java
// This is NOT how we do it:
// ADAPTER defines repository
public interface ProductRepository { ... }

// DOMAIN depends on adapter
public class ProductApplicationService {
    @Autowired
    ProductRepository repo; // Domain now depends on adapter layer
}
```

**Consequences:**
- Domain is reusable with different persistence (swap adapter)
- Domain testable without persistence adapter
- Flexible: could use MongoDB, ElasticSearch, in-memory, etc. as adapters

---

## Decision 3: Value Objects with Compact Constructor Validation

**Decision:**
Use Java records with compact constructor for value object validation.

```java
public record ProductName(String value) {
    public ProductName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        this.value = value.trim();
    }
}
```

**Why Not Traditional Setters/Getters?**
- Getters/setters are mutable, allow invalid states
- Easy to forget validation
- More boilerplate code

**Why Records?**
- Immutability enforced by language
- Automatic equals/hashCode/toString
- Compact constructor runs on every construction
- Self-documenting (one field = one value object responsibility)

**Why Validation in Constructor?**
- Impossible to create invalid state
- Fail-fast: error at construction time
- Testable: just test constructor

**Example of Invalid State Prevention:**
```java
// BEFORE (Traditional Class)
public class ProductName {
    private String value;
    
    public ProductName(String value) {
        this.value = value; // What if blank? Not caught!
    }
    
    public void setValue(String value) {
        this.value = value; // Could set to invalid
    }
}

// AFTER (Record)
public record ProductName(String value) {
    public ProductName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(...);
        }
        this.value = value.trim();
    }
    
    // setValue() impossible - record is immutable
}
```

**Consequences:**
- Business rules cannot be violated
- Type safety at compile time
- Cleaner code

---

## Decision 4: Keep Old Code During Migration (Strangler Pattern)

**Decision:**
Don't delete DataProductsController, ProductsCrudServiceImpl, or QueryDSL code. Let new code coexist.

**Why?**
- Reduces risk of big-bang rewrite failure
- Can roll back to old code if new code has bugs
- Allows gradual migration
- Tests prove new code works → then delete old code

**Timeline:**
- Week 1: New code 100% implemented, old code still 100% active
- Week 2: Tests prove new code works correctly
- Week 3: Delete old code (tests guarantee replacement is safe)

**Alternative (Dangerous):**
```
Day 1: Delete all old controller code
Day 2: Write new controller code
Day 3-5: Debug new code with no fallback
```
Result: Production broken if new code has bugs.

**Consequences:**
- Slight code duplication temporarily
- Safer migration path
- Clear "cut-over" point (when old code deleted)

---

## Decision 5: Separate Domain Aggregate from JPA Entity

**Decision:**
ProductAggregate (domain) ≠ ProductJpaEntity (adapter). They're different classes.

**Why?**
- Domain must be framework-independent
- JPA @Entity, @Column are framework concerns
- Mapping layer converts between them

**Example:**
```java
// DOMAIN (Pure Java)
public record ProductAggregate(
    Long id,
    ProductCode code,
    ProductName name,
    ProductPrice price,
    ProductStatus status
) { ... }

// ADAPTER (JPA)
@Entity
@Table(name = "products")
public class ProductJpaEntity {
    @Id @GeneratedValue
    private Long id;
    
    @Column(name = "product_code")
    private String code;
    
    // Different structure, different concerns
}

// MAPPER
public ProductAggregate toAggregate(ProductJpaEntity entity) {
    return new ProductAggregate(
        entity.getId(),
        new ProductCode(entity.getCode()),
        // ...
    );
}
```

**Why Not One Class?**
```java
// BAD: Domain coupled to framework
@Entity
public record ProductAggregate(...) {
    @Column(name = "product_code")
    String code;
}
```

**Consequences:**
- More mapping code
- But domain remains independent
- Worth the extra code

---

## Decision 6: JPA Specification Pattern for Dynamic Queries

**Decision:**
Use JPA Specification interface (buildSpecification method) for dynamic filtering instead of QueryDSL.

**Why?**
- Standard JPA pattern (works with any Spring Data JPA repository)
- CriteriaBuilder is standard JPA API
- No external dependencies (QueryDSL adds dependency)
- Easier to understand (Criteria API vs QueryDSL DSL)

**Example:**
```java
// Specification-based (chosen)
Specification<ProductJpaEntity> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    if (params.containsKey("name")) {
        predicates.add(cb.like(root.get("name"), "%" + params.get("name") + "%"));
    }
    return cb.and(predicates.toArray(new Predicate[0]));
};

// QueryDSL-based (old code, being replaced)
BooleanBuilder predicates = new BooleanBuilder();
if (params.containsKey("name")) {
    predicates.and(product.name.like("%" + params.get("name") + "%"));
}
```

**Consequences:**
- Standard approach (portable)
- Testable with TestContainers
- Slightly more verbose than QueryDSL but clearer

---

## Decision 7: Record-Based Command/Query Objects

**Decision:**
Use Java records for Command and Query objects instead of traditional classes.

```java
public record CreateProductCommand(
    String code,
    String name,
    BigDecimal price,
    boolean isActive
) { ... }

public record GetProductQuery(Long id) { ... }
```

**Why?**
- Immutable (no accidental mutations)
- Automatic equals/hashCode/toString
- Less boilerplate (no getters)
- Clear intent: this is data, not logic

**Alternative (Traditional):**
```java
public class CreateProductCommand {
    private String code;
    private String name;
    private BigDecimal price;
    private boolean isActive;
    
    // 40 lines of getters/setters/constructors
}
```

**Consequences:**
- Less code to maintain
- Type-safe queries/commands
- Clear boundary objects

---

## Decision 8: Testing Pyramid (60-75 Tests Across 4 Layers)

**Decision:**
- 25-30 domain tests (pure Java)
- 10-15 application tests (mocked)
- 15-20 adapter tests (TestContainers)
- 5-10 controller tests (MockMvc)

**Why This Distribution?**
- Domain tests: Run fast, high confidence, no framework needed
- Application tests: Medium speed, test orchestration, mock external dependencies
- Adapter tests: Slower (real DB), high confidence in persistence
- Controller tests: Smoke tests for HTTP contract

**Why Not Just Integration Tests?**
```
300 integration tests (whole stack):
- Takes 10 minutes to run
- Hard to debug (unclear which layer failed)
- Redundant (if domain test passes, don't need to retest in integration)
```

**Why Pyramid Instead?**
```
60 tests (4 layers):
- Takes ~1 minute to run
- Clear failure point (which layer broke?)
- Redundancy minimized (tests each layer separately)
```

**Consequences:**
- Fast feedback loop (tests pass in ~1 minute)
- Clear root cause when tests fail
- Regression detection confident

```

---

### Day 14: Create Presentation Slides

**File to Create:** [PRESENTATION_SLIDES.md](PRESENTATION_SLIDES.md)

```markdown
# Presentation: Clean Architecture Refactoring of Product Domain
## Duration: 5-7 minutes

---

### SLIDE 1: Problem Statement

**Title:** Why We Needed Clean Architecture

**Bullet Points:**
- Old 3-layer architecture (Controller → Service → Repository) couples business logic to framework
- Business rules scattered across layers (validation in service, queries in repository)
- Hard to test domain logic independently (requires database)
- Difficult to change persistence technology (Domain logic depends on JPA)
- Strangler fig pattern: Gradually replace old code with new architecture

**Visuals:**
```
OLD ARCHITECTURE (Problem):
┌─────────────────┐
│   Controllers   │  Couples HTTP concerns
├─────────────────┤
│   Services      │  Couples business rules to persistence
├─────────────────┤
│  Repositories   │  Couples business rules to ORM
├─────────────────┤
│   Entities      │  Couples domain to framework
└─────────────────┘
```

---

### SLIDE 2: Solution - Clean Architecture

**Title:** Clean Architecture with Ports & Adapters

**Bullet Points:**
- Domain layer: Pure Java, no framework, business rules only
- Application layer: Use cases, queries, commands, orchestration
- Adapter layer: HTTP (inbound), Persistence (outbound), framework code isolated
- Infrastructure: Spring configuration, dependency injection
- Dependencies point inward (adapters depend on domain, not vice versa)

**Visuals:**
```
CLEAN ARCHITECTURE (Solution):
        ┌─────────────────┐
        │   Domain        │  Pure Java, independent
        │  Aggregates,    │  
        │  Value Objects, │
        │  Ports          │
        └─────────────────┘
              ▲       ▲
              │       │
        ┌─────────────────┐
        │  Application    │  Orchestrates business logic
        │   Use Cases,    │
        │   Commands      │
        └─────────────────┘
              ▲       ▲
         ┌────┴───────┴────┐
         │                 │
    ┌──────────┐     ┌──────────┐
    │ Inbound  │     │ Outbound │
    │(HTTP)    │     │(Database)│
    └──────────┘     └──────────┘
```

---

### SLIDE 3: Migration Strategy - Vertical Slices

**Title:** One Business Flow at a Time (Not One Layer at a Time)

**Bullet Points:**
- Slice 1: GET single product (complete end-to-end)
- Slice 2: LIST with pagination/filtering (complete end-to-end)
- Slices 3-5: CREATE/UPDATE/DELETE (follow same pattern)
- Old code coexists during migration (strangler pattern)
- Each slice independently testable and deployable

**Visuals:**
```
VERTICAL SLICE MIGRATION:

Slice 1 (GET):
Domain Layer     ✅ ProductAggregate, ProductCode, ProductName
   │
Application Layer ✅ GetProductUseCase, GetProductQuery
   │
HTTP Adapter     ✅ @GetMapping("/{id}")
   │
Persistence      ✅ findById()

Slice 2 (LIST):
Domain Layer     ✅ (same as Slice 1)
   │
Application Layer ✅ ListProductsUseCase with filtering/sorting
   │
HTTP Adapter     ✅ @GetMapping with query params
   │
Persistence      ✅ findAll(Specification, Pageable)
```

---

### SLIDE 4: Key Implementations - Code Walkthrough

**Title:** What We Built - Code Examples

**Code Example 1: Value Object (Domain - Pure Java)**
```java
public record ProductName(String value) {
    public ProductName {  // Compact constructor
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        this.value = value.trim();
    }
}
```

**Code Example 2: Use Case (Application Layer)**
```java
public ProductResult execute(GetProductQuery query) {
    ProductAggregate product = productRepository.findById(query.id())
        .orElseThrow(() -> new ProductNotFoundException(query.id()));
    return productApplicationMapper.toResult(product);
}
```

**Code Example 3: Controller (HTTP Adapter)**
```java
@GetMapping("/{id}")
public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
    ProductResult result = productApplicationService.execute(new GetProductQuery(id));
    return ResponseEntity.ok(productWebMapper.toResponse(result));
}
```

**Code Example 4: Dynamic Queries (Persistence Adapter)**
```java
Specification<ProductJpaEntity> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    if (params.containsKey("name")) {
        predicates.add(cb.like(root.get("name"), "%" + params.get("name") + "%"));
    }
    return cb.and(predicates.toArray(new Predicate[0]));
};
```

---

### SLIDE 5: Challenges Overcome

**Title:** Problems Faced & How We Solved Them

**Challenge 1: Framework Leakage**
- Problem: JPA @Entity annotations spreading to domain classes
- Solution: Separate domain aggregate from JPA entity, use mapper layer
- Result: Domain remains pure Java, fully testable without Spring

**Challenge 2: Dynamic Query Building**
- Problem: Filter parameters come from HTTP, need dynamic JPA queries
- Solution: JPA Specification pattern with CriteriaBuilder
- Result: Flexible queries without QueryDSL complexity

**Challenge 3: Coexistence of Old & New Code**
- Problem: Can't delete old code while building new code (nothing would work)
- Solution: Strangler pattern - new code works alongside old code
- Result: Can test new code, verify it works, then safely delete old code

**Challenge 4: Testing Multiple Layers**
- Problem: Domain needs pure Java tests, adapters need real DB, controller needs MockMvc
- Solution: 4-layer testing pyramid (different tools for each layer)
- Result: Fast feedback (25-30 domain tests in < 100ms), high confidence (60-75 total tests)

---

### SLIDE 6: Test Results & Validation

**Title:** Testing Strategy: Pyramid of Confidence

**Test Pyramid:**
```
                     👑
                Controller Tests
                 (MockMvc) 5-10
                     │
                Adapter Tests
             (TestContainers) 15-20
                  /        \
          Application    Database
              Tests       Tests
           (Mockito)    (Real DB)
            10-15        Spec. Tests
                 │
             Domain Tests
         (Pure Java) 25-30
         Value Objects & Aggregate
```

**Results:**
- ✅ 60-75 total tests passing
- ✅ All layers tested independently
- ✅ Code coverage: 70%+ for business logic
- ✅ Execution time: ~1 minute
- ✅ Domain tests: < 100ms (fastest)
- ✅ Adapter tests: 10-30s (real DB, slowest)

**What Gets Tested:**
1. Value objects validate fields
2. Aggregates mutate correctly
3. Use cases orchestrate correctly (with mocks)
4. Persistence layer persists and retrieves
5. HTTP endpoints return correct responses

---

### SLIDE 7: Results & Next Steps

**Title:** What We Achieved & What's Next

**Completed:**
- ✅ All 5 HTTP endpoints implemented (GET, LIST, CREATE, UPDATE, DELETE)
- ✅ Complete domain layer (pure Java, independent)
- ✅ Complete application layer (orchestration, use cases)
- ✅ Complete persistence adapter (filtering, sorting, pagination)
- ✅ 60-75 tests passing, high confidence

**Next Steps:**
1. Apply same pattern to other domains (Mailing, Payment, Security)
2. Delete old code for Product domain (tests prove replacement works)
3. Gradual migration of legacy code
4. Establish domain-driven design as standard

**Key Learnings:**
- Clean Architecture is achievable in existing monolith
- Vertical slices enable safe migration
- Testing pyramid provides fast feedback
- Value objects prevent invalid states
- Strangler pattern enables gradual replacement

```

---

### Day 15: Complete Report & Thesis Sections

**Files to Create:**
1. [FINAL_REPORT.md](FINAL_REPORT.md) - 5-8 pages
2. [THESIS_SECTIONS.md](THESIS_SECTIONS.md) - 10-15 pages

**FINAL_REPORT.md Structure:**

```markdown
# Final Report: Clean Architecture Refactoring of Product Domain

## Executive Summary

This report documents the successful refactoring of the Trébol backend's Product domain from a 3-layer architecture (Controller → Service → Repository) to a Clean Architecture with explicit domain, application, and adapter layers. The refactoring focuses on isolating business rules from framework and persistence concerns, enabling independent testing, flexible technology choices, and maintainable code.

**Key Results:**
- All 5 HTTP endpoints (GET, LIST, CREATE, UPDATE, DELETE) implemented
- 60-75 tests across 4 layers, all passing
- 70%+ code coverage for business logic
- Domain layer: pure Java, framework-independent
- Vertical slice migration enables safe, gradual code replacement

---

## 1. Introduction

### Problem Statement
The original Trébol backend monolith uses a 3-layer architecture that couples business logic to framework and persistence concerns. This creates several issues:

1. **Testing Difficulty:** Domain logic requires database/framework to test
2. **Technology Lock-in:** Business rules tightly coupled to JPA/Spring
3. **Scattered Rules:** Business logic lives in controller validation, service logic, and repository queries
4. **Difficult Refactoring:** Changing persistence technology requires rewriting business logic

### Project Objective
Refactor the Product domain using Clean/Hexagonal Architecture to:
- Isolate business rules in domain layer (pure Java)
- Separate application orchestration (use cases)
- Isolate framework code in adapters (HTTP, persistence)
- Enable independent testing at each layer
- Create template for refactoring other domains

### Scope
- **Domain:** Product (CRUD operations)
- **Operations:** GET, LIST (with pagination/filtering/sorting), CREATE, UPDATE, DELETE
- **Framework:** Spring Boot 3.2, Java 17, MariaDB, Hibernate/JPA
- **Timeline:** 3 weeks (1 week implementation, 1 week testing, 1 week documentation)

---

## 2. Architecture & Design

### Clean Architecture Layers

**Domain Layer (org.trebol.product.domain/)**
- Pure Java, no framework dependencies
- Contains: Aggregates, Value Objects, Ports, Exceptions
- Responsibilities: Business rules, validation, state management

**Application Layer (org.trebol.product.application/)**
- Spring @Service layer for orchestration
- Contains: Use Cases, Commands, Queries, Results
- Responsibilities: Implement use cases, coordinate domain and ports

**Adapter Layers**
- **Inbound (HTTP):** Controllers, Web Mappers, DTOs
- **Outbound (Persistence):** Repository Adapters, JPA Entities, Persistence Mappers

**Infrastructure (org.trebol.product.infrastructure/)**
- Spring @Configuration for dependency injection
- Wires domain ports to adapter implementations

### Dependency Graph

```
        ┌─────────────────┐
        │   Domain        │  ← Nobody depends on this!
        │  (Products)     │  ← Framework independent
        │  (Pure Java)    │
        └─────────────────┘
              ▲
              │ (depends on)
        ┌─────────────────┐
        │  Application    │
        │   (Products)    │
        └─────────────────┘
              ▲
              │
        ┌─────────────────┐
        │   Adapters      │
        │  (HTTP, DB)     │
        └─────────────────┘
```

All dependencies point inward. Adapters depend on domain, not vice versa.

---

## 3. Implementation Details

[Include code examples from IMPLEMENTATION_DETAILS.md section above]

### Value Objects
- ProductCode, ProductName, ProductPrice, ProductStatus, ProductId
- Immutable Java records with compact constructor validation
- Prevents invalid states at construction time

### Aggregates
- ProductAggregate: Root entity with mutable state (updateX methods)
- Value object composition (aggregates contain value objects)
- Business logic: status transitions, price updates, name validation

### Use Cases
1. **GetProductUseCase** - Query by ID, return ProductResult
2. **ListProductsUseCase** - Query with pagination/filtering/sorting
3. **CreateProductUseCase** - Create new product, check uniqueness
4. **UpdateProductUseCase** - Update product, handle not found
5. **DeleteProductUseCase** - Delete product

### Persistence Adapter
- Dynamic query building via JPA Specification pattern
- CriteriaBuilder for filter predicates
- Pagination support (PageRequest)
- Sorting support (Sort.by with direction)
- Entity mapping: ProductJpaEntity ↔ ProductAggregate

---

## 4. Challenges & Solutions

[Include content from CHALLENGES_AND_SOLUTIONS.md section above]

### Key Challenges

1. **Framework Leakage** → Separate domain from JPA entity
2. **Dynamic Queries** → JPA Specification pattern
3. **Aggregate Mapping** → Two-layer mapper (persistence + application)
4. **Old Code Coexistence** → Strangler pattern, keep old code during migration
5. **Multi-Layer Testing** → 4-layer pyramid with different test tools
6. **Exception Handling** → @ExceptionHandler to map domain exceptions to HTTP status
7. **Transaction Boundaries** → @Transactional on application service

---

## 5. Design Decisions

[Include content from DESIGN_DECISIONS.md section above]

### Key Decisions

1. **Vertical Slice Migration** over horizontal layer refactoring
   - Each business flow (slice) migrated end-to-end
   - One complete working endpoint per week

2. **Domain-Owned Repository Port**
   - Domain defines contract, adapter implements
   - Inversion of control pattern
   - Domain independent of persistence technology

3. **Value Objects with Compact Constructor Validation**
   - Impossible to create invalid state
   - Fail-fast validation at construction
   - Self-documenting business rules

4. **Record-Based Objects**
   - Commands, Queries, Results all use records
   - Immutability, minimal boilerplate
   - Clear boundary objects

5. **Separate Domain Aggregate from JPA Entity**
   - Domain remains pure Java
   - Mapping layer handles conversion
   - Framework code isolated in adapter

6. **JPA Specification Pattern for Dynamic Queries**
   - Standard Spring Data JPA approach
   - CriteriaBuilder for flexible predicates
   - Portable, no external dependencies

7. **Testing Pyramid**
   - 25-30 domain tests (pure Java)
   - 10-15 application tests (mocked)
   - 15-20 adapter tests (TestContainers)
   - 5-10 controller tests (MockMvc)
   - Total: 60-75 tests, ~1 minute execution

---

## 6. Testing Strategy

### Test Pyramid by Layer

| Layer | Test Type | Framework | Count | Execution Time |
|-------|-----------|-----------|-------|-----------------|
| Domain | JUnit5 | Pure Java | 25-30 | < 100ms |
| Application | JUnit5 + Mockito | Mocking | 10-15 | < 500ms |
| Adapter | JUnit5 + TestContainers | Real DB | 15-20 | 10-30s |
| Controller | JUnit5 + MockMvc | Spring MVC | 5-10 | < 5s |
| **Total** | | | **60-75** | **~1 min** |

### Sample Test Cases

**Domain Tests (ProductAggregateTest):**
- Aggregate creation with value objects
- updateName() changes name
- updatePrice() changes price
- Invalid value objects throw exceptions

**Application Tests (ProductApplicationServiceTest):**
- GetProductQuery returns correct ProductResult
- ListProductsQuery handles pagination
- CreateProductCommand checks code uniqueness
- UpdateProductCommand handles not found
- DeleteProductCommand throws ProductNotFoundException

**Adapter Tests (ProductRepositoryAdapterTest):**
- save() assigns ID from database
- findById() returns aggregate
- findByCode() finds by unique code
- findAll() with Specification filters correctly
- Pagination works: PageRequest with pageIndex and pageSize
- Sorting works: Sort.by field and direction

**Controller Tests (ProductControllerTest):**
- GET /{id} returns 200 with ProductResponse
- GET /{id} returns 404 when not found
- GET returns PagedProductsResponse with items and totalCount
- POST creates product, returns 201
- PUT updates product, returns 200
- DELETE returns 204 no content

### Coverage Results
- Overall code coverage: 70%+
- Domain layer coverage: 90%+ (focus area)
- Persistence adapter coverage: 85%+ (careful testing)
- HTTP controller coverage: 80%+

---

## 7. Results & Validation

### Implementation Complete
- ✅ All 5 HTTP endpoints implemented
- ✅ All use cases implemented (GET, LIST, CREATE, UPDATE, DELETE)
- ✅ All adapters implemented (HTTP, persistence)
- ✅ Infrastructure wiring complete
- ✅ Exception handling in place

### Testing Complete
- ✅ 60-75 tests across 4 layers
- ✅ All tests passing
- ✅ 70%+ code coverage
- ✅ ~1 minute total execution time
- ✅ Domain tests < 100ms (fast feedback)

### Validation Checklist
- ✅ Domain layer: pure Java, no Spring/JPA annotations
- ✅ Business rules: enforced at domain layer (value objects + aggregate)
- ✅ Dependency direction: adapters depend on domain, not vice versa
- ✅ Error handling: domain exceptions mapped to HTTP status codes
- ✅ Data flow: request → controller → use case → domain → persistence
- ✅ Response flow: persistence → domain → result → response DTO

### HTTP Endpoint Validation
```bash
# GET single product
GET /product-module/1
Response: 200 OK
{ id: 1, code: "LAP001", name: "Laptop", price: 999.99, status: "ACTIVE" }

# GET list with pagination
GET /product-module?pageIndex=0&pageSize=10&name=Laptop
Response: 200 OK
{ items: [...], totalCount: 5 }

# CREATE product
POST /product-module
{ code: "NEW001", name: "New Product", price: 499.99, isActive: true }
Response: 201 Created
{ id: 42, code: "NEW001", ... }

# UPDATE product
PUT /product-module/1
{ name: "Updated Name", price: 1299.99, isActive: true }
Response: 200 OK
{ id: 1, name: "Updated Name", ... }

# DELETE product
DELETE /product-module/1
Response: 204 No Content
```

---

## 8. Lessons Learned

### What Went Well
1. **Vertical Slices:** Easy to see progress, one working endpoint at a time
2. **Testing Pyramid:** Fast feedback from domain tests, high confidence from integration tests
3. **Records:** Clean, concise code for value objects and boundaries
4. **Strangler Pattern:** Old code coexisting made migration low-risk

### What Was Challenging
1. **Mapping Complexity:** Converting between domain aggregates and JPA entities required careful thought
2. **Dynamic Queries:** JPA Specification pattern has learning curve
3. **Test Infrastructure:** TestContainers adds ~20s to test execution but worth the confidence

### What Would Be Done Differently
1. **Earlier Integration:** Wire controllers earlier to catch mapping issues sooner
2. **Test-Driven Development:** Write tests before implementation to clarify contracts
3. **Documentation:** Document decisions earlier (easier to remember reasoning)

---

## 9. Future Work

### Immediate Next Steps
1. **Apply to Other Domains:** Payment, Mailing, Security domains follow same pattern
2. **Delete Old Code:** Once all tests pass, delete DataProductsController and related old code
3. **Establish Standard:** Make Clean Architecture the standard for new features

### Long-Term
1. **API Gateway:** Consider API Gateway for cross-domain concerns
2. **Event-Driven:** Add events for domain changes (ProductCreated, ProductUpdated)
3. **CQRS:** Separate read and write models if scalability needed
4. **Microservices:** Once domains are clean, can independently deploy as services

---

## 10. Conclusion

This refactoring successfully demonstrates that Clean Architecture can be applied to an existing Spring Boot monolith without complete rewrite. The vertical slice approach enabled safe, incremental migration. The testing pyramid provides confidence at each layer while maintaining fast feedback loops.

The Product domain now serves as a template for refactoring other domains in the Trébol backend. The investment in clean architecture and comprehensive testing will pay dividends in maintainability, flexibility, and developer velocity.

---

# Appendix: Supporting Artifacts

- IMPLEMENTATION_DETAILS.md: Code examples and design patterns
- CHALLENGES_AND_SOLUTIONS.md: In-depth explanations of challenges
- DESIGN_DECISIONS.md: Rationale for architectural choices
- PRESENTATION_SLIDES.md: Visual presentation (5-7 minutes)

```

**THESIS_SECTIONS.md:** (This would be 10-15 pages with detailed analysis - similar structure but more depth on technical explanations, research, and academic rigor)

---

## Week 3 Checkpoint (Days 11-15):
- ✅ IMPLEMENTATION_DETAILS.md: Code examples, design patterns, before/after comparisons
- ✅ CHALLENGES_AND_SOLUTIONS.md: 7 major challenges with technical solutions
- ✅ DESIGN_DECISIONS.md: 8 key decisions with rationale and consequences
- ✅ PRESENTATION_SLIDES.md: 7 slides, 5-7 minute presentation
- ✅ FINAL_REPORT.md: 5-8 page executive report
- ✅ THESIS_SECTIONS.md: 10-15 page detailed academic thesis

---

## FINAL CHECKLIST: 3-Week Project Completion

### Week 1: Implementation ✅
- [ ] Day 1: Wire GET single endpoint
- [ ] Day 2: Wire GET list + implement ProductWebMapper
- [ ] Day 3: Populate command objects + implement CreateProductUseCase
- [ ] Day 4: Implement UpdateProductUseCase + DeleteProductUseCase
- [ ] Day 5: Wire all 5 controller endpoints + manual testing

**Deliverable:** All 5 HTTP endpoints working via Thunder Client

### Week 2: Testing ✅
- [ ] Day 6: Domain layer tests (25-30 tests)
- [ ] Day 7: Application layer tests (10-15 tests)
- [ ] Day 8: Adapter layer tests with TestContainers (15-20 tests)
- [ ] Day 9: Controller layer tests with MockMvc (5-10 tests)
- [ ] Day 10: Run all tests, generate coverage report

**Deliverable:** 60-75 tests passing, 70%+ coverage, ~1 minute execution

### Week 3: Documentation ✅
- [ ] Day 11-12: Create IMPLEMENTATION_DETAILS.md, CHALLENGES_AND_SOLUTIONS.md
- [ ] Day 13: Create DESIGN_DECISIONS.md, PRESENTATION_SLIDES.md
- [ ] Day 14: Create FINAL_REPORT.md
- [ ] Day 15: Create THESIS_SECTIONS.md, Polish all documents

**Deliverable:** Presentation ready, Report complete, Thesis complete

---

## How to Present This to Professor

### Meeting Format (15-20 minutes)
1. **Opening (1 min):** "I've completed the Clean Architecture refactoring of the Product domain"
2. **Problem (2 min):** Show old architecture pain points, explain why migration needed
3. **Solution (3 min):** Show Clean Architecture diagram, explain layers, show vertical slice approach
4. **Implementation (5 min):** Live demo of GET/LIST/CREATE/UPDATE/DELETE endpoints, show code snippets
5. **Testing (3 min):** Show test pyramid, run tests, show coverage
6. **Challenges (3 min):** Explain 2-3 main challenges and solutions
7. **Results (1 min):** Summarize what was accomplished
8. **Questions (2 min):** Address professor questions

### What to Bring
- Computer for live demo
- Thunder Client requests to show endpoints
- Code editor with key files visible
- Test execution output (screenshot or terminal)
- Presentation slides (if using projector)

### Key Points to Emphasize
- "I kept old code intentionally (strangler pattern) - it's there if needed"
- "All business rules are in domain layer (value objects + aggregate) - testable without database"
- "60+ tests with fast feedback (1 minute) - catches regressions"
- "This pattern can be applied to other domains (Mailing, Payment, Security)"

---

This is your complete 3-week roadmap. Execute this plan day-by-day and you'll have a complete, well-tested, well-documented Clean Architecture refactoring ready for presentation, report, and thesis.

Good luck! 🚀
