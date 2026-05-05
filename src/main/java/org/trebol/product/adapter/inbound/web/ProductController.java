package org.trebol.product.adapter.inbound.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trebol.product.adapter.inbound.dto.PagedProductResponse;
import org.trebol.product.adapter.inbound.dto.ProductResponse;
import org.trebol.product.application.query.GetProductQuery;
import org.trebol.product.application.query.ListProductsQuery;
import org.trebol.product.application.result.PagedProductResult;
import org.trebol.product.application.result.ProductResult;
import org.trebol.product.application.service.ProductApplicationService;

import java.util.Map;

@RestController
@RequestMapping("/product-module")
public class ProductController {

	private final ProductApplicationService productApplicationService;
	private final ProductWebMapper productWebMapper;

	public ProductController(ProductApplicationService productApplicationService, ProductWebMapper productWebMapper) {
		this.productApplicationService = productApplicationService;
		this.productWebMapper = productWebMapper;
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
		ProductResult result = productApplicationService.execute(new GetProductQuery(id));
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
		PagedProductResult result = productApplicationService.execute(
			new ListProductsQuery(pageIndex, pageSize, requestParams)
		);

		return ResponseEntity.ok(productWebMapper.toPagedResponse(result));
	}
}
