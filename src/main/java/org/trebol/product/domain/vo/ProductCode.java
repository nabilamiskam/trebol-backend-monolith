package org.trebol.product.domain.vo;

import java.util.Objects;

public record ProductCode(String value) {
    public ProductCode {
        Objects.requireNonNull(value, "Product code cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Product code cannot be blank");
        }
    }
}
