package org.trebol.product.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trebol.product.application.command.CreateProductCommand;
import org.trebol.product.application.command.DeleteProductCommand;
import org.trebol.product.application.command.UpdateProductCommand;
import org.trebol.product.application.query.GetProductQuery;
import org.trebol.product.application.query.ListProductsQuery;
import org.trebol.product.application.result.PagedProductResult;
import org.trebol.product.application.result.ProductResult;
import org.trebol.product.domain.aggregate.ProductAggregate;
import org.trebol.product.domain.exception.ProductCodeAlreadyExistsException;
import org.trebol.product.domain.exception.ProductNotFoundException;
import org.trebol.product.domain.port.ProductRepository;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;
import org.trebol.product.domain.vo.ProductName;
import org.trebol.product.domain.vo.ProductPrice;
import org.trebol.product.domain.vo.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductApplicationMapper mapper;

    private ProductApplicationService service;

    @BeforeEach
    void setUp() {
        mapper = new ProductApplicationMapper();
        service = new ProductApplicationService(productRepository, mapper);
    }

    @Test
    void shouldGetProductById() {
        ProductAggregate aggregate = createProductAggregate(1L, "LAP001", "Laptop", "999.99", ProductStatus.ACTIVE);
        when(productRepository.findById(new ProductId(1L))).thenReturn(Optional.of(aggregate));

        ProductResult result = service.execute(new GetProductQuery(1L));

        assertEquals(new ProductResult(1L, "LAP001", "Laptop", 999.99, true, 0, 0), result);
        verify(productRepository).findById(new ProductId(1L));
    }

    @Test
    void shouldReturnNullWhenProductNotFoundById() {
        when(productRepository.findById(new ProductId(999L))).thenReturn(Optional.empty());

        ProductResult result = service.execute(new GetProductQuery(999L));

        assertEquals(null, result);
        verify(productRepository).findById(new ProductId(999L));
    }

    @Test
    void shouldListProductsWithPagination() {
        ProductAggregate first = createProductAggregate(1L, "LAP001", "Laptop", "999.99", ProductStatus.ACTIVE);
        ProductAggregate second = createProductAggregate(2L, "LAP002", "Desktop", "1499.50", ProductStatus.INACTIVE);
        Map<String, String> filters = Map.of("name", "Lap");

        when(productRepository.findAll(0, 10, filters)).thenReturn(List.of(first, second));
        when(productRepository.countAll(filters)).thenReturn(2L);

        PagedProductResult result = service.execute(new ListProductsQuery(0, 10, filters));

        assertEquals(2L, result.totalCount());
        assertEquals(2, result.items().size());
        assertEquals(new ProductResult(1L, "LAP001", "Laptop", 999.99, true, 0, 0), result.items().get(0));
        assertEquals(new ProductResult(2L, "LAP002", "Desktop", 1499.50, false, 0, 0), result.items().get(1));
        verify(productRepository).findAll(0, 10, filters);
        verify(productRepository).countAll(filters);
    }

    @Test
    void shouldCreateProduct() {
        CreateProductCommand command = new CreateProductCommand("LAP003", "Gaming Laptop", new BigDecimal("2499.99"), true, 0, 0);
        ProductAggregate saved = createProductAggregate(3L, "LAP003", "Gaming Laptop", "2499.99", ProductStatus.ACTIVE);

        when(productRepository.findByCode(new ProductCode("LAP003"))).thenReturn(Optional.empty());
        when(productRepository.save(any(ProductAggregate.class))).thenReturn(saved);

        ProductResult result = service.execute(command);

        assertEquals(new ProductResult(3L, "LAP003", "Gaming Laptop", 2499.99, true, 0, 0), result);
        verify(productRepository).findByCode(new ProductCode("LAP003"));
        verify(productRepository).save(any(ProductAggregate.class));
    }

    @Test
    void shouldThrowWhenCodeAlreadyExistsDuringCreate() {
        CreateProductCommand command = new CreateProductCommand("LAP001", "Laptop", new BigDecimal("999.99"), true, 0, 0);
        when(productRepository.findByCode(new ProductCode("LAP001"))).thenReturn(Optional.of(createProductAggregate(1L, "LAP001", "Laptop", "999.99", ProductStatus.ACTIVE)));

        assertThrows(ProductCodeAlreadyExistsException.class, () -> service.execute(command));
        verify(productRepository).findByCode(new ProductCode("LAP001"));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldUpdateProduct() {
        UpdateProductCommand command = new UpdateProductCommand(1L, "Updated Laptop", new BigDecimal("1199.99"), false, null, null);
        ProductAggregate existing = createProductAggregate(1L, "LAP001", "Laptop", "999.99", ProductStatus.ACTIVE);
        when(productRepository.findById(new ProductId(1L))).thenReturn(Optional.of(existing));
        when(productRepository.save(any(ProductAggregate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = service.execute(command);

        assertEquals(new ProductResult(1L, "LAP001", "Updated Laptop", 1199.99, false, 0, 0), result);
        verify(productRepository).findById(new ProductId(1L));
        verify(productRepository).save(any(ProductAggregate.class));
    }

    @Test
    void shouldThrowWhenProductNotFoundDuringUpdate() {
        UpdateProductCommand command = new UpdateProductCommand(404L, "Updated Laptop", new BigDecimal("1199.99"), true, null, null);
        when(productRepository.findById(new ProductId(404L))).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.execute(command));
        verify(productRepository).findById(new ProductId(404L));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldDeleteProduct() {
        ProductAggregate existing = createProductAggregate(1L, "LAP001", "Laptop", "999.99", ProductStatus.ACTIVE);
        when(productRepository.findById(new ProductId(1L))).thenReturn(Optional.of(existing));

        service.execute(new DeleteProductCommand(1L));

        verify(productRepository).findById(new ProductId(1L));
        verify(productRepository).deleteById(new ProductId(1L));
    }

    @Test
    void shouldThrowWhenProductNotFoundDuringDelete() {
        when(productRepository.findById(new ProductId(999L))).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.execute(new DeleteProductCommand(999L)));
        verify(productRepository).findById(new ProductId(999L));
        verify(productRepository, never()).deleteById(any());
    }

    private ProductAggregate createProductAggregate(Long id, String code, String name, String price, ProductStatus status) {
        ProductAggregate aggregate = new ProductAggregate(
            id == null ? null : new ProductId(id),
            new ProductCode(code),
            new ProductName(name),
            new ProductPrice(new BigDecimal(price)),
            0,
            0
        );
        aggregate.updateStatus(status);
        return aggregate;
    }
}
