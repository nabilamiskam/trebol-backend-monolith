package org.trebol.application.product.usecase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.trebol.application.product.port.ProductRepository;
import org.trebol.domain.product.model.Money;
import org.trebol.domain.product.model.Product;

/**
 * Use case for creating a new product.
 * Application layer - orchestrates domain logic and calls ports.
 */
@Component
public class CreateProductUseCase {
    private final ProductRepository productRepository;
    
    @Autowired
    public CreateProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    /**
     * Execute the use case to create a product.
     * @param name product name
     * @param priceAmount price in cents/smallest currency unit
     * @return the created product with ID
     */
    public Product execute(String name, int priceAmount) {
        // Validate price using domain Money value object
        Money price = new Money(priceAmount);
        
        // Create domain entity
        Product product = new Product(name, price);
        
        // Save via port (abstraction)
        return productRepository.save(product);
    }
}
