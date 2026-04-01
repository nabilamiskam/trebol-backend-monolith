package org.trebol.product.application.usecase;

import org.trebol.product.application.command.DeleteProductCommand;

public interface DeleteProductUseCase {
    void execute(DeleteProductCommand command);
}
