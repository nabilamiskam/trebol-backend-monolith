# Clean Product Migration

## Overview

This branch migrates the **Product Information Management** bounded context from the legacy 3-layer monolithic architecture to a **Clean, Hexagonal Architecture** with Domain-Driven Design principles.

**Legacy:** `DataProductsController` (`/data/products` endpoint) - CRUD operations in 3-layer pattern  
**New:** `ProductController` (`/product-module` endpoint) - Use-case driven, clean architecture 
---

## Architecture Approach

### Design Patterns
- **Clean Architecture** – Organized into distinct layers. 
- **Hexagonal (Ports & Adapters)** – Boundaries between layers enforced via interfaces
- **Domain-Driven Design (DDD)** – Product as a core subdomain with its own bounded context
- **Strangler Fig Pattern** – Both endpoints coexist; gradual migration away from legacy
- **Anti-Corruption Layer (ACL)** – Bridge between legacy `Product` JPA entity and new `ProductResult` domain model

---

## What's Being Migrated

| Operation | Legacy Endpoint | New Endpoint | Status |
|-----------|-----------------|--------------|--------|
| List Products | `GET /data/products` | `GET /product-module` | ✅ |
| Get Single Product | N/A | `GET /product-module/{id}` | ✅ |
| Create Product | `POST /data/products` | `POST /product-module` | ✅ |
| Update Product (Full) | `PUT /data/products` | `PUT /product-module/{id}` | ✅ |
| Partial Update | `PATCH /data/products` | `PATCH /product-module` | ✅ |
| Delete Product | `DELETE /data/products` | `DELETE /product-module/{id}` | ✅ |

---

## Running Tests

```bash
# All tests
mvn clean test

# Product migration tests only
mvn test -Dtest=Product*

# Specific test class
mvn test -Dtest=ProductRepositoryAdapterTest

---

## Notes

- **Backward Compatibility:** Legacy `/data/products` endpoint remains active (Strangler Fig)
- **Database:** Uses H2 for tests

---

## References

- **Clean Architecture:** [Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- **Domain-Driven Design:** [Eric Evans](https://www.domainlanguage.com/ddd/)
- **Strangler Fig Pattern:** [Martin Fowler](https://martinfowler.com/bliki/StranglerFigApplication.html)
- **Hexagonal Architecture:** [Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
