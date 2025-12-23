# Feature Trace: <Feature Data Products Controller Test>

## API entry
- Controller class: DataProductsController
- Endpoint (HTTP method + path):
	- GET /data/products
	- POST /data/products
	- PUT /data/products
	- PATCH /data/products
	- DELETE /data/products

- Controller method: readMany(), create(), update(), partialUpdate(), delete()

## Application flow
- API service interface: org.trebol.api.services.PaginationService
- API service implementation: org.trebol.api.services.impl.PaginationServiceImpl
- Main service method(s) used: determineRequestedPageIndex(), determineRequestedPageSize()
 - Sort parsing interface: org.trebol.jpa.services.SortSpecParserService
 - Sort parsing method used: parse(orderSpecMap, queryMap)
 - Note: No dedicated products-specific API service under org.trebol.api.services; the controller directly orchestrates JPA services for business/persistence logic (plus PaginationService).
 - Injected domain/persistence services used by controller:
	 - org.trebol.jpa.services.crud.ProductsCrudService
	 - org.trebol.jpa.services.predicates.ProductsPredicateService
	 - org.trebol.jpa.services.SortSpecParserService

## Persistence flow
- JPA service(s) used (crud/patch/conversion/predicate/sort):
	- Crud: org.trebol.jpa.services.crud.ProductsCrudService (impl: org.trebol.jpa.services.crud.impl.ProductsCrudServiceImpl)
	- Patch: org.trebol.jpa.services.patch.ProductsPatchService (impl: org.trebol.jpa.services.patch.impl.ProductsPatchServiceImpl)
	- Conversion: org.trebol.jpa.services.conversion.ProductsConverterService (impl: org.trebol.jpa.services.conversion.impl.ProductsConverterServiceImpl)
	- Predicate: org.trebol.jpa.services.predicates.ProductsPredicateService (impl: org.trebol.jpa.services.predicates.impl.ProductsPredicateServiceImpl)
	- Sort: org.trebol.jpa.sortspecs.ProductsSortSpec.ORDER_SPEC_MAP
- Repository interface:
	- org.trebol.jpa.repositories.ProductsRepository
	- org.trebol.jpa.repositories.ProductImagesRepository
	- org.trebol.jpa.repositories.ProductsCategoriesRepository
- Entity class(es):
	- org.trebol.jpa.entities.Product
	- org.trebol.jpa.entities.ProductImage
	- org.trebol.jpa.entities.ProductCategory
- Tables (if known): Not explicitly shown; implied by entities/repositories

## Inputs/Outputs
- Request DTO/model:
	- ProductPojo (create/update body)
	- Map<String,Object> (partialUpdate body)
	- Map<String,String> (query params for read/delete)
- Response DTO/model:
	- DataPagePojo<ProductPojo> (readMany)
	- void / HTTP status (create: 201, update/patch/delete: 204)

## Business rules observed
- Validations:
	- Update/partialUpdate require non-empty request params; else BadInputException
	- Product barcode must be non-blank for getExisting; else BadInputException
	- Pagination enforces numeric pageIndex/pageSize; caps pageSize to maxAllowedPageSize
- State transitions:
	- Create: convert pojo → entity, persist, attach images, return pojo with images
	- Update: convert pojo → entity (with id), persist, delete and re-link product images, return updated pojo
	- Partial update: patch fields (barcode, name, price, description, currentStock, criticalStock)
- Error cases:
	- EntityNotFoundException when no element matches filters (update/partialUpdate/readOne)
	- BadInputException for invalid inputs (e.g., missing request params, invalid barcode)
	- NumberFormat issues in predicate/pagination logged; pagination may throw NumberFormatException
 - Authorization:
	 - create requires authority 'products:create'
	 - update/partialUpdate require authority 'products:update'
	 - delete requires authority 'products:delete'

	## Layered View (Controller → Services → Helpers → Repos)

	- Layer 1: Controller → Services
		- Controller: DataProductsController
		- Endpoints: GET/POST/PUT/PATCH/DELETE /data/products
		- Injected services: PaginationService, SortSpecParserService, ProductsCrudService, ProductsPredicateService

	- Layer 2: Application Services (as implemented in JPA services)
		- Interfaces → Implementations:
			- ProductsCrudService → ProductsCrudServiceImpl
			- ProductsPredicateService → ProductsPredicateServiceImpl
			- PaginationService → PaginationServiceImpl
			- SortSpecParserService (uses ProductsSortSpec.ORDER_SPEC_MAP)
		- Main controller-driven methods:
			- readMany(params), create(ProductPojo), update(ProductPojo, params), partialUpdate(changes, params), delete(params)

	- Layer 3: JPA Data Helpers
		- ProductsConverterService → ProductsConverterServiceImpl (DTO ↔ Entity)
		- ProductsPatchService → ProductsPatchServiceImpl (partial updates)
		- ImagesCrudService, ImagesConverterService (image handling)
		- ProductCategoriesConverterService, ProductCategoryTreeResolverService (category handling/predicates)
		- ProductsSortSpec.ORDER_SPEC_MAP (sorting)

	- Layer 4: Repositories
		- ProductsRepository (products)
		- ProductImagesRepository (product-image relations)
		- ProductsCategoriesRepository (categories)

## Notes / Questions
- ...
 - Confirm whether GET /data/products is publicly accessible or governed by global security config (no @PreAuthorize on readMany).
 - Verify full sort fields supported by ProductsSortSpec.ORDER_SPEC_MAP.