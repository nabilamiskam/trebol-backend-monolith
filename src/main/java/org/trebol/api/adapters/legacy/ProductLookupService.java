package org.trebol.api.adapters.legacy;

import org.trebol.api.models.ProductPojo;
import org.trebol.jpa.entities.Product;

import java.util.Optional;

/**
 * Anti-Corruption Layer: Gateway for all legacy product lookups.
 */
public interface ProductLookupService {
    Optional<ProductPojo> findPojoById(Long id);
    Optional<ProductPojo> findPojoByBarcode(String barcode);
    Optional<Product> findAsJpaById(Long id); // Keeps legacy code happy with transient JPA entities
}
