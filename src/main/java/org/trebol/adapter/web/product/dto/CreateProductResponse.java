package org.trebol.adapter.web.product.dto;

import org.trebol.domain.product.model.Product;

/**
 * Response DTO for product (clean architecture endpoint).
 */
public record CreateProductResponse(
    Long id,
    String name,
    int price
) {
    public static CreateProductResponse from(Product product) {
        return new CreateProductResponse(
            product.id(),
            product.name(),
            product.price().amount()
        );
    }
}
