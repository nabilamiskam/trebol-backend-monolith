package org.trebol.product.domain.vo;

import java.math.BigDecimal;
import java.util.Objects;

public record ProductPrice(BigDecimal value) {
    public ProductPrice {
        Objects.requireNonNull(value, "Product price cannot be null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Product price cannot be negative");
        }
    }
}
