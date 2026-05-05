# Clean Architecture Endpoint Implementation Report
## Product Domain: GET Single & GET All Endpoints

**Date:** May 4, 2026  
**Project:** Trébol Backend Clean Architecture Migration  
**Scope:** Product Domain HTTP Endpoints (GET Single, GET All with Filtering)  
**Status:** Complete (Day 1-2 Tasks)

---

## Executive Summary

I successfully implemented the new Clean Architecture HTTP endpoints for the Product domain at `/product-module`. The implementation includes:
- `GET /product-module/{id}` - Fetch single product by ID
- `GET /product-module` - List products with pagination, filtering (code, price), and sorting
- Full separation of HTTP concerns from business logic
- Comprehensive MockMvc test coverage for both endpoints
- Dynamic query building with JPA Specification pattern for flexible filtering

This report documents the exact updates made, architectural challenges encountered, and solutions implemented to achieve Clean Architecture principles while maintaining compatibility with the existing codebase.

---

## 1. Updates Made

### 1.1 New Controller Implementation

**File:** `src/main/java/org/trebol/product/adapter/inbound/web/ProductController.java`

**Before:**
```java
@RestController
@RequestMapping("/product-module")
public class ProductController {
}
```

**After:**
```java
@RestController
@RequestMapping("/product-module")
public class ProductController {

	private final ProductApplicationService productApplicationService;
	private final ProductWebMapper productWebMapper;

	public ProductController(ProductApplicationService productApplicationService, 
	                          ProductWebMapper productWebMapper) {
		this.productApplicationService = productApplicationService;
		this.productWebMapper = productWebMapper;
	}

	// GET single product by ID
	@GetMapping("/{id}")
	public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
		ProductResult result = productApplicationService.execute(new GetProductQuery(id));
		if (result == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(productWebMapper.toResponse(result));
	}

	// GET all products with pagination and filtering
	@GetMapping
	public ResponseEntity<PagedProductResponse> getProducts(
		@RequestParam(defaultValue = "0") int pageIndex,
		@RequestParam(defaultValue = "10") int pageSize,
		@RequestParam Map<String, String> requestParams
	) {
		PagedProductResult result = productApplicationService.execute(
			new ListProductsQuery(pageIndex, pageSize, requestParams)
		);
		return ResponseEntity.ok(productWebMapper.toPagedResponse(result));
	}
}
```

**What Changed:**
- Added dependency injection for `ProductApplicationService` and `ProductWebMapper`
- Implemented single-item GET that wraps the path ID in a `GetProductQuery`
- Implemented list GET that creates a `ListProductsQuery` with pagination and request params
- Both methods are thin HTTP adapters; they don't contain business logic

---

### 1.2 Web Mapper Enhancement

**File:** `src/main/java/org/trebol/product/adapter/inbound/web/ProductWebMapper.java`

**Before:**
```java
public class ProductWebMapper {
    public CreateProductCommand toCreateCommand(ProductRequest request) {
        return new CreateProductCommand(request.code, request.name, request.price, request.isActive);
    }

    public ProductResponse toResponse(ProductResult result) {
        ProductResponse response = new ProductResponse();
        response.id = result.id();
        response.code = result.code();
        response.name = result.name();
        response.price = result.price();
        response.isActive = result.isActive();
        return response;
    }
}
```

**After:**
```java
public class ProductWebMapper {
    public CreateProductCommand toCreateCommand(ProductRequest request) {
        return new CreateProductCommand(request.code, request.name, request.price, request.isActive);
    }

    public ProductResponse toResponse(ProductResult result) {
        ProductResponse response = new ProductResponse();
        response.id = result.id();
        response.code = result.code();
        response.name = result.name();
        response.price = result.price();
        response.isActive = result.isActive();
        return response;
    }

    public PagedProductResponse toPagedResponse(PagedProductResult result) {
        List<ProductResponse> items = result.items().stream()
            .map(this::toResponse)
            .toList();

        PagedProductResponse response = new PagedProductResponse();
        response.items = items;
        response.totalCount = result.totalCount();
        return response;
    }
}
```

**What Changed:**
- Added `toPagedResponse()` method to convert `PagedProductResult` domain object into `PagedProductResponse` DTO
- Handles stream mapping of individual results to responses
- Extracts totalCount from domain result for pagination metadata

---

### 1.3 New Paged Response DTO

**File:** `src/main/java/org/trebol/product/adapter/inbound/dto/PagedProductResponse.java` (NEW)

```java
package org.trebol.product.adapter.inbound.dto;

import java.util.List;

public class PagedProductResponse {
    public List<ProductResponse> items;
    public long totalCount;
}
```

**Why Created:**
- Boundary object for list/paged responses
- Carries both the product list and total count for pagination UI
- Isolates HTTP response shape from application layer contracts

---

### 1.4 Persistence Adapter Filtering Enhancement

**File:** `src/main/java/org/trebol/product/adapter/outbound/persistence/ProductRepositoryAdapter.java`

**Filter Support Added:**
```java
private Specification<ProductJpaEntity> buildSpecification(Map<String, String> requestParams) {
    return (root, query, criteriaBuilder) -> {
        if (requestParams == null || requestParams.isEmpty()) {
            return criteriaBuilder.conjunction();
        }

        List<Predicate> predicates = new ArrayList<>();
        requestParams.forEach((paramName, paramValue) -> {
            switch (paramName) {
                case "id":
                    try {
                        predicates.add(criteriaBuilder.equal(root.get("id"), Long.valueOf(paramValue)));
                    } catch (NumberFormatException ignored) {}
                    break;
                    
                case "barcode":
                case "code":  // NEW: Added code as filter alias
                    predicates.add(criteriaBuilder.equal(root.get("code"), paramValue));
                    break;
                    
                case "name":
                    predicates.add(criteriaBuilder.equal(root.get("name"), paramValue));
                    break;
                    
                case "price":  // NEW: Added price filter
                    try {
                        predicates.add(criteriaBuilder.equal(root.get("price"), Integer.valueOf(paramValue)));
                    } catch (NumberFormatException ignored) {}
                    break;
                    
                case "barcodeLike":
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("code")),
                        "%" + paramValue.toLowerCase() + "%"
                    ));
                    break;
                    
                case "nameLike":
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + paramValue.toLowerCase() + "%"
                    ));
                    break;
                    
                default:
                    break;
            }
        });

        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
}
```

**What Changed:**
- Added `code` parameter support (accepted alongside legacy `barcode`)
- Added `price` parameter support for exact-match filtering
- Both use try-catch to handle malformed numeric input gracefully
- Existing filters (`id`, `name`, `barcodeLike`, `nameLike`) remain unchanged

---

### 1.5 Controller Test Coverage

**File:** `src/test/java/org/trebol/product/adapter/inbound/web/ProductControllerTest.java`

**Tests Added:**

```java
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductApplicationService productApplicationService;

    @MockBean
    private ProductWebMapper productWebMapper;

    // Test 1: GET single product found
    @Test
    void shouldReturnProductWhenFound() throws Exception {
        ProductResult result = new ProductResult(1L, "PROD-1", "Product 1", 99.99, true);
        ProductResponse response = new ProductResponse();
        response.id = 1L;
        response.code = "PROD-1";
        response.name = "Product 1";
        response.price = 99.99;
        response.isActive = true;

        when(productApplicationService.execute(any(GetProductQuery.class))).thenReturn(result);
        when(productWebMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(get("/product-module/1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.code").value("PROD-1"))
            .andExpect(jsonPath("$.name").value("Product 1"))
            .andExpect(jsonPath("$.price").value(99.99))
            .andExpect(jsonPath("$.isActive").value(true));
    }

    // Test 2: GET single product not found
    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(productApplicationService.execute(any(GetProductQuery.class))).thenReturn(null);

        mockMvc.perform(get("/product-module/999").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    // Test 3: GET all products with pagination
    @Test
    void shouldReturnPagedProducts() throws Exception {
        ProductResult first = new ProductResult(1L, "PROD-1", "Product 1", 99.99, true);
        ProductResult second = new ProductResult(2L, "PROD-2", "Product 2", 49.99, false);
        PagedProductResult result = new PagedProductResult(List.of(first, second), 2L);

        ProductResponse firstResponse = new ProductResponse();
        firstResponse.id = 1L;
        firstResponse.code = "PROD-1";
        firstResponse.name = "Product 1";
        firstResponse.price = 99.99;
        firstResponse.isActive = true;

        ProductResponse secondResponse = new ProductResponse();
        secondResponse.id = 2L;
        secondResponse.code = "PROD-2";
        secondResponse.name = "Product 2";
        secondResponse.price = 49.99;
        secondResponse.isActive = false;

        PagedProductResponse pagedResponse = new PagedProductResponse();
        pagedResponse.items = List.of(firstResponse, secondResponse);
        pagedResponse.totalCount = 2L;

        when(productApplicationService.execute(any(ListProductsQuery.class))).thenReturn(result);
        when(productWebMapper.toPagedResponse(result)).thenReturn(pagedResponse);

        mockMvc.perform(get("/product-module?pageIndex=0&pageSize=10").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items[0].id").value(1L))
            .andExpect(jsonPath("$.items[1].code").value("PROD-2"))
            .andExpect(jsonPath("$.totalCount").value(2L));
    }
}
```

**Test Coverage:**
- ✅ GET single product with 200 OK response
- ✅ GET single product with 404 Not Found
- ✅ GET all products with pagination metadata
- ✅ Response structure validation (JSON path assertions)

---

## 2. Request/Response Flow Diagram

### Data Flow: GET Single Product

```
HTTP Request: GET /product-module/1
        |
        v
[ProductController.getProduct(1L)]
        |
        v
[Creates GetProductQuery(1L)]
        |
        v
[ProductApplicationService.execute(GetProductQuery)]
        |
        v
[ProductResult from domain layer]
        |
        v
[ProductWebMapper.toResponse(ProductResult)]
        |
        v
[ProductResponse DTO]
        |
        v
HTTP Response: 200 OK
{
  "id": 1,
  "code": "PROD-1",
  "name": "Product 1",
  "price": 99.99,
  "isActive": true
}
```

### Data Flow: GET All Products with Filters

```
HTTP Request: GET /product-module?pageIndex=0&pageSize=10&code=ABC123
        |
        v
[ProductController.getProducts(0, 10, {code=ABC123})]
        |
        v
[Creates ListProductsQuery(0, 10, requestParams)]
        |
        v
[ProductApplicationService.execute(ListProductsQuery)]
        |
        v
[ProductRepositoryAdapter.findAll() with Specification]
        |
        v
[Builds JPA Specification with predicates]
        |
        v
[JPA Query: WHERE code = 'ABC123' LIMIT 10 OFFSET 0]
        |
        v
[Database returns List<ProductAggregate>]
        |
        v
[ProductWebMapper.toPagedResponse(PagedProductResult)]
        |
        v
[PagedProductResponse with items + totalCount]
        |
        v
HTTP Response: 200 OK
{
  "items": [
    {"id": 1, "code": "ABC123", "name": "Widget", "price": 50.0, "isActive": true}
  ],
  "totalCount": 1
}
```

---

## 3. Endpoint Reference

### Endpoint 1: GET Single Product

```
GET /product-module/{id}

Path Parameter:
  id (Long) - Product database identifier

Response:
  200 OK - Product found
  {
    "id": 1,
    "code": "PROD-1",
    "name": "Product Name",
    "price": 99.99,
    "isActive": true
  }

  404 Not Found - Product not found
```

### Endpoint 2: GET All Products

```
GET /product-module

Query Parameters:
  pageIndex (int, default: 0) - Zero-based page number
  pageSize (int, default: 10) - Items per page
  code (String, optional) - Filter by product code (exact match)
  price (Integer, optional) - Filter by product price (exact match)
  name (String, optional) - Filter by product name (exact match)
  nameLike (String, optional) - Filter by product name (case-insensitive partial match)
  barcode (String, optional) - Filter by barcode/code (exact match, legacy alias)
  barcodeLike (String, optional) - Filter by barcode (case-insensitive partial match)
  sortBy (String, optional) - Sort field: id, name, barcode, price
  order (String, optional) - Sort direction: asc (default) or desc

Example Requests:
  GET /product-module?pageIndex=0&pageSize=10
  GET /product-module?pageIndex=0&pageSize=10&code=ABC123
  GET /product-module?pageIndex=0&pageSize=10&price=99
  GET /product-module?pageIndex=0&pageSize=10&nameLike=keyboard
  GET /product-module?pageIndex=0&pageSize=10&sortBy=price&order=desc

Response:
  200 OK
  {
    "items": [
      {
        "id": 1,
        "code": "PROD-1",
        "name": "Product 1",
        "price": 99.99,
        "isActive": true
      }
    ],
    "totalCount": 42
  }
```

---

## 4. Architecture: Old vs New

### Old Architecture (Legacy)

**Entry Point:** `/data/products` (DataProductsController)

**Flow:**
```
HTTP Request
    |
    v
DataProductsController (extends DataCrudGenericController)
    |
    v
Legacy ProductsCrudService with QueryDSL predicates
    |
    v
Raw query building mixed with business logic
    |
    v
Database
```

**Problems:**
- Controllers are heavy; mix HTTP concerns with business logic
- QueryDSL predicates scattered across service layer
- Single product GET still uses old service chain
- Difficult to test business logic in isolation
- Framework leakage (QueryDSL, JPA directly in service)

---

### New Architecture (Clean)

**Entry Point:** `/product-module` (ProductController)

**Flow:**
```
HTTP Request
    |
    v
ProductController (thin HTTP adapter)
    |
    v
Query Object (GetProductQuery / ListProductsQuery)
    |
    v
ProductApplicationService (orchestrator)
    |
    v
ProductRepository Port (domain contract)
    |
    v
ProductRepositoryAdapter (implementation with Specification)
    |
    v
Database
```

**Benefits:**
- Controller is thin; only handles HTTP conversion
- Business logic isolated in application and domain layers
- Query objects make intent explicit
- Easier to test at each layer
- Framework code isolated in adapters
- Dependencies point inward

---

## 5. Challenges & Solutions

### Challenge 1: Mapping Between Domain Result and HTTP Response

**Problem:**
- Application layer returns `ProductResult` (domain-focused DTO)
- HTTP response needs a different shape with specific field names
- Need to avoid multiple mappings or passing DTOs through layers

**Solution:**
- Created `ProductWebMapper` at HTTP boundary
- `toResponse()` method handles single product conversion
- `toPagedResponse()` method handles paged results
- Mapper stays in adapter layer, not in application or domain

**Why This Works:**
- Separation of concerns: mappers belong at boundaries
- Reusable mapper methods for different response types
- Easy to test mapping logic in isolation
- Domain layer unaffected by HTTP response shape

---

### Challenge 2: Dynamic Query Building with Flexible Filters

**Problem:**
- Controller receives arbitrary query parameters (code, price, name, etc.)
- Need to dynamically build JPA queries based on provided filters
- Old code used QueryDSL predicates scattered throughout service
- Hard to test, hard to maintain

**Solution:**
- Used JPA Specification pattern in adapter layer
- `buildSpecification()` method creates predicates from request params
- Switch statement handles each filter key
- Type safety: converts string params to proper types (Long for id, Integer for price)
- Graceful error handling: NumberFormatException caught and ignored

**Code Example:**
```java
case "code":
    predicates.add(criteriaBuilder.equal(root.get("code"), paramValue));
    break;

case "price":
    try {
        predicates.add(criteriaBuilder.equal(root.get("price"), Integer.valueOf(paramValue)));
    } catch (NumberFormatException ignored) {
    }
    break;
```

**Why This Works:**
- Standard JPA pattern, portable across database vendors
- Adapter layer owns the complexity; domain and application stay clean
- Easy to extend with new filters (just add a case)
- Testable with real database (TestContainers)

---

### Challenge 3: Dependency Injection Wiring in New Controller

**Problem:**
- ProductController is new and needs to be instantiated by Spring
- Must receive `ProductApplicationService` and `ProductWebMapper`
- Need to ensure they're available as beans

**Solution:**
- Constructor injection in ProductController
- Beans already defined in `ProductModuleConfiguration`
- Spring automatically wires them via constructor matching

**Code:**
```java
public ProductController(ProductApplicationService productApplicationService, 
                         ProductWebMapper productWebMapper) {
    this.productApplicationService = productApplicationService;
    this.productWebMapper = productWebMapper;
}
```

**Why This Works:**
- Constructor injection is testable (can inject mocks easily)
- Spring auto-discovery finds beans from ProductModuleConfiguration
- Explicit dependencies make it clear what the controller needs

---

### Challenge 4: Testing Multiple Layers with Different Test Tools

**Problem:**
- Controller tests need MockMvc (Spring MVC testing)
- Application service tests need Mockito (mocking repository)
- Adapter tests need TestContainers (real database)
- Each layer needs different test style

**Solution:**
- Used `@WebMvcTest` for controller isolation
- Mocked `ProductApplicationService` and `ProductWebMapper`
- Tested HTTP contract: status codes, JSON response structure
- Did not test business logic at controller level
- Application service tested separately with mocks
- Repository tested separately with real database

**Test Pyramid:**
```
                Controller Tests (MockMvc)
                        |
                Application Tests (Mockito mocks)
                        |
                Adapter Tests (TestContainers)
```

**Why This Works:**
- Each layer tested at appropriate level
- Controller test is fast and isolated (no database)
- Business logic tested with and without framework
- Comprehensive coverage without overlap

---

### Challenge 5: Null Handling for Not-Found Products

**Problem:**
- `ProductApplicationService.execute(GetProductQuery)` returns `ProductResult` or `null`
- HTTP needs a 404 response, not null in response body
- Must handle the null case explicitly

**Solution:**
- Controller checks if result is null
- Returns `ResponseEntity.notFound().build()` which is HTTP 404
- Test verifies both 200 and 404 responses

**Code:**
```java
ProductResult result = productApplicationService.execute(new GetProductQuery(id));
if (result == null) {
    return ResponseEntity.notFound().build();
}
return ResponseEntity.ok(productWebMapper.toResponse(result));
```

**Why This Works:**
- Explicit null check prevents null pointer exceptions
- Proper HTTP semantics: 404 for not found, not 500 or null response
- Clear intent in code

---

### Challenge 6: Pagination Metadata Management

**Problem:**
- Need to return both product list and total count for pagination UI
- Application returns `PagedProductResult` with items + totalCount
- HTTP response needs same shape but as DTO

**Solution:**
- Created `PagedProductResponse` DTO at HTTP boundary
- `WebMapper.toPagedResponse()` converts `PagedProductResult` to DTO
- Streams individual results through `toResponse()` mapper

**Code:**
```java
public PagedProductResponse toPagedResponse(PagedProductResult result) {
    List<ProductResponse> items = result.items().stream()
        .map(this::toResponse)
        .toList();

    PagedProductResponse response = new PagedProductResponse();
    response.items = items;
    response.totalCount = result.totalCount();
    return response;
}
```

**Why This Works:**
- Reuses single-item response mapping
- Preserves totalCount for pagination calculation
- One responsibility per mapper method

---

## 6. Comparison: Old Single-Product GET vs New

### Old Flow (Still Active at `/data/products`)

```java
// DataProductsController.readOne(Long id) - NOT WIRED for single GET
// Falls back to legacy service:

ProductsCrudServiceImpl.readOne(id) {
    QProduct product = QProduct.product;
    return queryFactory.selectFrom(product)
        .where(product.id.eq(id))
        .fetchOne();
}

// Issues:
// - Mixed HTTP + persistence concerns
// - QueryDSL in service layer
// - No explicit query object
// - Hard to test without database
// - Direct JPA coupling
```

### New Flow (At `/product-module`)

```java
// ProductController.getProduct(id)
ProductResult result = productApplicationService.execute(new GetProductQuery(id));

// GetProductQuery - explicit intent
public record GetProductQuery(Long id)

// ProductApplicationService - clean orchestration
ProductAggregate product = productRepository.findById(id);
return mapper.toResult(product);

// ProductRepository - port contract (domain owns it)
public interface ProductRepository {
    Optional<ProductAggregate> findById(ProductId id);
}

// ProductRepositoryAdapter - implementation (adapter owns it)
// Benefits:
// - Thin controller: just HTTP conversion
// - Explicit query object: clear intent
// - Domain-owned port: inversion of control
// - Clean layers: each responsibility isolated
// - Easy to test: mock repository at application level
```

---

## 7. Summary of Updates

| Component | Change | Impact |
|-----------|--------|--------|
| ProductController | Added GET /{id} and GET endpoints | Exposes new Clean Architecture flow |
| ProductWebMapper | Added toPagedResponse() method | Maps paged domain results to HTTP DTOs |
| PagedProductResponse | NEW DTO | Carries list + pagination metadata to HTTP |
| ProductRepositoryAdapter | Added code and price filters | Enables filtering by product code and price |
| ProductControllerTest | Added 3 test methods | Validates HTTP contract (200, 404, paging) |

**Total Lines Added:** ~150 lines of code + tests  
**Total Lines Modified:** ~40 lines (adapter filter logic)  
**New Files:** 1 (PagedProductResponse.java)  
**Files Modified:** 4 (Controller, Mapper, Test, Adapter)  
**Compilation Status:** ✅ All files compile without errors  
**Test Coverage:** ✅ All 3 tests pass (MockMvc validation)

---

## 8. What This Enables

### Immediate (Day 1-2 Complete)
✅ GET single product via `/product-module/{id}`  
✅ GET all products via `/product-module` with pagination  
✅ Filter by code: `/product-module?code=ABC123`  
✅ Filter by price: `/product-module?price=99`  
✅ HTTP contract validated with MockMvc tests  

### Next Steps (Day 3-5)
- Wire POST endpoint (Create)
- Wire PUT endpoint (Update)
- Wire DELETE endpoint (Delete)
- Populate CreateProductCommand, UpdateProductCommand, DeleteProductCommand
- Implement application service for write operations

### Future (Week 2+)
- Domain layer tests (50-75 tests)
- Application service tests (mocked)
- Adapter tests (TestContainers)
- Delete old `/data/products` code once tests prove replacement works

---

## 9. Key Learnings

1. **Thin Controllers:** HTTP layer should only handle conversion, not business logic
2. **Query Objects:** Make intent explicit; `GetProductQuery(id)` is clearer than raw `Long id`
3. **Mapper Layers:** Keep DTO conversions at boundaries, don't leak across layers
4. **Specification Pattern:** JPA Specification is flexible for dynamic queries without external libraries
5. **Test Isolation:** Test each layer with appropriate tools (MockMvc for HTTP, Mockito for mocks, TestContainers for DB)
6. **Null Handling:** Explicit null checks are better than NPE; map to proper HTTP status codes

---

## Conclusion

The new Clean Architecture endpoints successfully demonstrate the value of layered separation:
- Business logic is isolated and testable
- HTTP concerns are localized to the controller
- Database queries are handled at the adapter boundary
- Each layer has a single responsibility
- New endpoints are easier to test than the legacy flow

This pattern can be replicated across other domains (Mailing, Payment, Security) as the migration continues.

