# Chapter 1 — Introduction & Project Scope

1.1 Baseline and migration scope

This report documents a targeted refactor of the Product bounded context inside the Trébol backend monolith. The legacy codebase is organized in conventional technical layers (presentation, business services, data access). In practice, however, the product area exhibits significant coupling: JPA annotations, Spring Data repositories, and QueryDSL predicate composition leak into service code and controller plumbing. That coupling raises maintenance costs and prevents fast, framework-free validation of core business rules.

The refactor is intentionally incremental and low-risk. Rather than a full rewrite of the monolith, the project applies the Strangler Fig Pattern to migrate one vertical domain (product management) into a Clean Architecture slice. The new `org.trebol.product` module is introduced alongside legacy modules. An Anti‑Corruption Layer (ACL) provides a reversible read bridge so legacy consumers can read authoritative product data from the new module while writes continue to use the legacy persistence path as a safe fallback.

For testing and validation, both the legacy and new code run inside the same local H2 file-based test database (`./data/trebol`). The test setup preserves separation at the package and adapter level while allowing end-to-end checks that cover the ACL bridge, fallback behavior, and persistence adapter round-trips.

1.2 Engineering objectives

The engineering goal is constrained and measurable: migrate the Product domain to a Clean Architecture perimeter while keeping runtime behavior stable and minimizing deployment risk. Success is evaluated against four operational objectives:

- Concentric-ring insulation: move business policies into a domain core implemented in plain Java types and value objects, minimizing direct runtime dependencies on framework APIs inside the domain.
- Interface inversion compliance: define persistence contracts (repository ports) inwards so that the business core expresses what it needs, and outbound adapters implement those contracts.
- External contract parity: keep public REST endpoints, JSON wire formats, and security access controls stable so downstream integrators are not required to change.
- Multi-tier testing security: validate the migration with a pyramid of automated tests that run from fast domain-level unit tests to full-context integration tests (MockMvc / SpringBoot test) that exercise ACL reads, legacy fallbacks, and transaction guarantees.

1.3 Report purpose and audience

This report is intended for engineering stakeholders, platform owners, and reviewers who need an operational view of the migration work and objective evidence that the refactor preserves runtime safety. It presents the baseline observations, the target architecture, a concise list of implemented changes, and the testing evidence used to validate each claim. The document is evidence-driven: each architectural change is paired with the tests and artifacts that prove its correctness.

1.4 Scope limitations

The migration focuses exclusively on the Product bounded context and its immediate integration points (product lists, order product resolution). It does not attempt to modernize other domains (checkout, users, order processing) within the same iteration. Writes remain on the legacy path until additional validation (price round-trip tests, Testcontainers integration tests, and consumer migration tests) are complete.

1.5 How to read the report

The rest of the report is organized as follows:

- Chapter 2 — Problem statement and legacy coupling analysis (baseline evidence).
- Chapter 3 — Target architecture and decoupling strategy (design and package layout).
- Chapter 4 — Migration approach and implementation highlights (code changes and ACL wiring).
- Chapter 5 — Testing strategy and validation results (unit, adapter, controller, and E2E tests used as proof).
- Chapter 6 — Operational checklist, remaining tasks, and recommendation for disabling legacy fallback.

Appendices include test lists, important code links, and diagrams (ACL flow, package topology, and boundary inversion). Where the report claims behavioral changes, it links directly to the test artifact that demonstrates the proof.
