package org.trebol.product.adapter.inbound.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.trebol.product.adapter.inbound.dto.PagedProductResponse;
import org.trebol.product.adapter.inbound.dto.BulkPatchProductResponse;
import org.trebol.product.adapter.inbound.dto.ErrorResponse;
import org.trebol.product.adapter.inbound.dto.ProductRequest;
import org.trebol.product.adapter.inbound.dto.ProductResponse;
import org.trebol.product.application.command.CreateProductCommand;
import org.trebol.product.application.command.BulkPatchProductCommand;
import org.trebol.product.application.command.DeleteProductCommand;
import org.trebol.product.application.command.UpdateProductCommand;
import org.trebol.product.application.query.GetProductQuery;
import org.trebol.product.application.query.ListProductsQuery;
import org.trebol.product.application.result.BulkPatchProductResult;
import org.trebol.product.application.result.PagedProductResult;
import org.trebol.product.application.result.ProductResult;
import org.trebol.product.application.usecase.CreateProductUseCase;
import org.trebol.product.application.usecase.DeleteProductUseCase;
import org.trebol.product.application.usecase.GetProductUseCase;
import org.trebol.product.application.usecase.ListProductsUseCase;
import org.trebol.product.application.usecase.PatchProductsUseCase;
import org.trebol.product.application.usecase.UpdateProductUseCase;
import org.trebol.product.domain.exception.ProductCodeAlreadyExistsException;
import org.trebol.product.domain.exception.ProductNotFoundException;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/product-module")
public class ProductController {

	private final GetProductUseCase getProductUseCase;
	private final ListProductsUseCase listProductsUseCase;
	private final CreateProductUseCase createProductUseCase;
	private final UpdateProductUseCase updateProductUseCase;
	private final DeleteProductUseCase deleteProductUseCase;
	private final PatchProductsUseCase patchProductsUseCase;
	private final ProductWebMapper productWebMapper;

	public ProductController(
		GetProductUseCase getProductUseCase,
		ListProductsUseCase listProductsUseCase,
		CreateProductUseCase createProductUseCase,
		UpdateProductUseCase updateProductUseCase,
		DeleteProductUseCase deleteProductUseCase,
		PatchProductsUseCase patchProductsUseCase,
		ProductWebMapper productWebMapper
	) {
		this.getProductUseCase = getProductUseCase;
		this.listProductsUseCase = listProductsUseCase;
		this.createProductUseCase = createProductUseCase;
		this.updateProductUseCase = updateProductUseCase;
		this.deleteProductUseCase = deleteProductUseCase;
		this.patchProductsUseCase = patchProductsUseCase;
		this.productWebMapper = productWebMapper;
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
		ProductResult result = getProductUseCase.execute(new GetProductQuery(id));
		if (result == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(productWebMapper.toResponse(result));
	}

	@GetMapping
	public ResponseEntity<PagedProductResponse> getProducts(
		@RequestParam(defaultValue = "0") int pageIndex,
		@RequestParam(defaultValue = "10") int pageSize,
		@RequestParam Map<String, String> requestParams
	) {
		PagedProductResult result = listProductsUseCase.execute(
			new ListProductsQuery(pageIndex, pageSize, requestParams)
		);

		return ResponseEntity.ok(productWebMapper.toPagedResponse(result));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('products:create')")
	public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest request) {
		CreateProductCommand command = productWebMapper.toCreateCommand(request);
		ProductResult result = createProductUseCase.execute(command);
		ProductResponse response = productWebMapper.toResponse(result);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(result.id())
			.toUri();

		return ResponseEntity.created(location).body(response);
	}

	@PatchMapping
	@PreAuthorize("hasAuthority('products:update')")
	public ResponseEntity<BulkPatchProductResponse> patchProducts(
		@RequestParam Map<String, String> requestParams,
		@RequestBody Map<String, Object> changes
	) {
		BulkPatchProductCommand command = new BulkPatchProductCommand(requestParams, changes);
		BulkPatchProductResult result = patchProductsUseCase.execute(command);
		return ResponseEntity.ok(productWebMapper.toBulkPatchResponse(result));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('products:update')")
	public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
		UpdateProductCommand command = productWebMapper.toUpdateCommand(id, request);
		ProductResult result = updateProductUseCase.execute(command);
		ProductResponse response = productWebMapper.toResponse(result);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('products:delete')")
	public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
		DeleteProductCommand command = productWebMapper.toDeleteCommand(id);
		deleteProductUseCase.execute(command);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(new ErrorResponse("Product not found: " + e.getMessage()));
	}

	@ExceptionHandler(ProductCodeAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleCodeExists(ProductCodeAlreadyExistsException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(new ErrorResponse("Code already exists: " + e.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleValidation(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse("Validation error: " + e.getMessage()));
	}
}
