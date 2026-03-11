# Order Management Architecture

## Scope
Covers the Orders domain: controllers, services, repositories, entities, converters, predicates/sort, and workflow/state transitions.

## Current Three-Layer Architecture
- Presentation (Web/API)
  - Controller: [src/main/java/org/trebol/api/controllers/DataOrdersController.java](src/main/java/org/trebol/api/controllers/DataOrdersController.java)
  - Endpoints: list/create/update/patch/delete + workflow (`/confirmation`, `/rejection`, `/completion`)
  - Collaborators: `PaginationService`, `SortSpecParserService`, `OrdersCrudService`, `OrdersPredicateService`, `OrdersProcessService`, optional `MailingService`
- Application (Services/Use Cases)
  - CRUD service: [src/main/java/org/trebol/jpa/services/crud/impl/OrdersCrudServiceImpl.java](src/main/java/org/trebol/jpa/services/crud/impl/OrdersCrudServiceImpl.java)
    - Responsibilities: `readOne(predicate)` assembling `OrderPojo` with addresses and details; `flushPartialChanges` blocking edits when processed unless `apiProperties.isAbleToEditOrdersAfterBeingProcessed()`; `getExisting(OrderPojo)` by `buyOrder`
    - Dependencies: `OrdersRepository`, `OrdersConverterService`, `AddressesConverterService`, `ApiProperties`
  - Workflow service: [src/main/java/org/trebol/api/services/impl/OrdersProcessServiceImpl.java](src/main/java/org/trebol/api/services/impl/OrdersProcessServiceImpl.java)
    - Use cases: `markAsStarted`, `markAsAborted`, `markAsFailed`, `markAsPaid`, `markAsConfirmed`, `markAsRejected`, `markAsCompleted`
    - Logic: validates current status, looks up next `OrderStatus` by name, updates status/token via repository, returns converted `OrderPojo` enriched with details/products
    - Dependencies: `OrdersCrudService`, `OrdersRepository`, `OrderDetailsRepository`, `OrderStatusesRepository`, `OrdersConverterService`, `ProductsConverterService`
  - Cross-cutting: predicates and sorting via `OrdersPredicateService`, `OrdersSortSpec`
- Persistence (Repositories/Entities)
  - Repositories: [src/main/java/org/trebol/jpa/repositories/OrdersRepository.java](src/main/java/org/trebol/jpa/repositories/OrdersRepository.java), `OrderDetailsRepository`, `OrderStatusesRepository`
    - Custom queries: `findByTransactionToken`, `findByIdWithDetails`, `setStatus`, `setTransactionToken`
  - Entities: [src/main/java/org/trebol/jpa/entities/Order.java](src/main/java/org/trebol/jpa/entities/Order.java), `OrderDetail`, `OrderStatus`, `Address`
  - Converters: `OrdersConverterService`, `AddressesConverterService`, `ProductsConverterService`

## Relations Overview
- `DataOrdersController` → `OrdersCrudService` / `OrdersPredicateService` / `SortSpecParserService` for data operations
- `DataOrdersController` → `OrdersProcessService` for status transitions (+ `MailingService` notifications)
- `OrdersCrudServiceImpl` → `OrdersRepository` (read/save), converters (entity↔pojo), `AddressesConverterService`
- `OrdersProcessServiceImpl` → `OrdersRepository` (status/token updates) + `OrderDetailsRepository` (line items) + `OrderStatusesRepository` (status lookups)
- Repositories → Entities (Order aggregate references status, details, addresses)

## State Model
Status names and codes (as observed):
- 0: Pending
- 1: Payment Started
- 2: Paid (Unconfirmed)
- 3: Paid (Confirmed)
- 4: Rejected
- 5: Payment Cancelled / Aborted
- 6: Payment Failed
- 7: Completed

Example transition sequences:
- Pending → Started → Paid → Confirmed → Completed
- Pending → Started → Paid → Rejected
- Started → Aborted/Failed

## Clean/Hexagonal Architecture (Target)

**Design Note:** Uses **anemic domain entities** (data containers without business methods) with business logic in domain services. This is a valid architectural choice where entities focus on data structure and relationships, while domain services encapsulate business rules.

- Domain (Core)
  - **Entities (Anemic):** `Order`, `OrderDetail`, `OrderStatus` (data-only, no business methods)
    - `Order`: id, date, totals, status, addresses, details (relationships preserved)
    - `OrderDetail`: productId, units, unitValue
    - `OrderStatus`: code, name (consider converting to enum for type safety)
  - **Domain Services:** Business logic extracted from entities
    - `OrderWorkflow`: enforces status transitions via enum/code (not string comparisons)
    - `OrderTotalsCalculator`: computes net value, taxes (19%), transport, total
    - `OrderValidator`: checks if order can be modified based on status
  - **Domain Events:** `OrderConfirmed`, `OrderRejected`, `OrderCompleted` (optional/outbox pattern)
  - **Ports (Interfaces):** `OrderRepository`, `OrderStatusRepository`, `OrderDetailRepository`, `NotificationPort`
- Application (Use Cases)
  - **Use Cases:** `StartPayment`, `AbortPayment`, `FailPayment`, `MarkPaid`, `ConfirmOrder`, `RejectOrder`, `CompleteOrder`
  - **Responsibilities:** 
    - Load entities via repository ports
    - Invoke domain services (OrderWorkflow, OrderTotalsCalculator)
    - Persist changes through ports
    - Publish domain events / trigger notifications
    - Return DTOs to presentation layer
  - **DTO Mappers:** Map between `OrderPojo`/`OrderDetailPojo` and domain entities (boundary translation)
- Adapters (Inbound/Outbound)
  - **Inbound (Web):** REST controllers delegate to use cases; no direct repository/QueryDSL access
  - **Outbound (Persistence):** Spring Data JPA repositories implement domain ports; keep JPA annotations at adapter boundary
  - **Outbound (Notifications):** Email/SMS adapter implementing `NotificationPort` (MailingService)
  - **Outbound (Payment):** Payment gateway adapter implementing `PaymentPort` (WebPay Plus decoupled)

## Package Refactoring Proposal
- `org.trebol.order.domain`
  - `entities`: `Order`, `OrderDetail`, `OrderStatus` (anemic entities - data only)
  - `services`: `OrderWorkflow`, `OrderTotalsCalculator`, `OrderValidator` (business logic)
  - `ports`: `OrderRepository`, `OrderStatusRepository`, `NotificationPort` (interfaces)
- `org.trebol.order.application`
  - `usecases`: `ConfirmOrder`, `RejectOrder`, `CompleteOrder`, etc.
  - `mappers`: DTO ↔ Entity translation at boundary
- `org.trebol.order.adapters.in.web` 
  - `DataOrdersController` (thin, delegates to use cases)
- `org.trebol.order.adapters.out.persistence` 
  - JPA repositories implementing domain ports
  - QueryDSL specifications
- `org.trebol.order.adapters.out.notifications` 
  - Mailing adapter implementing `NotificationPort`
- `org.trebol.order.adapters.out.payment`
  - Payment gateway adapter implementing `PaymentPort`

## Key Improvements
- **Anemic entities accepted**: Business logic in domain services (OrderWorkflow, OrderTotalsCalculator), not entity methods
- **Replace string-based status checks**: Use enum/code-based transitions in `OrderWorkflow` domain service
- **Extract business logic from converter**: Move tax calculation (19%) to `OrderTotalsCalculator` domain service
- **Reduce converter dependencies**: OrdersConverterService has 10+ repositories - split into mappers (simple conversion) + domain services (business logic)
- **Controller as thin adapter**: Move all status transition logic to application use cases
- **Repository access through ports**: Use cases depend on port interfaces, not concrete JPA repositories
- **Decouple payment provider**: Abstract PaymentService behind `PaymentPort` to easily swap WebPay Plus
- **Improve testability**: 
  - Unit-test domain services (no DB, no frameworks)
  - Integration-test adapters with real infrastructure
  - Test use cases with port mocks

## Migration Steps
- **Phase 1: Extract domain services from converter**
  - Create `OrderTotalsCalculator` domain service (extract 19% tax calculation from OrdersConverterService)
  - Create `OrderValidator` domain service (extract status-based validation rules)
  - Reduce OrdersConverterService dependencies from 10+ to simple entity↔DTO mapping
- **Phase 2: Introduce status enum and OrderWorkflow**
  - Create `OrderStatusCode` enum (PENDING=0, STARTED=1, PAID=2, etc.)
  - Build `OrderWorkflow` domain service with code-based transitions (replace string comparisons)
  - Keep OrderStatus entity for backwards compatibility, add enum mapping
- **Phase 3: Define repository ports**
  - Create port interfaces: `OrderRepository`, `OrderStatusRepository`, `NotificationPort`, `PaymentPort`
  - Implement adapters using existing Spring Data repositories and MailingService
  - Abstract WebPay Plus behind PaymentPort for decoupling
- **Phase 4: Extract use-case classes**
  - Split OrdersProcessServiceImpl into individual use cases (`ConfirmOrder`, `RejectOrder`, etc.)
  - Wire controller to use cases instead of direct service calls
  - Move DTO mapping to application layer boundaries
- **Phase 5: Reorganize packages**
  - Move entities to `domain.entities` (keep anemic)
  - Move domain services to `domain.services`
  - Move use cases to `application.usecases`
  - Separate adapters by type (web, persistence, notifications, payment)
- **Phase 6: Testing strategy**
  - Add unit tests for domain services (no DB/framework dependencies)
  - Add integration tests for adapters (with real Spring Data, email service mocks)
  - Keep existing API tests passing throughout refactor

## ASCII Diagrams

Current 3-Layer Architecture

```
+------------------------------ Presentation ------------------------------+
|                                                                          |
|  DataOrdersController                                                    |
|    - GET/POST/PUT/PATCH/DELETE /data/orders                              |
|    - POST /data/orders/{confirmation|rejection|completion}               |
|                                                                          |
|  Collaborators: PaginationService, SortSpecParserService,                |
|                 OrdersCrudService, OrdersPredicateService,               |
|                 OrdersProcessService, MailingService (optional)          |
+------------------------------------|-------------------------------------+
                                     v
+------------------------------ Application -------------------------------+
|                                                                          |
|  OrdersCrudServiceImpl        OrdersProcessServiceImpl                   |
|    - readOne()                  - markAsStarted/Aborted/Failed           |
|    - flushPartialChanges()      - markAsPaid/Confirmed/Rejected/Completed|
|    - getExisting()                                                    |
|                                                                          |
|  Cross-cutting: OrdersPredicateService, OrdersSortSpec, converters       |
+------------------------------------|-------------------------------------+
                                     v
+------------------------------ Persistence -------------------------------+
|                                                                          |
|  OrdersRepository     OrderDetailsRepository     OrderStatusesRepository |
|   - findBy...          - findBySellId()           - findByName()         |
|   - setStatus()        (line items)                                      |
|   - setTransactionToken()                                                |
|                                                                          |
|  Entities: Order, OrderDetail, OrderStatus, Address                      |
+--------------------------------------------------------------------------+

Mail notifications (optional):

  DataOrdersController --(on workflow calls)--> MailingService
```

Order Status Transitions

```
Pending (0)
  |
  v
Started (1) -----> Aborted (5)
  |  \
  |   \-> Failed (6)
  v
Paid - Unconfirmed (2) -----> Rejected (4)
  |
  v
Confirmed (3)
  |
  v
Completed (7)
```

Target Clean/Hexagonal Architecture

```
         +--------------------- Inbound Adapter ---------------------+
         |  Web (REST): DataOrdersController                        |
         +------------------------------|----------------------------+
                                        v
         +---------------------- Application Layer ------------------+
         |  Use Cases:                                              |
         |    StartPayment / AbortPayment / FailPayment             |
         |    MarkPaid / ConfirmOrder / RejectOrder / CompleteOrder |
         |                                                          |
         |  - Orchestrate domain, map DTOs, call ports              |
         +------------------------------|----------------------------+
                                        v
         +------------------------ Domain Layer ---------------------+
         |  Entities (Anemic): Order, OrderDetail, OrderStatus       |
         |    - Data containers only (no business methods)           |
         |                                                           |
         |  Domain Services (Business Logic):                        |
         |    - OrderWorkflow (status transitions via enum/code)     |
         |    - OrderTotalsCalculator (net, tax 19%, total)          |
         |    - OrderValidator (can modify? based on status)         |
         |                                                           |
         |  Ports (Interfaces):                                      |
         |    OrderRepository, OrderStatusRepository,                |
         |    NotificationPort, PaymentPort                          |
         +-------------------|-------------------|-------------------+
                             |                   |
                             v                   v
    +------------ Outbound Adapter -------------+  +----- Outbound Adapter -----+
    | Persistence (Spring Data JPA)             |  | Notifications (Mail)       |
    | - Repo impls for domain ports             |  | - MailingService adapter   |
    | - Entity<->Domain mappers at boundary     |  |   implements Notification  |
    +-------------------------------------------+  +----------------------------+
```

## References
- Controller: [src/main/java/org/trebol/api/controllers/DataOrdersController.java](src/main/java/org/trebol/api/controllers/DataOrdersController.java)
- CRUD service: [src/main/java/org/trebol/jpa/services/crud/impl/OrdersCrudServiceImpl.java](src/main/java/org/trebol/jpa/services/crud/impl/OrdersCrudServiceImpl.java)
- Workflow service: [src/main/java/org/trebol/api/services/impl/OrdersProcessServiceImpl.java](src/main/java/org/trebol/api/services/impl/OrdersProcessServiceImpl.java)
- Repository: [src/main/java/org/trebol/jpa/repositories/OrdersRepository.java](src/main/java/org/trebol/jpa/repositories/OrdersRepository.java)
- Entities: [src/main/java/org/trebol/jpa/entities/Order.java](src/main/java/org/trebol/jpa/entities/Order.java), [src/main/java/org/trebol/jpa/entities/OrderDetail.java](src/main/java/org/trebol/jpa/entities/OrderDetail.java), [src/main/java/org/trebol/jpa/entities/OrderStatus.java](src/main/java/org/trebol/jpa/entities/OrderStatus.java)
- Study notes: [.github/docs/study/DataOrdersControllerTest.md](.github/docs/study/DataOrdersControllerTest.md)
