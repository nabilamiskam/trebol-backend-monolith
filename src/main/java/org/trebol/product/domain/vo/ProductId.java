package org.trebol.product.domain.vo;

import java.util.Objects;

public record ProductId(Long value) {
    public ProductId {
        Objects.requireNonNull(value, "Product ID cannot be null");
        if (value <= 0) {
            throw new IllegalArgumentException("Product ID must be positive");
        }
    }
}
