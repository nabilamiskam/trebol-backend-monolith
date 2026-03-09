package org.trebol.adapter.web.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trebol.adapter.web.product.dto.CreateProductRequest;
import org.trebol.adapter.web.product.dto.CreateProductResponse;
import org.trebol.application.product.usecase.CreateProductUseCase;
import org.trebol.domain.product.model.Product;

import jakarta.validation.Valid;

/**
 * Clean Architecture controller for product operations.
 * This controller depends on use cases (application layer), not JPA services.
 */
@RestController
@RequestMapping("/clean/products")
@Tag(name = "Clean Architecture - Products")
public class ProductCleanController {
    
    private final CreateProductUseCase createProductUseCase;
    
    @Autowired
    public ProductCleanController(CreateProductUseCase createProductUseCase) {
        this.createProductUseCase = createProductUseCase;
    }
    
    @PostMapping
    @Operation(summary = "Create product (clean architecture slice)")
    public ResponseEntity<CreateProductResponse> createProduct(
        @Valid @RequestBody CreateProductRequest request
    ) {
        try {
            Product product = createProductUseCase.execute(request.name(), request.price());
            return ResponseEntity.ok(CreateProductResponse.from(product));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
