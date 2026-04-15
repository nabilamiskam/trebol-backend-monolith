package org.trebol.product.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.trebol.product.domain.aggregate.ProductAggregate;
import org.trebol.product.domain.exception.ProductCodeAlreadyExistsException;
import org.trebol.product.domain.port.ProductRepository;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;
import org.trebol.product.domain.vo.ProductName;
import org.trebol.product.domain.vo.ProductPrice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductDomainServiceTest {
    private InMemoryProductRepository repository;
    private ProductDomainService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
        service = new ProductDomainService(repository);
    }

    @Test
    void shouldAllowAvailableCode() {
        assertDoesNotThrow(() -> service.ensureCodeAvailable(new ProductCode("P001")));
    }

    @Test
    void shouldRejectDuplicateCode() {
        repository.saveExistingCode("P001");

        assertThrows(ProductCodeAlreadyExistsException.class, () -> service.ensureCodeAvailable(new ProductCode("P001")));
    }

    private static final class InMemoryProductRepository implements ProductRepository {
        private String existingCode;

        void saveExistingCode(String code) {
            this.existingCode = code;
        }

        @Override
        public ProductAggregate save(ProductAggregate aggregate) {
            return aggregate;
        }

        @Override
        public Optional<ProductAggregate> findById(ProductId id) {
            return Optional.empty();
        }

        @Override
        public Optional<ProductAggregate> findByCode(ProductCode code) {
            if (existingCode != null && existingCode.equals(code.value())) {
                return Optional.of(ProductAggregate.create(
                    new ProductId(1L),
                    new ProductCode(existingCode),
                    new ProductName("Existing Product"),
                    new ProductPrice(BigDecimal.ONE)
                ));
            }
            return Optional.empty();
        }

        @Override
        public List<ProductAggregate> findAll(int pageIndex, int pageSize) {
            return List.of();
        }

        @Override
        public long countAll() {
            return 0;
        }

        @Override
        public void deleteById(ProductId id) {
        }
    }
}
