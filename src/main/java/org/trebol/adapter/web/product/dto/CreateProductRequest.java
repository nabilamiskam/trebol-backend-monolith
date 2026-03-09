package org.trebol.adapter.web.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for creating a product (clean architecture endpoint).
 */
public record CreateProductRequest(
    @NotBlank(message = "name is required")
    String name,
    
    @Positive(message = "price must be positive")
    int price
) {
}
