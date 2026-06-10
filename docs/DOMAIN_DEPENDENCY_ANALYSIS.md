# Domain Dependency Analysis
## Impact of Product Domain Migration to Clean Architecture

**Date:** May 4, 2026  
**Scope:** How `/product-module` migration affects Payment, Mailing, and other domains  
**Current Status:** Both `/data/products` (legacy) and `/product-module` (new) coexist

---

## 1. Current Dependency Structure

### Architecture Overview
```
┌─────────────────────────────────────┐
│       Payment Domain                │
│  (PaymentService Interface)         │
│  - requestNewPaymentPageDetails()   │
│  - requestPaymentResult()           │
└──────────┬──────────────────────────┘
           │
           │ Works with OrderPojo
           ↓
┌─────────────────────────────────────┐
│       Order Domain (Implicit)       │
│  OrderPojo contains:                │
│  - List<OrderDetailPojo>            │
│  - OrderDetailPojo contains:        │
│      - ProductPojo                  │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│       Product Domain (Legacy)       │
│  - /data/products endpoint          │
│  - DataProductsController           │
│  - ProductService (old)             │
└─────────────────────────────────────┘


┌─────────────────────────────────────┐
│       Mailing Domain                │
│  (MailingService Interface)         │
│  - notifyOrderStatusToClient()      │
│  - notifyOrderStatusToOwners()      │
└──────────┬──────────────────────────┘
           │
           │ Works with OrderPojo
           ↓
        (Same as above)
```

### Key Finding: **Indirect Dependency**

Payment and Mailing domains **DO NOT directly import or depend on ProductService/ProductRepository**. Instead:

1. **Payment** receives `OrderPojo` which contains `List<OrderDetailPojo>`
2. Each `OrderDetailPojo` contains a `ProductPojo` (lightweight DTO with: `id`, `code`, `name`, `price`)
3. **Payment** extracts product data from `OrderPojo.details[].product.price` to calculate totals
4. **Mailing** receives the same `OrderPojo` and extracts `product.name` and `product.code` for email templates

---

## 2. Impact Analysis by Domain

### Domain 1: Payment Domain ⚠️ **MINIMAL IMPACT**

**Current State:**
- Payment service receives fully-populated `OrderPojo`
- Accesses product data through `OrderDetailPojo.product.price`
- No direct ProductService calls

**Impact of Migration:**
- ✅ **No code changes required** in Payment implementation
- ✅ Product endpoint switch is transparent (data comes pre-embedded in OrderPojo)
- ✅ Can continue operating independently

**Example Current Flow:**
```java
public PaymentRedirectionDetailsPojo requestNewPaymentPageDetails(OrderPojo order) {
    // Payment extracts totals from order.netValue, order.taxValue, order.totalValue
    // Payment DOES NOT call product service
    // Product data already embedded in order.details[].product
    
    int calculatedTotal = order.getDetails().stream()
        .mapToInt(detail -> detail.getUnitValue() * detail.getUnits())
        .sum();
    
    // Works regardless of whether products come from /data/products or /product-module
}
```

**Migration Readiness:** ✅ **READY - No changes needed**

---

### Domain 2: Mailing Domain ⚠️ **MINIMAL IMPACT**

**Current State:**
- Mailing service receives `OrderPojo` with embedded products
- Uses `product.name`, `product.code` in email templates
- No direct ProductService calls

**Impact of Migration:**
- ✅ **No code changes required** in Mailing implementation
- ✅ Email templates continue to work
- ✅ Transparent to product endpoint changes

**Example Current Flow:**
```java
public void notifyOrderStatusToClient(OrderPojo order) {
    String emailBody = String.format("""
        Order #%d contains:
        %s
        """,
        order.getBuyOrder(),
        order.getDetails().stream()
            .map(d -> d.getProduct().getName() + " x" + d.getUnits())
            .collect(Collectors.joining(", "))
    );
    
    // Product data already in OrderPojo, regardless of which endpoint provided it
}
```

**Migration Readiness:** ✅ **READY - No changes needed**

---

### Domain 3: Security Domain 🟡 **POTENTIAL IMPACT**

**Current State:**
- Unknown if Security domain accesses product data
- Likely handles product-level access control rules

**Potential Scenarios:**
1. **If Security only validates user permissions:** ✅ No impact
2. **If Security checks product access rules:** 🟡 Might need adaptation if permission checks depend on ProductService

**Recommendation:** Review Security module implementation

---

### Domain 4: Order Domain (Implied but Not Explicit) 🔴 **MEDIUM IMPACT**

**Current State:**
- Orders are created by mixing product data with customer data
- Likely uses both `/data/products` for product lookup AND order creation logic

**Potential Impact:**
- When orders are created, if the code calls `/data/products` HTTP endpoint, needs migration
- If orders call ProductService directly, needs to use new Clean Architecture service

**Example Concern:**
```java
// CURRENT (might be doing this):
OrderDetail createOrderDetail(Long productId, int quantity) {
    Product product = productService.getById(productId);  // OLD service
    return new OrderDetail(product, quantity);
}

// AFTER MIGRATION, should be:
OrderDetail createOrderDetail(Long productId, int quantity) {
    ProductResult product = productApplicationService.execute(
        new GetProductQuery(productId)
    );
    return new OrderDetail(product, quantity);
}
```

---

## 3. Migration Strategy by Phase

### Phase 1: Current State (Coexistence) ✅

**What's happening now:**
```
HTTP Client
    |
    ├─→ /data/products (legacy, DataProductsController)
    |       └─→ Old QueryDSL service
    |
    └─→ /product-module (new, ProductController)
            └─→ Clean Architecture service
```

**Both endpoints active:**
- `/data/products` still serves old code
- `/product-module` serves new Clean Architecture code
- Payment and Mailing don't care which is used
- Order creation can use either endpoint (but should migrate)

### Phase 2: Migration (Weeks 2-3) 🟡

**Key Decision Points:**

1. **For Payment Domain:**
   - ✅ No action required (already receives OrderPojo with embedded products)
   - When orders are created, they should use new `/product-module` endpoint

2. **For Mailing Domain:**
   - ✅ No action required (already receives OrderPojo with embedded products)
   - No code changes needed

3. **For Order Creation Logic:**
   - 🟡 Needs to switch from calling `/data/products` to `/product-module`
   - Or switch from old ProductService to new ProductApplicationService

4. **For Security Domain:**
   - 🟡 Review if any product-level access control depends on ProductService
   - Adapt if needed

### Phase 3: Legacy Cleanup (Week 4+) 🔴

**After confirming all domains work with new endpoint:**
- Delete `/data/products` endpoint (DataProductsController)
- Delete old ProductService/ProductRepository implementations
- Archive legacy code

---

## 4. Detailed Impact Matrix

| Domain | Current Dependency | Dependency Type | Migration Impact | Action Required | Timeline |
|--------|-------------------|-----------------|-----------------|-----------------|----------|
| **Payment** | OrderPojo.details[].product | Embedded DTO | None | ✅ None | Phase 1 |
| **Mailing** | OrderPojo.details[].product | Embedded DTO | None | ✅ None | Phase 1 |
| **Security** | Possibly ProductService | Direct (?) | Unknown | 🟡 Review | Phase 1 |
| **Order Creation** | ProductService or /data/products | Direct API/Service | Code changes | 🟡 Update imports/endpoints | Phase 2 |
| **Caching Layer** | Likely depends on products | Service calls | May need invalidation rules | 🟡 Review cache keys | Phase 2 |
| **Search/Indexing** | Possibly ProductService | Service calls | Index rebuild | 🟡 Review if exists | Phase 2 |

---

## 5. What This Migration Enables

### Immediate Benefits (Now)
✅ Product GET endpoints use Clean Architecture  
✅ Payment and Mailing unaffected (use embedded product data in OrderPojo)  
✅ New endpoints testable independent of old code  

### Short-term Benefits (Phase 2-3)
✅ Order creation logic modernized  
✅ All CRUD operations (GET, POST, PUT, DELETE) using Clean Architecture  
✅ Single source of truth for product data  
✅ Easier to add new business logic (e.g., inventory, recommendations)  

### Long-term Benefits (Phase 4+)
✅ Easy to extract Product domain into microservice (bounded context clear)  
✅ Payment/Mailing can run independently of Product (already loosely coupled)  
✅ Event-driven architecture possible (product updated → publish event → payment/mailing subscribe)  
✅ Can replace implementations without affecting dependents  

---

## 6. Risk Assessment

### Low Risk Changes ✅
- Payment domain continues unchanged
- Mailing domain continues unchanged
- Both read-only from product data (via OrderPojo)

### Medium Risk Changes 🟡
- Order creation logic migrates to new ProductApplicationService
- Security domain reviews product access rules
- Caching layer adapts to new service structure

### High Risk Changes 🔴
- Deleting `/data/products` endpoint before all domains migrated
- Removing old ProductService before new one validated
- Assuming Payment/Mailing work without testing

---

## 7. Recommended Action Plan

### This Week (Day 3-5)
1. ✅ Complete GET endpoints (DONE)
2. 🟡 Search codebase for all calls to ProductService
3. 🟡 List all places that call `/data/products` endpoint
4. 🟡 Document which other domains import Product-related classes

### Next Week (Phase 2)
1. Implement POST/PUT/DELETE endpoints for Product
2. Migrate Order creation logic to use new ProductApplicationService
3. Run integration tests between Payment/Mailing and new Product endpoints
4. Update any caching/indexing layers

### Final Week (Phase 3)
1. Delete old ProductService implementations
2. Delete `/data/products` endpoint (DataProductsController)
3. Clean up legacy code
4. Final full integration testing

---

## 8. Key Architectural Insight

The beauty of the current monolith structure is that **Payment and Mailing are already decoupled from Product** because they receive **fully populated OrderPojo objects** with embedded product data. 

This means:

1. **Product migrations don't break Payment/Mailing** (already tested)
2. **Future extraction to microservices is easier** (Payment could call Product API over HTTP)
3. **Event-driven architecture becomes possible** (Product emits events, Payment/Mailing subscribe)

The migration is **low-risk** for dependent domains because the dependency is **indirect and read-only** through data transfer objects, not direct service coupling.

---

## 9. Code Locations to Monitor

**Review These Files for Product Dependencies:**

```
src/main/java/org/trebol/payment/
  ├── PaymentService.java (interface - SAFE, uses OrderPojo)
  └── impl/webpayplus/
      └── WebpayplusPaymentServiceImpl.java (check imports)

src/main/java/org/trebol/mailing/
  ├── MailingService.java (interface - SAFE, uses OrderPojo)
  └── impl/mailgun/
      └── MailgunMailingServiceImpl.java (check imports)

src/main/java/org/trebol/security/
  └── *.java (check for ProductService imports)

src/main/java/org/trebol/api/
  └── DataController.java (might use ProductService)

src/main/java/org/trebol/config/
  └── *.java (check bean configuration)
```

---

## Summary

**The Good News:** Payment and Mailing domains are **already loosely coupled** from Product through OrderPojo. The migration to Clean Architecture is **largely transparent** to these domains.

**What Changes:** When orders are created, they should use the new `/product-module` endpoint instead of `/data/products`.

**What Stays the Same:** Payment and Mailing implementations don't need to change—they already read product data from embedded OrderPojo objects.

**Next Step:** Complete the Clean Architecture implementation (POST, PUT, DELETE), then migrate order creation logic and clean up legacy code.
