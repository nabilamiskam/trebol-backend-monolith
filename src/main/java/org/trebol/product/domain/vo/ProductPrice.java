package org.trebol.product.domain.vo;

import org.trebol.product.domain.exception.ProductValidationException;

import java.math.BigDecimal;
import java.util.Objects;

public record ProductPrice(BigDecimal value) {
    public ProductPrice {
        Objects.requireNonNull(value, "Product price cannot be null");
        if (value.signum() < 0) {
            throw new ProductValidationException("Product price cannot be negative");
        }
    }
}
