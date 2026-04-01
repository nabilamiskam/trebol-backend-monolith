package org.trebol.product.application.service;

import org.trebol.product.application.command.CreateProductCommand;
import org.trebol.product.application.command.DeleteProductCommand;
import org.trebol.product.application.command.UpdateProductCommand;
import org.trebol.product.application.query.GetProductQuery;
import org.trebol.product.application.query.ListProductsQuery;
import org.trebol.product.application.result.PagedProductResult;
import org.trebol.product.application.result.ProductResult;
import org.trebol.product.application.usecase.CreateProductUseCase;
import org.trebol.product.application.usecase.DeleteProductUseCase;
import org.trebol.product.application.usecase.GetProductUseCase;
import org.trebol.product.application.usecase.ListProductsUseCase;
import org.trebol.product.application.usecase.UpdateProductUseCase;

public class ProductApplicationService implements
    CreateProductUseCase,
    UpdateProductUseCase,
    DeleteProductUseCase,
    GetProductUseCase,
    ListProductsUseCase {

    @Override
    public ProductResult execute(CreateProductCommand command) {
        throw new UnsupportedOperationException("Scaffold only");
    }

    @Override
    public ProductResult execute(UpdateProductCommand command) {
        throw new UnsupportedOperationException("Scaffold only");
    }

    @Override
    public void execute(DeleteProductCommand command) {
        throw new UnsupportedOperationException("Scaffold only");
    }

    @Override
    public ProductResult execute(GetProductQuery query) {
        throw new UnsupportedOperationException("Scaffold only");
    }

    @Override
    public PagedProductResult execute(ListProductsQuery query) {
        throw new UnsupportedOperationException("Scaffold only");
    }
}
