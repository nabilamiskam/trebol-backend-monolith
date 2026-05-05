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
import org.trebol.product.domain.exception.ProductCodeAlreadyExistsException;
import org.trebol.product.domain.exception.ProductNotFoundException;
import org.trebol.product.domain.port.ProductRepository;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;
import org.trebol.product.domain.vo.ProductName;
import org.trebol.product.domain.vo.ProductPrice;
import org.trebol.product.domain.vo.ProductStatus;

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
        // 1. Check if code already exists
        Optional<ProductAggregate> existingByCode = 
            productRepository.findByCode(new ProductCode(command.code()));
        if (existingByCode.isPresent()) {
            throw new ProductCodeAlreadyExistsException("Product code already exists: " + command.code());
        }

        // 2. Create aggregate with value objects
        ProductAggregate product = new ProductAggregate(
            null, // ID assigned by DB
            new ProductCode(command.code()),
            new ProductName(command.name()),
            new ProductPrice(command.price())
        );

        // 3. Save via port
        ProductAggregate saved = productRepository.save(product);

        // 4. Return result
        return mapper.toResult(saved);
    }

    @Override
    public ProductResult execute(UpdateProductCommand command) {
        // 1. Load existing aggregate
        ProductAggregate product = productRepository.findById(new ProductId(command.id()))
            .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + command.id()));

        // 2. Apply mutations (if field provided)
        if (command.name() != null) {
            product.updateName(new ProductName(command.name()));
        }
        if (command.price() != null) {
            product.updatePrice(new ProductPrice(command.price()));
        }
        product.updateStatus(ProductStatus.fromBoolean(command.isActive()));

        // 3. Save via port
        ProductAggregate updated = productRepository.save(product);

        // 4. Return result
        return mapper.toResult(updated);
    }

    @Override
    public void execute(DeleteProductCommand command) {
        // 1. Verify product exists
        productRepository.findById(new ProductId(command.id()))
            .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + command.id()));

        // 2. Delete
        productRepository.deleteById(new ProductId(command.id()));
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
