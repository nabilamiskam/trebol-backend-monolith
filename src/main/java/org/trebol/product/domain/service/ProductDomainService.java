package org.trebol.product.domain.service;

import org.trebol.product.domain.exception.ProductCodeAlreadyExistsException;
import org.trebol.product.domain.port.ProductRepository;
import org.trebol.product.domain.vo.ProductCode;

import java.util.Objects;

public class ProductDomainService {
    private final ProductRepository productRepository;

    public ProductDomainService(ProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository);
    }

    public void ensureCodeAvailable(ProductCode code) {
        if (productRepository.findByCode(code).isPresent()) {
            throw new ProductCodeAlreadyExistsException("Product code already exists: " + code.value());
        }
    }
}
