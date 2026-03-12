# Product Domain Migration Checklist

Status values:
- NOT_STARTED
- IN_PROGRESS
- DONE
- N/A

## Core Product

| Status | Old Class | Target Class | Target Folder | Notes |
|---|---|---|---|---|
| NOT_STARTED | org.trebol.api.controllers.DataProductsController | ProductController (replace ProductCleanController) | src/main/java/org/trebol/product/adapter/in/web | Move to use-case orchestration only |
| NOT_STARTED | org.trebol.api.models.ProductPojo | ProductRequest, ProductResponse, ProductDetailResponse | src/main/java/org/trebol/product/adapter/in/web/dto | Split request/response DTOs |
| NOT_STARTED | org.trebol.jpa.services.crud.ProductsCrudService | Create/Get/List/Update/Delete Product use cases | src/main/java/org/trebol/product/application/usecase | Replace CRUD abstraction with use cases |
| NOT_STARTED | org.trebol.jpa.services.crud.impl.ProductsCrudServiceImpl | ProductDomainService + use case handlers | src/main/java/org/trebol/product/domain/service and src/main/java/org/trebol/product/application/usecase | Split mixed concerns |
| NOT_STARTED | org.trebol.jpa.services.conversion.ProductsConverterService | ProductWebMapper + ProductPersistenceMapper | src/main/java/org/trebol/product/adapter/in/web/mapper and src/main/java/org/trebol/product/adapter/out/persistence/mapper | Separate API and persistence mapping |
| NOT_STARTED | org.trebol.jpa.services.conversion.impl.ProductsConverterServiceImpl | Mapper implementations/static mappers | src/main/java/org/trebol/product/adapter/in/web/mapper and src/main/java/org/trebol/product/adapter/out/persistence/mapper | Move converter logic |
| NOT_STARTED | org.trebol.jpa.services.patch.ProductsPatchService | ProductPatchPolicy (or domain update rules) | src/main/java/org/trebol/product/domain/service | Keep patch semantics in domain/application |
| NOT_STARTED | org.trebol.jpa.services.patch.impl.ProductsPatchServiceImpl | UpdateProductUseCase + domain update policy | src/main/java/org/trebol/product/application/usecase/update and src/main/java/org/trebol/product/domain/service | Remove JPA coupling |
| NOT_STARTED | org.trebol.jpa.services.predicates.ProductsPredicateService | ProductQueryService/ProductFilterTranslator | src/main/java/org/trebol/product/application/service or src/main/java/org/trebol/product/adapter/out/persistence/query | Keep filtering boundary clean |
| NOT_STARTED | org.trebol.jpa.services.predicates.impl.ProductsPredicateServiceImpl | ProductQuerySpecification | src/main/java/org/trebol/product/adapter/out/persistence/query | QueryDSL/specification in adapter only |
| NOT_STARTED | org.trebol.jpa.sortspecs.ProductsSortSpec | ProductSortTranslator | src/main/java/org/trebol/product/adapter/out/persistence/query | Map API sort to DB sort |
| NOT_STARTED | org.trebol.jpa.repositories.ProductsRepository | ProductJpaRepository + ProductRepositoryAdapter | src/main/java/org/trebol/product/adapter/out/persistence/jpa | Keep Spring Data behind adapter |
| NOT_STARTED | org.trebol.jpa.entities.Product | ProductJpaEntity + Product (domain model) | src/main/java/org/trebol/product/adapter/out/persistence/jpa and src/main/java/org/trebol/product/domain/model | Split persistence/domain models |
| DONE | org.trebol.application.product.port.ProductRepository | ProductRepository (port) | src/main/java/org/trebol/application/product/port (current) -> src/main/java/org/trebol/product/domain/port (target optional) | Already introduced clean port |
| DONE | org.trebol.application.product.usecase.CreateProductUseCase | CreateProductUseCase | src/main/java/org/trebol/application/product/usecase (current) | Existing clean slice |
| DONE | org.trebol.domain.product.model.Product | Product (domain model) | src/main/java/org/trebol/domain/product/model (current) | Existing clean slice |
| DONE | org.trebol.adapter.web.product.ProductCleanController | ProductController (temporary clean endpoint) | src/main/java/org/trebol/adapter/web/product (current) -> src/main/java/org/trebol/product/adapter/in/web (target optional) | Existing clean slice |
| DONE | org.trebol.adapter.persistence.product.InMemoryProductRepositoryAdapter | InMemoryProductRepositoryAdapter | src/main/java/org/trebol/adapter/persistence/product (current) -> src/main/java/org/trebol/product/adapter/out/persistence/inmemory (target optional) | Existing clean slice |

## Product Category

| Status | Old Class | Target Class | Target Folder | Notes |
|---|---|---|---|---|
| NOT_STARTED | org.trebol.api.controllers.DataProductCategoriesController | ProductCategoryController | src/main/java/org/trebol/productcategory/adapter/in/web | HTTP adapter only |
| NOT_STARTED | org.trebol.api.models.ProductCategoryPojo | ProductCategoryRequest/Response DTOs | src/main/java/org/trebol/productcategory/adapter/in/web/dto | Split API contracts |
| NOT_STARTED | org.trebol.jpa.services.crud.ProductCategoriesCrudService | Category use cases | src/main/java/org/trebol/productcategory/application/usecase | Replace CRUD abstraction |
| NOT_STARTED | org.trebol.jpa.services.crud.impl.ProductCategoriesCrudServiceImpl | CategoryDomainService + use case handlers | src/main/java/org/trebol/productcategory/domain/service and src/main/java/org/trebol/productcategory/application/usecase | Decompose logic |
| NOT_STARTED | org.trebol.jpa.services.conversion.ProductCategoriesConverterService | CategoryWebMapper + CategoryPersistenceMapper | src/main/java/org/trebol/productcategory/adapter/in/web/mapper and src/main/java/org/trebol/productcategory/adapter/out/persistence/mapper | Split mapper concerns |
| NOT_STARTED | org.trebol.jpa.services.conversion.impl.ProductCategoriesConverterServiceImpl | Mapper implementations | src/main/java/org/trebol/productcategory/adapter/in/web/mapper and src/main/java/org/trebol/productcategory/adapter/out/persistence/mapper | Move converter logic |
| NOT_STARTED | org.trebol.jpa.services.patch.ProductCategoriesPatchService | UpdateCategoryUseCase/domain patch policy | src/main/java/org/trebol/productcategory/application/usecase/update and src/main/java/org/trebol/productcategory/domain/service | Remove service-level patching |
| NOT_STARTED | org.trebol.jpa.services.patch.impl.ProductCategoriesPatchServiceImpl | UpdateCategoryUseCase implementation | src/main/java/org/trebol/productcategory/application/usecase/update | Use-case orchestration |
| NOT_STARTED | org.trebol.jpa.services.predicates.ProductCategoriesPredicateService | CategoryQueryService/translator | src/main/java/org/trebol/productcategory/application/service or src/main/java/org/trebol/productcategory/adapter/out/persistence/query | Keep query abstraction |
| NOT_STARTED | org.trebol.jpa.services.predicates.impl.ProductCategoriesPredicateServiceImpl | CategoryQuerySpecification | src/main/java/org/trebol/productcategory/adapter/out/persistence/query | Adapter-owned query logic |
| NOT_STARTED | org.trebol.jpa.sortspecs.ProductCategoriesSortSpec | CategorySortTranslator | src/main/java/org/trebol/productcategory/adapter/out/persistence/query | Adapter-owned sorting |
| NOT_STARTED | org.trebol.jpa.repositories.ProductsCategoriesRepository | ProductCategoryJpaRepository + CategoryRepositoryAdapter | src/main/java/org/trebol/productcategory/adapter/out/persistence/jpa | Port/adapter split |
| NOT_STARTED | org.trebol.jpa.entities.ProductCategory | ProductCategoryJpaEntity + ProductCategory (domain) | src/main/java/org/trebol/productcategory/adapter/out/persistence/jpa and src/main/java/org/trebol/productcategory/domain/model | Split persistence/domain |
| NOT_STARTED | org.trebol.jpa.services.ProductCategoryTreeResolverService | ProductCategoryTreeDomainService (interface) | src/main/java/org/trebol/productcategory/domain/service | Tree business logic |
| NOT_STARTED | org.trebol.jpa.services.impl.ProductCategoryTreeResolverServiceImpl | ProductCategoryTreeDomainServiceImpl + adapter queries | src/main/java/org/trebol/productcategory/domain/service and adapter/out/persistence/query | Separate domain and data access |

## Product List and Product List Item

| Status | Old Class | Target Class | Target Folder | Notes |
|---|---|---|---|---|
| NOT_STARTED | org.trebol.api.controllers.DataProductListsController | ProductListController | src/main/java/org/trebol/productlist/adapter/in/web | HTTP adapter only |
| NOT_STARTED | org.trebol.api.controllers.DataProductListContentsController | ProductListItemController | src/main/java/org/trebol/productlistitem/adapter/in/web | Separate endpoint adapter |
| NOT_STARTED | org.trebol.api.models.ProductListPojo | ProductListRequest/Response DTOs | src/main/java/org/trebol/productlist/adapter/in/web/dto | DTO split |
| NOT_STARTED | org.trebol.jpa.services.crud.ProductListCrudService | ProductList use cases | src/main/java/org/trebol/productlist/application/usecase | Replace CRUD abstraction |
| NOT_STARTED | org.trebol.jpa.services.crud.impl.ProductListsCrudServiceImpl | ProductListDomainService + use case handlers | src/main/java/org/trebol/productlist/domain/service and src/main/java/org/trebol/productlist/application/usecase | Decompose service |
| NOT_STARTED | org.trebol.jpa.services.conversion.ProductListsConverterService | ProductList mappers | src/main/java/org/trebol/productlist/adapter/in/web/mapper and src/main/java/org/trebol/productlist/adapter/out/persistence/mapper | Separate API and persistence mapping |
| NOT_STARTED | org.trebol.jpa.services.conversion.impl.ProductListConverterServiceImpl | ProductList mapper implementations | src/main/java/org/trebol/productlist/adapter/in/web/mapper and src/main/java/org/trebol/productlist/adapter/out/persistence/mapper | Move converter logic |
| NOT_STARTED | org.trebol.jpa.services.patch.ProductListsPatchService | UpdateProductListUseCase/domain patch policy | src/main/java/org/trebol/productlist/application/usecase/update and src/main/java/org/trebol/productlist/domain/service | Keep patch rules clean |
| NOT_STARTED | org.trebol.jpa.services.patch.impl.ProductListPatchServiceImpl | UpdateProductListUseCase implementation | src/main/java/org/trebol/productlist/application/usecase/update | Use-case orchestration |
| NOT_STARTED | org.trebol.jpa.services.predicates.ProductListsPredicateService | ProductListQueryService/translator | src/main/java/org/trebol/productlist/application/service or src/main/java/org/trebol/productlist/adapter/out/persistence/query | Query abstraction |
| NOT_STARTED | org.trebol.jpa.services.predicates.impl.ProductListsPredicateServiceImpl | ProductListQuerySpecification | src/main/java/org/trebol/productlist/adapter/out/persistence/query | Adapter-owned query logic |
| NOT_STARTED | org.trebol.jpa.sortspecs.ProductListsSortSpec | ProductListSortTranslator | src/main/java/org/trebol/productlist/adapter/out/persistence/query | Adapter-owned sorting |
| NOT_STARTED | org.trebol.jpa.repositories.ProductListsRepository | ProductListJpaRepository + ProductListRepositoryAdapter | src/main/java/org/trebol/productlist/adapter/out/persistence/jpa | Port/adapter split |
| NOT_STARTED | org.trebol.jpa.entities.ProductList | ProductListJpaEntity + ProductList (domain) | src/main/java/org/trebol/productlist/adapter/out/persistence/jpa and src/main/java/org/trebol/productlist/domain/model | Split persistence/domain |
| NOT_STARTED | org.trebol.jpa.services.conversion.ProductListItemsConverterService | ProductListItem mappers | src/main/java/org/trebol/productlistitem/adapter/in/web/mapper and src/main/java/org/trebol/productlistitem/adapter/out/persistence/mapper | Separate mapping concerns |
| NOT_STARTED | org.trebol.jpa.services.conversion.impl.ProductListItemsConverterServiceImpl | ProductListItem mapper implementations | src/main/java/org/trebol/productlistitem/adapter/in/web/mapper and src/main/java/org/trebol/productlistitem/adapter/out/persistence/mapper | Move converter logic |
| NOT_STARTED | org.trebol.jpa.services.predicates.ProductListItemsPredicateService | ProductListItemQueryService/translator | src/main/java/org/trebol/productlistitem/application/service or src/main/java/org/trebol/productlistitem/adapter/out/persistence/query | Query abstraction |
| NOT_STARTED | org.trebol.jpa.services.predicates.impl.ProductListItemsPredicateServiceImpl | ProductListItemQuerySpecification | src/main/java/org/trebol/productlistitem/adapter/out/persistence/query | Adapter-owned query logic |
| NOT_STARTED | org.trebol.jpa.sortspecs.ProductListItemsSortSpec | ProductListItemSortTranslator | src/main/java/org/trebol/productlistitem/adapter/out/persistence/query | Adapter-owned sorting |
| NOT_STARTED | org.trebol.jpa.repositories.ProductListItemsRepository | ProductListItemJpaRepository + ProductListItemRepositoryAdapter | src/main/java/org/trebol/productlistitem/adapter/out/persistence/jpa | Port/adapter split |
| NOT_STARTED | org.trebol.jpa.entities.ProductListItem | ProductListItemJpaEntity + ProductListItem (domain) | src/main/java/org/trebol/productlistitem/adapter/out/persistence/jpa and src/main/java/org/trebol/productlistitem/domain/model | Split persistence/domain |
| NOT_STARTED | org.trebol.jpa.entities.ProductImage | ProductImageJpaEntity + ProductImage (domain if needed) | src/main/java/org/trebol/productimage/adapter/out/persistence/jpa and src/main/java/org/trebol/productimage/domain/model | Keep image persistence isolated |
| NOT_STARTED | org.trebol.jpa.repositories.ProductImagesRepository | ProductImageJpaRepository + ProductImageRepositoryAdapter | src/main/java/org/trebol/productimage/adapter/out/persistence/jpa | Port/adapter split |

## Execution Order

1. Core Product vertical slice: CreateProduct (already partially done)
2. Core Product full CRUD use cases and DataProductsController migration
3. Product Category migration
4. Product List migration
5. Product List Item migration
6. Product Image persistence adapterization

## Exit Criteria

- No class under org.trebol.jpa.services.* is used by Product endpoints
- Controllers depend on application use cases only
- Domain models are framework-free
- QueryDSL/JPA code exists only under adapter/out/persistence/*
