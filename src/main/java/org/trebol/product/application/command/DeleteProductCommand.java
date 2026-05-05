package org.trebol.product.application.command;

public record DeleteProductCommand(Long id) {
    public DeleteProductCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID must be positive");
        }
    }
}
