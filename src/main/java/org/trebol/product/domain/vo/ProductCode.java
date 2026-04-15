package org.trebol.product.domain.vo;

import org.trebol.product.domain.exception.ProductValidationException;

import java.util.Objects;

public record ProductCode(String value) {
    public ProductCode {
        Objects.requireNonNull(value, "Product code cannot be null");
        if (value.isBlank()) {
            throw new ProductValidationException("Product code cannot be blank");
        }
    }
}
