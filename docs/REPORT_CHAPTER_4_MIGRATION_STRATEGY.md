# Chapter 4: Migration and Implementation Strategy

The Product bounded context was migrated incrementally using the Strangler Fig Pattern. This approach avoids a big-bang cutover and allows the legacy and refactored implementations to coexist while the new slice is validated.

## 4.1 Migration approach

The refactor was organized as a phased migration rather than a full rewrite. Reads were routed first, because they are easier to validate and can be switched back to the legacy path if needed. Writes remained on the legacy implementation during the early phases so that operational risk stayed low.

This produced a hybrid runtime model:

- the new `org.trebol.product` slice handles the target architecture,
- the legacy `org.trebol.jpa` code remains available as a fallback,
- read traffic can be routed through the Anti-Corruption Layer,
- write operations stay on the legacy path until the migration is complete.

Figure 4.1 can show the coexistence of both implementations during the transition.

## 4.2 Vertical slicing

The migration was implemented feature by feature instead of layer by layer. Each vertical slice covers a complete request path from controller to domain to persistence adapter.

This approach was selected for three practical reasons:

- each slice remains buildable and testable on its own,
- integration issues surface earlier because the feature path is exercised end to end,
- the first slice becomes a reusable template for later slices.

The read path was prioritized first, followed by the remaining CRUD operations in a controlled order.

## 4.3 Slice structure

Each slice follows the same structure:

1. Inbound adapter: `ProductController` receives the HTTP request and maps it into a command or query.
2. Application layer: `ProductApplicationService` coordinates the use case.
3. Domain layer: `ProductAggregate` and the value objects enforce business rules.
4. Outbound adapter: `ProductRepositoryAdapter` translates domain operations to JPA persistence.

This order keeps the core business model isolated from transport and storage concerns.

## 4.4 Implementation order

The implementation within each slice follows a bottom-up sequence:

- define the domain model and invariants first,
- add the application use case around the domain contract,
- implement the persistence adapter and mapper,
- connect the controller to the application service,
- validate the slice with tests before moving to the next feature.

This sequence reduces rework because outer layers are built to match a stable core instead of shaping the core around framework details.

## 4.5 Validation strategy

The migration was verified at multiple levels:

- domain unit tests check value object rules and aggregate behavior,
- adapter integration tests verify persistence mapping against the test database,
- controller tests confirm request handling, validation, and JSON response shape,
- full-stack tests exercise the product flow through the application runtime.

These checks were used to confirm that the new slice behaves correctly before it is promoted further.

## 4.6 Coexistence and fallback

The hybrid stage keeps the system safe while migration work continues.

- legacy writes remain active,
- read routing can move through the ACL path,
- the legacy implementation is still available as fallback,
- the new slice can be expanded without forcing immediate removal of the old code.

This staged coexistence reduces deployment risk and supports rollback if a regression is detected.
