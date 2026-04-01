package org.trebol.product.application.service;

import org.trebol.product.application.result.ProductResult;
import org.trebol.product.domain.aggregate.ProductAggregate;

public class ProductApplicationMapper {
    public ProductResult toResult(ProductAggregate aggregate) {
        return new ProductResult(
            aggregate.getId().value(),
            aggregate.getCode().value(),
            aggregate.getName().value(),
            aggregate.getPrice().value().doubleValue(),
            aggregate.getStatus().asBoolean()
        );
    }
}
