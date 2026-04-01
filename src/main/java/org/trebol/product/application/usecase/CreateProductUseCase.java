package org.trebol.product.application.usecase;

import org.trebol.product.application.command.CreateProductCommand;
import org.trebol.product.application.result.ProductResult;

public interface CreateProductUseCase {
    ProductResult execute(CreateProductCommand command);
}
