# Feature Trace: <DataOrdersController.md>

## API entry
- Controller class: DataOrdersController
- Endpoint (HTTP method + path):
  - GET /data/orders (with optional buyOrder filter)
  - POST /data/orders
  - PUT /data/orders
  - PATCH /data/orders
  - DELETE /data/orders
  - POST /data/orders/confirmation
  - POST /data/orders/rejection
  - POST /data/orders/completion
- Controller method: readMany(), create(), update(), partialUpdate(), delete(), confirmSell(), rejectSell(), completeSell()

## Application flow
- API service interface:
  - org.trebol.api.services.PaginationService
  - org.trebol.api.services.OrdersProcessService
- API service implementation:
  - org.trebol.api.services.impl.PaginationServiceImpl
  - org.trebol.api.services.impl.OrdersProcessServiceImpl
- Main service method(s) used:
  - determineRequestedPageIndex(), determineRequestedPageSize()
  - markAsConfirmed(), markAsRejected(), markAsCompleted()
- Sort parsing interface: org.trebol.jpa.services.SortSpecParserService
- Sort parsing method used: parse(orderSpecMap, queryMap)
- Note: Controller directly orchestrates JPA services for business/persistence logic plus PaginationService and OrdersProcessService for order workflow.
- Injected domain/persistence services:
  - org.trebol.jpa.services.crud.OrdersCrudService
  - org.trebol.jpa.services.predicates.OrdersPredicateService
  - org.trebol.jpa.services.SortSpecParserService
  - org.trebol.mailing.MailingService (optional)

## Persistence flow
- JPA service(s) used (crud/patch/conversion/predicate/sort):
  - Crud: org.trebol.jpa.services.crud.OrdersCrudService (impl: org.trebol.jpa.services.crud.impl.OrdersCrudServiceImpl)
  - Patch: org.trebol.jpa.services.patch.OrdersPatchService (impl: org.trebol.jpa.services.patch.impl.OrdersPatchServiceImpl)
  - Conversion: org.trebol.jpa.services.conversion.OrdersConverterService (impl: org.trebol.jpa.services.conversion.impl.OrdersConverterServiceImpl)
  - Conversion: org.trebol.jpa.services.conversion.AddressesConverterService (for billing/shipping addresses)
  - Predicate: org.trebol.jpa.services.predicates.OrdersPredicateService (impl: org.trebol.jpa.services.predicates.impl.OrdersPredicateServiceImpl)
  - Sort: org.trebol.jpa.sortspecs.OrdersSortSpec.ORDER_SPEC_MAP
- Repository interface:
  - org.trebol.jpa.repositories.OrdersRepository
  - org.trebol.jpa.repositories.AddressesRepository (for billing/shipping addresses)
- Entity class(es):
  - org.trebol.jpa.entities.Order
  - org.trebol.jpa.entities.OrderDetail
  - org.trebol.jpa.entities.Address
  - org.trebol.jpa.entities.OrderStatus
- Tables (if known): Not explicitly shown; implied by entities/repositories

## Inputs/Outputs
- Request DTO/model:
  - OrderPojo (create/update/confirmation/rejection/completion body)
  - Map<String,Object> (partialUpdate body)
  - Map<String,String> (query params for read/delete; supports buyOrder filter)
- Response DTO/model:
  - DataPagePojo<OrderPojo> (readMany; default sort by buyOrder descending)
  - void / HTTP status (create: 201, update/patch/delete/confirm/reject/complete: 204)

## Business rules observed
- Validations:
  - Update/partialUpdate require non-empty request params; else BadInputException
  - Orders cannot be edited after status >= 3 (processed) unless apiProperties.isAbleToEditOrdersAfterBeingProcessed() is true
  - Order buyOrder must be non-null/non-zero for getExisting; else Optional.empty()
  - Pagination enforces numeric pageIndex/pageSize; caps pageSize to maxAllowedPageSize
- State transitions:
  - Pending → Started → Paid → Confirmed → Completed
  - Pending → Started → Paid → Rejected (with refund)
  - Started → Aborted/Failed (error states)
  - readMany with buyOrder filter returns single-item page (status 200)
  - readMany without explicit sort defaults to sortBy=buyOrder, order=desc
  - confirmSell: Paid → Confirmed + email notification (if mailingService available)
  - rejectSell: Paid → Rejected + client email notification
  - completeSell: Confirmed → Completed + client email notification
- Error cases:
  - EntityNotFoundException when no order matches filters (update/partialUpdate/readOne)
  - BadInputException for invalid inputs (e.g., missing request params, invalid state transitions)
  - BadInputException when trying to edit processed orders without config override
  - MailingServiceException on email notification failures (from confirmSell/rejectSell/completeSell)
- Authorization (all operations require isAuthenticated()):
  - readMany requires 'orders:read'
  - create requires 'orders:create'
  - update/partialUpdate/confirm/reject/complete require 'orders:update'
  - delete requires 'orders:delete'

## Notes / Questions
- MailingService is optional (@Autowired(required=false)); confirm/reject/complete gracefully skip notifications if null.
- OrderDetail entities hold line-item data for each order.
- OrderStatus codes: 0=Pending, 1=Started, 2=Paid, 3=Confirmed, 4=Rejected, 5=Aborted, 6=Failed, 7=Completed.
- buyOrder filter in readMany is a special case; returns exact match as single-page result.

---

## Three-Layer Architecture (Current – Order Management)
- Presentation: REST controller orchestrating workflows and CRUD
  - Controller: org.trebol.api.controllers.DataOrdersController
  - Endpoints: list/create/update/patch/delete + workflow (`/confirmation`, `/rejection`, `/completion`)
  - Collaborators: PaginationService, SortSpecParserService, OrdersCrudService, OrdersPredicateService, OrdersProcessService, optional MailingService
- Application: services implementing business use cases and CRUD
  - CRUD service: org.trebol.jpa.services.crud.impl.OrdersCrudServiceImpl
    - Responsibilities: `readOne(predicate)` assembling `OrderPojo` with addresses and details; `flushPartialChanges` blocking edits when processed unless `apiProperties.isAbleToEditOrdersAfterBeingProcessed()` is true; `getExisting(OrderPojo)` by `buyOrder`
    - Dependencies: OrdersRepository, OrdersConverterService, AddressesConverterService, ApiProperties
  - Workflow service: org.trebol.api.services.impl.OrdersProcessServiceImpl
    - Use cases: `markAsStarted`, `markAsAborted`, `markAsFailed`, `markAsPaid`, `markAsConfirmed`, `markAsRejected`, `markAsCompleted`
    - Logic: validates current status, looks up next `OrderStatus` by name, updates status/token via repository, returns converted `OrderPojo` enriched with details/products
    - Dependencies: OrdersCrudService, OrdersRepository, OrderDetailsRepository, OrderStatusesRepository, OrdersConverterService, ProductsConverterService
  - Cross-cutting: Predicates and sorting (QueryDSL) via OrdersPredicateService and OrdersSortSpec
- Persistence: JPA repositories and entities
  - Repositories: org.trebol.jpa.repositories.OrdersRepository, OrderDetailsRepository, OrderStatusesRepository
    - Custom queries: `findByTransactionToken`, `findByIdWithDetails`, `setStatus`, `setTransactionToken`
  - Entities: org.trebol.jpa.entities.Order, OrderDetail, OrderStatus, Address
  - Converters: OrdersConverterService, AddressesConverterService, ProductsConverterService

## Relations Overview (Current)
- DataOrdersController → OrdersCrudService / OrdersPredicateService / SortSpecParserService for data operations
- DataOrdersController → OrdersProcessService for status transitions (+ MailingService notifications)
- OrdersCrudServiceImpl → OrdersRepository (read/save), Converters (entity↔pojo), AddressesConverterService
- OrdersProcessServiceImpl → OrdersRepository (status/token updates) + OrderDetailsRepository (line items) + OrderStatusesRepository (status lookups)
- Repositories → Entities (Order aggregate references status, details, addresses)

## Clean/Hexagonal Architecture (Target – Order Management)
- Domain (core): business rules, invariants, aggregates
  - Aggregate: `Order` (id, date, totals, `OrderStatus`, addresses, `List<OrderDetail>`)
  - Value objects: `OrderDetail` (productId, units, unitValue), `OrderStatus` (code, name)
  - Domain services: `OrderWorkflow` encapsulating transitions (start, paid, confirm, reject, complete) using status enum/codes, not strings
  - Domain events: `OrderConfirmed`, `OrderRejected`, `OrderCompleted` (optional for integration/outbox)
  - Ports (interfaces): `OrderRepository`, `OrderStatusRepository`, `OrderDetailRepository`, `NotificationPort`
- Application (use cases): orchestrate domain operations and map DTOs
  - Use cases: `StartPayment`, `AbortPayment`, `FailPayment`, `MarkPaid`, `ConfirmOrder`, `RejectOrder`, `CompleteOrder`
  - Responsibilities: load aggregate via ports, invoke domain transitions, persist, publish events/notifications, return response models
  - DTO mappers: map between `OrderPojo`/`OrderDetailPojo` and domain models
- Adapters (in/out): implement ports and expose APIs
  - Inbound (web): REST controller mapping endpoints to use cases; no direct repository/QueryDSL access
    - Package: `org.trebol.order.adapters.in.web`
  - Outbound (persistence): Spring Data JPA repositories implementing domain ports; entity mappers kept at adapter boundary
    - Package: `org.trebol.order.adapters.out.persistence`
  - Outbound (notifications): email/SMS adapters implementing `NotificationPort` (MailingService)
    - Package: `org.trebol.order.adapters.out.notifications`

## Package Refactoring Proposal
- `org.trebol.order.domain` → Order, OrderDetail, OrderStatus, OrderRepository (port), OrderWorkflow (service)
- `org.trebol.order.application` → use-case classes, DTO mappers
- `org.trebol.order.adapters.in.web` → DataOrdersController (thin, delegates to use cases)
- `org.trebol.order.adapters.out.persistence` → JPA repositories, QueryDSL specs, entity↔domain mappers
- `org.trebol.order.adapters.out.notifications` → Mailing adapter

## Key Improvements
- Status model: replace string comparisons with enum/code-based transitions enforced in domain
- Controller thickness: move all status logic to application use cases; controller becomes input adapter
- Repository access: controller does not access repositories/predicates; only through use cases/ports
- Converters/mappers: centralize mapping at adapter boundaries; domain works with pure models
- Testability: unit-test domain transitions; integration-test adapters

## Migration Steps (Incremental)
- Introduce domain-layer models and status enum; adapt existing converters
- Create ports for repositories/notifications; implement adapters around existing Spring Data repositories/MailingService
- Extract use-case classes from OrdersProcessServiceImpl; wire controller to use cases
- Gradually move predicate/sort concerns to persistence adapter (keep API-compatible responses)
- Add domain tests for transitions and invariants; keep existing API tests passing
