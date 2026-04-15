package org.trebol.product.application.service;

import org.springframework.stereotype.Service;
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
import org.trebol.product.domain.aggregate.ProductAggregate;
import org.trebol.product.domain.port.ProductRepository;
import org.trebol.product.domain.vo.ProductId;

import java.util.List;
import java.util.Optional;

@Service
public class ProductApplicationService implements
    CreateProductUseCase,
    UpdateProductUseCase,
    DeleteProductUseCase,
    GetProductUseCase,
    ListProductsUseCase {

    private final ProductRepository productRepository;
    private final ProductApplicationMapper mapper;

    public ProductApplicationService(ProductRepository productRepository, ProductApplicationMapper mapper) {
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    @Override
    public ProductResult execute(CreateProductCommand command) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public ProductResult execute(UpdateProductCommand command) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void execute(DeleteProductCommand command) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public ProductResult execute(GetProductQuery query) {
        ProductId id = new ProductId(query.id());
        Optional<ProductAggregate> aggregate = productRepository.findById(id);
        return aggregate.map(mapper::toResult).orElse(null);
    }

    @Override
    public PagedProductResult execute(ListProductsQuery query) {
        List<ProductAggregate> aggregates = productRepository.findAll(
            query.pageIndex(),
            query.pageSize(),
            query.requestParams()
        );
        long totalCount = productRepository.countAll(query.requestParams());
        
        List<ProductResult> results = aggregates.stream()
            .map(mapper::toResult)
            .toList();
        
        return new PagedProductResult(results, totalCount);
    }
}
