package org.trebol.product.domain.aggregate;

import org.junit.jupiter.api.Test;
import org.trebol.product.domain.exception.ProductValidationException;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;
import org.trebol.product.domain.vo.ProductName;
import org.trebol.product.domain.vo.ProductPrice;
import org.trebol.product.domain.vo.ProductStatus;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductAggregateTest {
    @Test
    void createShouldBuildActiveAggregate() {
        ProductAggregate aggregate = ProductAggregate.create(
            new ProductId(1L),
            new ProductCode("P001"),
            new ProductName("Product 1"),
            new ProductPrice(BigDecimal.valueOf(100))
        );

        assertEquals(1L, aggregate.getId().value());
        assertEquals("P001", aggregate.getCode().value());
        assertEquals("Product 1", aggregate.getName().value());
        assertEquals(BigDecimal.valueOf(100), aggregate.getPrice().value());
        assertEquals(ProductStatus.ACTIVE, aggregate.getStatus());
    }

    @Test
    void restoreShouldPreserveInactiveStatus() {
        ProductAggregate aggregate = ProductAggregate.restore(
            new ProductId(2L),
            new ProductCode("P002"),
            new ProductName("Product 2"),
            new ProductPrice(BigDecimal.valueOf(250)),
            ProductStatus.INACTIVE
        );

        assertEquals(ProductStatus.INACTIVE, aggregate.getStatus());
    }

    @Test
    void renameShouldUpdateName() {
        ProductAggregate aggregate = createAggregate();

        aggregate.rename(new ProductName("Updated Product"));

        assertEquals("Updated Product", aggregate.getName().value());
    }

    @Test
    void repriceShouldUpdatePrice() {
        ProductAggregate aggregate = createAggregate();

        aggregate.reprice(new ProductPrice(BigDecimal.valueOf(175)));

        assertEquals(BigDecimal.valueOf(175), aggregate.getPrice().value());
    }

    @Test
    void deactivateAndActivateShouldToggleStatus() {
        ProductAggregate aggregate = createAggregate();

        aggregate.deactivate();
        assertEquals(ProductStatus.INACTIVE, aggregate.getStatus());

        aggregate.activate();
        assertEquals(ProductStatus.ACTIVE, aggregate.getStatus());
    }

    @Test
    void invalidNameShouldFailFast() {
        assertThrows(ProductValidationException.class, () -> new ProductName("   "));
    }

    private ProductAggregate createAggregate() {
        return ProductAggregate.create(
            new ProductId(1L),
            new ProductCode("P001"),
            new ProductName("Product 1"),
            new ProductPrice(BigDecimal.valueOf(100))
        );
    }
}
