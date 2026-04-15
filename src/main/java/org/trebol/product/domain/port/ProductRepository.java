package org.trebol.product.domain.port;

import org.trebol.product.domain.aggregate.ProductAggregate;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductRepository {
    ProductAggregate save(ProductAggregate aggregate);
    Optional<ProductAggregate> findById(ProductId id);
    Optional<ProductAggregate> findByCode(ProductCode code);
    List<ProductAggregate> findAll(int pageIndex, int pageSize, Map<String, String> requestParams);
    long countAll(Map<String, String> requestParams);
    void deleteById(ProductId id);
}
