package org.trebol.product.application.usecase;

import org.trebol.product.application.command.UpdateProductCommand;
import org.trebol.product.application.result.ProductResult;

public interface UpdateProductUseCase {
    ProductResult execute(UpdateProductCommand command);
}
