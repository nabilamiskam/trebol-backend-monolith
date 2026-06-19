package org.trebol.product.adapter.outbound.persistence;

import org.trebol.product.domain.aggregate.ProductAggregate;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;
import org.trebol.product.domain.vo.ProductName;
import org.trebol.product.domain.vo.ProductPrice;

import java.math.BigDecimal;


public class ProductPersistenceMapper {
    public ProductJpaEntity toEntity(ProductAggregate aggregate) {
        ProductJpaEntity entity = new ProductJpaEntity();
        if (aggregate.getId() != null) {
            entity.setId(aggregate.getId().value());
        }
        entity.setCode(aggregate.getCode().value());
        entity.setName(aggregate.getName().value());
        entity.setPrice(aggregate.getPrice().value().intValue());
        entity.setDescription("");
        entity.setCurrentStock(aggregate.getCurrentStock());
        entity.setCriticalStock(aggregate.getCriticalStock());
        return entity;
    }

    public ProductAggregate toAggregate(ProductJpaEntity entity) {
        ProductAggregate aggregate = new ProductAggregate(
            new ProductId(entity.getId()),
            new ProductCode(entity.getCode()),
            new ProductName(entity.getName()),
            new ProductPrice(BigDecimal.valueOf(entity.getPrice())),
            entity.getCurrentStock() == null ? 0 : entity.getCurrentStock(),
            entity.getCriticalStock() == null ? 0 : entity.getCriticalStock()
        );
        return aggregate;
    }
}
