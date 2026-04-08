package org.trebol.product.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {
    Optional<ProductJpaEntity> findByCode(String code);
}
