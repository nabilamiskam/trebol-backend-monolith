# Chapter 3: Target Architecture and Decoupling Strategy

To eliminate the technical debt, framework coupling, and change amplification identified in the legacy baseline, the Product bounded context was restructured around a Clean Architecture and Hexagonal Architecture style. The goal of the target design is to keep business rules inside an insulated core while pushing frameworks, persistence, and web concerns to the outside of the system.

## 3.1 Target topology and package layout

The new Product slice is isolated under `org.trebol.product`. Its package structure follows concentric layers, with dependencies allowed to point inward only.

```text
src/main/java/org/trebol/product
├── domain/
│   ├── aggregate/
│   ├── vo/
│   └── port/
├── application/
│   ├── usecase/
│   ├── command/
│   ├── query/
│   ├── result/
│   └── service/
├── adapter/
│   ├── inbound/
│   │   ├── web/
│   │   └── dto/
│   └── outbound/
│       └── persistence/
└── infrastructure/
```

Figure 3.1 shows the product context as a concentric package-per-layer structure. The inner layers contain the business core, while the outer layers contain framework integration and technical adapters.

The domain layer contains the policy core. It defines the `ProductAggregate`, immutable value objects, and the repository port used by the core. This layer is written in plain Java and does not depend on Spring, JPA, or database annotations.

The application layer coordinates use cases such as creating, listing, and retrieving products. It accepts immutable command and query objects and returns result objects that are simple data carriers. This layer orchestrates business flow without knowing about HTTP, SQL, or persistence frameworks.

The adapter layer translates between the application core and the outside world. Inbound adapters handle HTTP requests and map them to commands or queries. Outbound adapters map domain objects to persistence structures and back again.

The infrastructure layer is the framework edge. It contains the Spring configuration that wires the module together explicitly.

## 3.2 Decoupling strategy

The target architecture is designed to remove direct dependencies from business logic to framework code. Instead of letting controllers and services talk directly to JPA entities or Spring Data repositories, the system now routes requests through explicit application use cases and repository ports.

This separation makes the architecture easier to reason about for three reasons. First, domain rules are expressed in a small set of pure Java types. Second, application flow is centralized in use cases rather than spread across generic service helpers. Third, adapters can be replaced or rewritten without changing the core business model.

## 3.3 Port and adapter boundaries

The design uses ports to define the boundaries between the core and the outside world.

Primary ports are the inbound interfaces exposed by the application core. In this implementation, the primary ports are the product use cases implemented by `ProductApplicationService`. `ProductController` acts as the driving adapter: it receives HTTP requests, converts them into commands or queries, and delegates execution to the application layer.

Secondary ports are the outbound contracts used by the core to reach external systems. `ProductRepository` is the main secondary port. It expresses persistence behavior in terms of domain concepts such as `ProductAggregate`, `ProductId`, and `ProductCode`. The concrete JPA implementation lives outside the core in the outbound persistence adapter.

## 3.4 Dependency inversion rationale

Placing the repository contract inside the inner layer is a deliberate application of the Dependency Inversion Principle. The business core defines what it needs from storage, while the persistence layer provides the implementation.

This inversion is important because it prevents JPA, SQL, Spring Data, and database schema details from leaking into the domain model. The repository contract belongs to the core because the core owns the semantics of lookup, uniqueness, save, update, and delete operations. Infrastructure must conform to those rules, not define them.

The result is a one-way architectural flow: the core owns the contract, and the adapter fulfills it.

## 3.5 Operational impact

The target architecture improves maintainability and testability. A database migration, persistence optimization, or adapter replacement can be made without rewriting the product use cases, as long as the adapter continues to satisfy the repository contract.

It also improves automated testing. The core can be exercised with in-memory or mocked repository implementations, allowing business rules to be validated without starting the full framework stack. That reduces test cost and makes regressions easier to detect early.
