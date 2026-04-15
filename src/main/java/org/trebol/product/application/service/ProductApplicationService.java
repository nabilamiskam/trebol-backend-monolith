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
import org.trebol.product.domain.exception.ProductNotFoundException;
import org.trebol.product.domain.service.ProductDomainService;
import org.trebol.product.domain.port.ProductRepository;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;
import org.trebol.product.domain.vo.ProductName;
import org.trebol.product.domain.vo.ProductPrice;
import org.trebol.product.domain.vo.ProductStatus;

import java.math.BigDecimal;
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
    private final ProductDomainService domainService;
    private final ProductApplicationMapper mapper;

    public ProductApplicationService(
        ProductRepository productRepository,
        ProductDomainService domainService,
        ProductApplicationMapper mapper
    ) {
        this.productRepository = productRepository;
        this.domainService = domainService;
        this.mapper = mapper;
    }

    @Override
    public ProductResult execute(CreateProductCommand command) {
        ProductCode code = new ProductCode(command.code());
        ProductName name = new ProductName(command.name());
        ProductPrice price = new ProductPrice(BigDecimal.valueOf(command.price()));

        domainService.ensureCodeAvailable(code);

        long nextId = productRepository.countAll() + 1L;
        ProductAggregate aggregate = ProductAggregate.create(
            new ProductId(nextId),
            code,
            name,
            price
        );

        if (Boolean.FALSE.equals(command.isActive())) {
            aggregate.deactivate();
        }

        ProductAggregate saved = productRepository.save(aggregate);
        return mapper.toResult(saved);
    }

    @Override
    public ProductResult execute(UpdateProductCommand command) {
        ProductId id = new ProductId(command.id());
        ProductAggregate aggregate = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + command.id()));

        aggregate.rename(new ProductName(command.name()));
        aggregate.reprice(new ProductPrice(BigDecimal.valueOf(command.price())));

        if (Boolean.TRUE.equals(command.isActive())) {
            aggregate.activate();
        } else if (Boolean.FALSE.equals(command.isActive())) {
            aggregate.deactivate();
        }

        ProductAggregate saved = productRepository.save(aggregate);
        return mapper.toResult(saved);
    }

    @Override
    public void execute(DeleteProductCommand command) {
        ProductId id = new ProductId(command.id());
        productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + command.id()));
        productRepository.deleteById(id);
    }

    @Override
    public ProductResult execute(GetProductQuery query) {
        ProductId id = new ProductId(query.id());
        Optional<ProductAggregate> aggregate = productRepository.findById(id);
        return aggregate.map(mapper::toResult)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + query.id()));
    }

    @Override
    public PagedProductResult execute(ListProductsQuery query) {
        List<ProductAggregate> aggregates = productRepository.findAll(query.pageIndex(), query.pageSize());
        long totalCount = productRepository.countAll();
        
        List<ProductResult> results = aggregates.stream()
            .map(mapper::toResult)
            .toList();
        
        return new PagedProductResult(results, totalCount);
    }
}
