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
- Domain (Core)
  - Aggregate: `Order` (id, date, totals, `OrderStatus`, `Address`es, `List<OrderDetail>`)
  - Value objects: `OrderDetail` (productId, units, unitValue), `OrderStatus` (code, name)
  - Domain service: `OrderWorkflow` enforcing transitions via enum/code, not string comparisons
  - Domain events: `OrderConfirmed`, `OrderRejected`, `OrderCompleted` (optional/outbox)
  - Ports: `OrderRepository`, `OrderStatusRepository`, `OrderDetailRepository`, `NotificationPort`
- Application (Use Cases)
  - `StartPayment`, `AbortPayment`, `FailPayment`, `MarkPaid`, `ConfirmOrder`, `RejectOrder`, `CompleteOrder`
  - Responsibilities: load aggregate via ports, invoke domain transitions, persist, publish events/notifications, return DTOs
  - DTO mappers: map between `OrderPojo`/`OrderDetailPojo` and domain models
- Adapters (Inbound/Outbound)
  - Inbound (web): REST controller mapping endpoints to use cases; no direct repository/QueryDSL access
  - Outbound (persistence): Spring Data JPA implementing domain ports; entity↔domain mappers at adapter boundary
  - Outbound (notifications): email/SMS adapter implementing `NotificationPort` (MailingService)

## Package Refactoring Proposal
- `org.trebol.order.domain` → `Order`, `OrderDetail`, `OrderStatus`, `OrderRepository` (port), `OrderWorkflow`
- `org.trebol.order.application` → use-case classes, DTO mappers
- `org.trebol.order.adapters.in.web` → `DataOrdersController` (thin, delegates to use cases)
- `org.trebol.order.adapters.out.persistence` → JPA repositories, QueryDSL specs, entity↔domain mappers
- `org.trebol.order.adapters.out.notifications` → mailing adapter

## Key Improvements
- Replace string-based status checks with enum/code-based transitions in domain
- Move status logic from controller to application use cases (controller becomes input adapter)
- Access repositories only through ports in use cases; remove direct predicate/sort from controller
- Centralize converters/mappers at adapter boundaries; domain remains pure
- Improve testability: unit-test domain transitions; integration-test adapters and use cases

## Migration Steps
- Introduce domain models and status enum; adapt converters accordingly
- Define repository/notification ports; implement adapters around existing Spring Data/MailingService
- Extract use-case classes from `OrdersProcessServiceImpl`; wire controller to use cases
- Move predicate/sort concerns to persistence adapter; preserve API compatibility
- Add domain tests for transitions; keep existing API tests passing during refactor

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
         |  Aggregate: Order                                         |
         |  Value Objects: OrderDetail, OrderStatus (enum/code)      |
         |  Domain Service: OrderWorkflow (enforces transitions)     |
         |  Ports:                                                   |
         |    OrderRepository, OrderStatusRepository,                |
         |    OrderDetailRepository, NotificationPort                |
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
