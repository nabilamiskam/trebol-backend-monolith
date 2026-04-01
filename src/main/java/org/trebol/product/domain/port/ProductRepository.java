package org.trebol.product.domain.port;

import org.trebol.product.domain.aggregate.ProductAggregate;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    ProductAggregate save(ProductAggregate aggregate);
    Optional<ProductAggregate> findById(ProductId id);
    Optional<ProductAggregate> findByCode(ProductCode code);
    List<ProductAggregate> findAll(int pageIndex, int pageSize);
    long countAll();
    void deleteById(ProductId id);
}
