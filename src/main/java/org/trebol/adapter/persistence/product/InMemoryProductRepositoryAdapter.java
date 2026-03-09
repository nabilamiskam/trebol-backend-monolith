package org.trebol.adapter.persistence.product;

import org.springframework.stereotype.Component;
import org.trebol.application.product.port.ProductRepository;
import org.trebol.domain.product.model.Product;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory adapter for ProductRepository (clean architecture slice).
 * For testing/demo only. Replace with JPA adapter in production.
 */
@Component
public class InMemoryProductRepositoryAdapter implements ProductRepository {
    
    private final ConcurrentHashMap<Long, Product> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public Product save(Product product) {
        if (product.id() == null) {
            // Generate ID for new product
            long newId = idGenerator.getAndIncrement();
            product.setId(newId);
        }
        storage.put(product.id(), product);
        return product;
    }
    
    @Override
    public Product findById(Long id) {
        Product found = storage.get(id);
        if (found == null) {
            throw new RuntimeException("Product not found: " + id);
        }
        return found;
    }
}
