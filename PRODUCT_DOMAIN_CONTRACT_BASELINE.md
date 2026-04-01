# Product Domain Contract Baseline

This document locks the current HTTP contract for Product domain endpoints during the safe migration to clean architecture. All changes to these contracts must be explicitly planned and reviewed.

## Endpoints & Contracts

### GET /data/products
**Purpose**: List all products with pagination and filtering

**Request Parameters**
- `pageIndex` (query): Page number (0-indexed)
- `pageSize` (query): Items per page
- `sortBy` (query, optional): Sort column
- `direction` (query, optional): ASC or DESC

**Response (200)**
```json
{
  "items": [
    {
      "id": 1,
      "code": "PROD001",
      "name": "Product Name",
      "price": 29.99,
      "type": {
        "id": 1,
        "code": "TYPE_CODE",
        "name": "Type Name"
      },
      "category": {
        "id": 1,
        "code": "CAT_CODE",
        "name": "Category Name"
      },
      "description": "Product description",
      "isActive": true,
      "imageURLList": [
        {
          "id": 1,
          "url": "http://example.com/image.jpg"
        }
      ]
    }
  ],
  "totalCount": 100
}
```

---

### POST /data/products
**Purpose**: Create a new product

**Request Body**
```json
{
  "code": "PROD001",
  "name": "Product Name",
  "price": 29.99,
  "type": {
    "id": 1
  },
  "category": {
    "id": 1
  },
  "description": "Product description",
  "isActive": true
}
```

**Response (201)**
```json
{
  "id": 1,
  "code": "PROD001",
  "name": "Product Name",
  "price": 29.99,
  "type": {
    "id": 1,
    "code": "TYPE_CODE",
    "name": "Type Name"
  },
  "category": {
    "id": 1,
    "code": "CAT_CODE",
    "name": "Category Name"
  },
  "description": "Product description",
  "isActive": true,
  "imageURLList": []
}
```

---

### GET /data/products/{id}
**Purpose**: Get a single product by ID

**Response (200)**
Same as POST /data/products response format

**Response (404)**
```json
{
  "code": "NOTFOUND_01",
  "message": "No such entity"
}
```

---

### PUT /data/products?id={id}
**Purpose**: Replace entire product

**Request Query Parameters**
- `id`: Product ID (required)

**Request Body**
```json
{
  "code": "PROD001",
  "name": "Updated Name",
  "price": 39.99,
  "type": {
    "id": 1
  },
  "category": {
    "id": 1
  },
  "description": "Updated description",
  "isActive": true
}
```

**Response (200)**
Same format as GET response

**Response (400 - No query params)**
```json
{
  "code": "REJECTED_01",
  "message": "Bad input"
}
```

**Response (404)**
```json
{
  "code": "NOTFOUND_01",
  "message": "No such entity"
}
```

---

### PATCH /data/products?id={id}
**Purpose**: Partially update product

**Request Query Parameters**
- `id`: Product ID (required)

**Request Body** (all fields optional)
```json
{
  "name": "Updated Name",
  "price": 39.99,
  "isActive": false
}
```

**Response (200)**
Same format as GET response

**Response (400 - No query params)**
```json
{
  "code": "REJECTED_01",
  "message": "Bad input"
}
```

**Response (404)**
```json
{
  "code": "NOTFOUND_01",
  "message": "No such entity"
}
```

---

### DELETE /data/products?id={id}
**Purpose**: Delete product

**Request Query Parameters**
- `id`: Product ID (required)

**Response (204)**
No content

**Response (400 - No query params)**
```json
{
  "code": "REJECTED_01",
  "message": "Bad input"
}
```

**Response (404)**
```json
{
  "code": "NOTFOUND_01",
  "message": "No such entity"
}
```

---

## Error Codes Reference

Mapped by `ExceptionsControllerAdvice.java`:

| Exception | HTTP Status | Code | Message |
|-----------|-------------|------|---------|
| EntityNotFoundException | 404 | NOTFOUND_01 | No such entity |
| EntityExistsException | 400 | EXISTS_01 | Entity already exists |
| BadInputException | 400 | REJECTED_01 | Bad input |
| MethodArgumentNotValidException | 400 | REJECTED_02 | Validation error |

---

## Migration Guardrails

1. **Immutable Endpoints**: Paths must remain unchanged during migration
2. **Immutable Response Shape**: JSON structure of success responses must be preserved
3. **Immutable Status Codes**: HTTP status for each endpoint+scenario locked
4. **Immutable Error Codes**: Error code strings cannot change
5. **Backward Compatibility**: Old endpoints must continue working throughout migration
6. **Feature Flags**: New implementation hidden behind flags until ready
7. **Gradual Traffic**: Old→New routing controlled incrementally
8. **Schema Stability**: No database changes during migration
9. **Regression Tests**: HTTP-level tests verify all contracts remain locked

---

## Verification

All contracts verified against:
- `DataProductsController.java` - Current endpoint definitions
- `DataProductsControllerTest.java` - Existing unit tests
- `ExceptionsControllerAdvice.java` - Error handling mapping

Last verified: 2026-04-01
