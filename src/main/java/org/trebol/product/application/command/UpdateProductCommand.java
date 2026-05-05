package org.trebol.product.application.command;

import java.math.BigDecimal;

public record UpdateProductCommand(
    Long id,
    String name,
    BigDecimal price,
    boolean isActive
) {
    public UpdateProductCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID must be positive");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
    }
}
