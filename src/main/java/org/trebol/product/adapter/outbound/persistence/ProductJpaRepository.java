package org.trebol.product.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long>, JpaSpecificationExecutor<ProductJpaEntity> {
    Optional<ProductJpaEntity> findByCode(String code);
}
