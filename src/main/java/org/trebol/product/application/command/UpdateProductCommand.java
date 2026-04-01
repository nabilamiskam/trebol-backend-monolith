package org.trebol.product.application.command;

public record UpdateProductCommand(
    Long id,
    String name,
    Double price,
    Boolean isActive
) {
}
