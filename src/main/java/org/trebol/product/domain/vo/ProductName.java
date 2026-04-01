package org.trebol.product.domain.vo;

import java.util.Objects;

public record ProductName(String value) {
    public ProductName {
        Objects.requireNonNull(value, "Product name cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank");
        }
        if (value.length() > 255) {
            throw new IllegalArgumentException("Product name cannot exceed 255 chars");
        }
    }
}
