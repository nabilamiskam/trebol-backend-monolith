package org.trebol.product.application.command;

import java.math.BigDecimal;

public record UpdateProductCommand(
    Long id,
    String name,
    BigDecimal price,
    boolean isActive,
    Integer currentStock,
    Integer criticalStock
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
        if (currentStock != null && currentStock < 0) {
            throw new IllegalArgumentException("currentStock must be non-negative");
        }
        if (criticalStock != null && criticalStock < 0) {
            throw new IllegalArgumentException("criticalStock must be non-negative");
        }
    }
}
