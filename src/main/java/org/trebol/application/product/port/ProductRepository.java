package org.trebol.application.product.port;

import org.trebol.domain.product.model.Product;

/**
 * Port interface for product persistence.
 * This is an abstraction - implementations live in the adapter layer.
 */
public interface ProductRepository {
    
    /**
     * Save a product to persistence.
     * @param product the product to save
     * @return the saved product with generated ID
     */
    Product save(Product product);
    
    /**
     * Find a product by its ID.
     * @param id the product ID
     * @return the product if found
     */
    Product findById(Long id);
}
