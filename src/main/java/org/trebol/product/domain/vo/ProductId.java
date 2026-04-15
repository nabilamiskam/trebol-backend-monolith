package org.trebol.product.domain.vo;

import org.trebol.product.domain.exception.ProductValidationException;

import java.util.Objects;

public record ProductId(Long value) {
    public ProductId {
        Objects.requireNonNull(value, "Product ID cannot be null");
        if (value <= 0) {
            throw new ProductValidationException("Product ID must be positive");
        }
    }
}
