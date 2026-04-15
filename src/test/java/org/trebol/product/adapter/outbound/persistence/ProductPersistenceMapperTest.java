package org.trebol.product.adapter.outbound.persistence;

import org.junit.jupiter.api.Test;
import org.trebol.product.domain.aggregate.ProductAggregate;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;
import org.trebol.product.domain.vo.ProductName;
import org.trebol.product.domain.vo.ProductPrice;
import org.trebol.product.domain.vo.ProductStatus;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductPersistenceMapperTest {
    private final ProductPersistenceMapper mapper = new ProductPersistenceMapper();

    @Test
    void shouldMapStatusToEntityAndBack() {
        ProductAggregate aggregate = ProductAggregate.restore(
            new ProductId(1L),
            new ProductCode("P001"),
            new ProductName("Product 1"),
            new ProductPrice(BigDecimal.valueOf(10)),
            ProductStatus.INACTIVE
        );

        ProductJpaEntity entity = mapper.toEntity(aggregate);

        assertEquals("INACTIVE", entity.getStatus());

        ProductAggregate restored = mapper.toAggregate(entity);

        assertEquals(ProductStatus.INACTIVE, restored.getStatus());
        assertEquals("P001", restored.getCode().value());
    }
}