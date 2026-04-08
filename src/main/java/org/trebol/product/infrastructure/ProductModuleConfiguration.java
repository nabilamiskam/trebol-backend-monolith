package org.trebol.product.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.trebol.product.adapter.inbound.web.ProductWebMapper;
import org.trebol.product.adapter.outbound.persistence.ProductJpaRepository;
import org.trebol.product.adapter.outbound.persistence.ProductPersistenceMapper;
import org.trebol.product.adapter.outbound.persistence.ProductRepositoryAdapter;
import org.trebol.product.application.service.ProductApplicationMapper;
import org.trebol.product.application.service.ProductApplicationService;
import org.trebol.product.domain.port.ProductRepository;

@Configuration
public class ProductModuleConfiguration {

    @Bean
    public ProductPersistenceMapper productPersistenceMapper() {
        return new ProductPersistenceMapper();
    }

    @Bean
    public ProductRepository productRepository(ProductJpaRepository jpaRepository, ProductPersistenceMapper mapper) {
        return new ProductRepositoryAdapter(jpaRepository, mapper);
    }

    @Bean
    public ProductApplicationMapper productApplicationMapper() {
        return new ProductApplicationMapper();
    }

    @Bean
    public ProductApplicationService productApplicationService(ProductRepository repository, ProductApplicationMapper mapper) {
        return new ProductApplicationService(repository, mapper);
    }

    @Bean
    public ProductWebMapper productWebMapper() {
        return new ProductWebMapper();
    }
}
