package org.trebol.product.application.service;

import org.junit.jupiter.api.Test;
import org.trebol.product.application.command.CreateProductCommand;
import org.trebol.product.application.command.DeleteProductCommand;
import org.trebol.product.application.command.UpdateProductCommand;
import org.trebol.product.application.query.GetProductQuery;
import org.trebol.product.application.result.ProductResult;
import org.trebol.product.domain.aggregate.ProductAggregate;
import org.trebol.product.domain.exception.ProductCodeAlreadyExistsException;
import org.trebol.product.domain.exception.ProductNotFoundException;
import org.trebol.product.domain.port.ProductRepository;
import org.trebol.product.domain.service.ProductDomainService;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;
import org.trebol.product.domain.vo.ProductName;
import org.trebol.product.domain.vo.ProductPrice;
import org.trebol.product.domain.vo.ProductStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductApplicationServiceTest {
    @Test
    void createShouldPersistNewAggregate() {
        InMemoryProductRepository repository = new InMemoryProductRepository();
        ProductApplicationService service = new ProductApplicationService(repository, new ProductDomainService(repository), new ProductApplicationMapper());

        ProductResult result = service.execute(new CreateProductCommand("P001", "Product 1", 100.0, true));

        assertEquals(1L, result.id());
        assertEquals("P001", result.code());
        assertEquals("Product 1", result.name());
        assertEquals(100.0, result.price());
        assertEquals(true, result.isActive());
    }

    @Test
    void createShouldRejectDuplicateCode() {
        InMemoryProductRepository repository = new InMemoryProductRepository();
        repository.seed(ProductAggregate.create(
            new ProductId(1L),
            new ProductCode("P001"),
            new ProductName("Seed"),
            new ProductPrice(BigDecimal.valueOf(10))
        ));
        ProductApplicationService service = new ProductApplicationService(repository, new ProductDomainService(repository), new ProductApplicationMapper());

        assertThrows(ProductCodeAlreadyExistsException.class, () -> service.execute(new CreateProductCommand("P001", "Product 1", 100.0, true)));
    }

    @Test
    void updateShouldModifyAggregate() {
        InMemoryProductRepository repository = new InMemoryProductRepository();
        repository.seed(ProductAggregate.create(
            new ProductId(1L),
            new ProductCode("P001"),
            new ProductName("Product 1"),
            new ProductPrice(BigDecimal.valueOf(100))
        ));
        ProductApplicationService service = new ProductApplicationService(repository, new ProductDomainService(repository), new ProductApplicationMapper());

        ProductResult result = service.execute(new UpdateProductCommand(1L, "Updated", 250.0, false));

        assertEquals("Updated", result.name());
        assertEquals(250.0, result.price());
        assertEquals(false, result.isActive());
    }

    @Test
    void getShouldThrowWhenMissing() {
        InMemoryProductRepository repository = new InMemoryProductRepository();
        ProductApplicationService service = new ProductApplicationService(repository, new ProductDomainService(repository), new ProductApplicationMapper());

        assertThrows(ProductNotFoundException.class, () -> service.execute(new GetProductQuery(99L)));
    }

    @Test
    void deleteShouldRemoveAggregate() {
        InMemoryProductRepository repository = new InMemoryProductRepository();
        repository.seed(ProductAggregate.create(
            new ProductId(1L),
            new ProductCode("P001"),
            new ProductName("Product 1"),
            new ProductPrice(BigDecimal.valueOf(100))
        ));
        ProductApplicationService service = new ProductApplicationService(repository, new ProductDomainService(repository), new ProductApplicationMapper());

        service.execute(new DeleteProductCommand(1L));

        assertEquals(0, repository.currentSize());
    }

    private static final class InMemoryProductRepository implements ProductRepository {
        private final List<ProductAggregate> aggregates = new ArrayList<>();

        void seed(ProductAggregate aggregate) {
            aggregates.add(aggregate);
        }

        int currentSize() {
            return aggregates.size();
        }

        @Override
        public ProductAggregate save(ProductAggregate aggregate) {
            deleteById(aggregate.getId());
            aggregates.add(aggregate);
            return aggregate;
        }

        @Override
        public Optional<ProductAggregate> findById(ProductId id) {
            return aggregates.stream()
                .filter(aggregate -> aggregate.getId().value().equals(id.value()))
                .findFirst();
        }

        @Override
        public Optional<ProductAggregate> findByCode(ProductCode code) {
            return aggregates.stream()
                .filter(aggregate -> aggregate.getCode().value().equals(code.value()))
                .findFirst();
        }

        @Override
        public List<ProductAggregate> findAll(int pageIndex, int pageSize) {
            return List.copyOf(aggregates);
        }

        @Override
        public long countAll() {
            return aggregates.size();
        }

        @Override
        public void deleteById(ProductId id) {
            aggregates.removeIf(aggregate -> aggregate.getId().value().equals(id.value()));
        }
    }
}
