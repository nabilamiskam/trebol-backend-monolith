package org.trebol.product.application.command;

import java.math.BigDecimal;

public record CreateProductCommand(
    String code,
    String name,
    BigDecimal price,
    boolean isActive,
    int currentStock,
    int criticalStock
) {
    public CreateProductCommand {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        if (currentStock < 0) {
            throw new IllegalArgumentException("currentStock must be non-negative");
        }
        if (criticalStock < 0) {
            throw new IllegalArgumentException("criticalStock must be non-negative");
        }
    }
}
