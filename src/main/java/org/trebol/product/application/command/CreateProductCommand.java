package org.trebol.product.application.command;

public record CreateProductCommand(
    String code,
    String name,
    Double price,
    Boolean isActive
) {
}
