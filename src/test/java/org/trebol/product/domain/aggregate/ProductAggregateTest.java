package org.trebol.product.domain.aggregate;

import org.junit.jupiter.api.Test;
import org.trebol.product.domain.vo.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductAggregateTest {
    
    @Test
    void shouldCreateProductAggregate() {
        ProductAggregate product = new ProductAggregate(
            new ProductId(1L),
            new ProductCode("LAP001"),
            new ProductName("Laptop"),
            new ProductPrice(new BigDecimal("999.99"))
        );
        
        assertEquals(1L, product.getId().value());
        assertEquals("LAP001", product.getCode().value());
        assertEquals("Laptop", product.getName().value());
        assertEquals(new BigDecimal("999.99"), product.getPrice().value());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
    }
    
    @Test
    void shouldInitializeWithActiveStatus() {
        ProductAggregate product = createDefaultProduct();
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
    }
    
    @Test
    void shouldUpdateProductName() {
        ProductAggregate product = createDefaultProduct();
        ProductName newName = new ProductName("Premium Laptop");
        product.updateName(newName);
        assertEquals("Premium Laptop", product.getName().value());
    }
    
    @Test
    void shouldUpdateProductPrice() {
        ProductAggregate product = createDefaultProduct();
        ProductPrice newPrice = new ProductPrice(new BigDecimal("1299.99"));
        product.updatePrice(newPrice);
        assertEquals(new BigDecimal("1299.99"), product.getPrice().value());
    }
    
    @Test
    void shouldUpdateProductStatus() {
        ProductAggregate product = createDefaultProduct();
        product.updateStatus(ProductStatus.INACTIVE);
        assertEquals(ProductStatus.INACTIVE, product.getStatus());
    }
    
    @Test
    void shouldToggleStatusFromActiveToInactive() {
        ProductAggregate product = createDefaultProduct();
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
        product.updateStatus(ProductStatus.INACTIVE);
        assertEquals(ProductStatus.INACTIVE, product.getStatus());
    }
    
    @Test
    void shouldToggleStatusFromInactiveToActive() {
        ProductAggregate product = createDefaultProduct();
        product.updateStatus(ProductStatus.INACTIVE);
        product.updateStatus(ProductStatus.ACTIVE);
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
    }
    
    @Test
    void shouldThrowOnNullNameUpdate() {
        ProductAggregate product = createDefaultProduct();
        assertThrows(NullPointerException.class, () -> product.updateName(null));
    }
    
    @Test
    void shouldThrowOnNullPriceUpdate() {
        ProductAggregate product = createDefaultProduct();
        assertThrows(NullPointerException.class, () -> product.updatePrice(null));
    }
    
    @Test
    void shouldThrowOnNullStatusUpdate() {
        ProductAggregate product = createDefaultProduct();
        assertThrows(NullPointerException.class, () -> product.updateStatus(null));
    }
    
    @Test
    void shouldSupportMultipleConsecutiveUpdates() {
        ProductAggregate product = createDefaultProduct();
        product.updateName(new ProductName("Updated Name"));
        product.updatePrice(new ProductPrice(new BigDecimal("1199.99")));
        product.updateStatus(ProductStatus.INACTIVE);
        
        assertEquals("Updated Name", product.getName().value());
        assertEquals(new BigDecimal("1199.99"), product.getPrice().value());
        assertEquals(ProductStatus.INACTIVE, product.getStatus());
    }
    
    @Test
    void shouldPreserveCodeAfterUpdates() {
        ProductAggregate product = createDefaultProduct();
        String originalCode = product.getCode().value();
        product.updateName(new ProductName("New Name"));
        product.updatePrice(new ProductPrice(new BigDecimal("555.55")));
        
        assertEquals(originalCode, product.getCode().value());
    }
    
    @Test
    void shouldPreserveIdAfterUpdates() {
        ProductAggregate product = createDefaultProduct();
        Long originalId = product.getId().value();
        product.updateName(new ProductName("New Name"));
        
        assertEquals(originalId, product.getId().value());
    }
    
    private ProductAggregate createDefaultProduct() {
        return new ProductAggregate(
            new ProductId(1L),
            new ProductCode("LAP001"),
            new ProductName("Laptop"),
            new ProductPrice(new BigDecimal("999.99"))
        );
    }
}
