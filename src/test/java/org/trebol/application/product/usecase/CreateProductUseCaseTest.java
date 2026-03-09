package org.trebol.application.product.usecase;

import org.junit.jupiter.api.Test;
import org.trebol.application.product.port.ProductRepository;
import org.trebol.domain.product.model.Product;

import static org.junit.jupiter.api.Assertions.*;

class CreateProductUseCaseTest {

    @Test
    void shouldCreateProductWithValidPrice() {
        // Arrange
        FakeProductRepository fakeRepository = new FakeProductRepository();
        CreateProductUseCase useCase = new CreateProductUseCase(fakeRepository);
        
        // Act
        Product result = useCase.execute("Test Product", 1000);
        
        // Assert
        assertNotNull(result);
        assertEquals("Test Product", result.name());
        assertEquals(1000, result.price().amount());
        assertTrue(fakeRepository.saveCalled);
    }
    
    @Test
    void shouldRejectNegativePrice() {
        // Arrange
        FakeProductRepository fakeRepository = new FakeProductRepository();
        CreateProductUseCase useCase = new CreateProductUseCase(fakeRepository);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> useCase.execute("Test Product", -100));
        assertFalse(fakeRepository.saveCalled, "save should not be called with invalid price");
    }
    
    @Test
    void shouldRejectEmptyName() {
        // Arrange
        FakeProductRepository fakeRepository = new FakeProductRepository();
        CreateProductUseCase useCase = new CreateProductUseCase(fakeRepository);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> useCase.execute("", 1000));
        assertFalse(fakeRepository.saveCalled, "save should not be called with invalid name");
    }
    
    /**
     * Fake in-memory repository for testing (no database needed!)
     */
    private static class FakeProductRepository implements ProductRepository {
        boolean saveCalled = false;
        Product savedProduct = null;
        
        @Override
        public Product save(Product product) {
            saveCalled = true;
            savedProduct = product;
            // Simulate ID generation
            product.setId(1L);
            return product;
        }
        
        @Override
        public Product findById(Long id) {
            return savedProduct;
        }
    }
}
