package org.trebol.product.adapter.outbound.persistence;

import org.trebol.product.domain.aggregate.ProductAggregate;
import org.trebol.product.domain.port.ProductRepository;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;

import java.util.List;
import java.util.Optional;

public class ProductRepositoryAdapter implements ProductRepository {
    private final ProductJpaRepository jpaRepository;
    private final ProductPersistenceMapper mapper;

    public ProductRepositoryAdapter(ProductJpaRepository jpaRepository, ProductPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ProductAggregate save(ProductAggregate aggregate) {
        ProductJpaEntity saved = jpaRepository.save(mapper.toEntity(aggregate));
        return mapper.toAggregate(saved);
    }

    @Override
    public Optional<ProductAggregate> findById(ProductId id) {
        return jpaRepository.findById(id.value()).map(mapper::toAggregate);
    }

    @Override
    public Optional<ProductAggregate> findByCode(ProductCode code) {
        return jpaRepository.findByCode(code.value()).map(mapper::toAggregate);
    }

    @Override
    public List<ProductAggregate> findAll(int pageIndex, int pageSize) {
        return List.of();
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }

    @Override
    public void deleteById(ProductId id) {
        jpaRepository.deleteById(id.value());
    }
}
