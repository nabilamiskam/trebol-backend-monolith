package org.trebol.product.domain.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductPriceTest {
    
    @Test
    void shouldCreateValidProductPrice() {
        ProductPrice price = new ProductPrice(new BigDecimal("99.99"));
        assertEquals(new BigDecimal("99.99"), price.value());
    }
    
    @Test
    void shouldCreateProductPriceWithZero() {
        ProductPrice price = new ProductPrice(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, price.value());
    }
    
    @Test
    void shouldCreateProductPriceWithLargeAmount() {
        ProductPrice price = new ProductPrice(new BigDecimal("999999.99"));
        assertEquals(new BigDecimal("999999.99"), price.value());
    }
    
    @Test
    void shouldCreateProductPriceWithManyDecimals() {
        ProductPrice price = new ProductPrice(new BigDecimal("99.9999"));
        assertEquals(new BigDecimal("99.9999"), price.value());
    }
    
    @Test
    void shouldThrowOnNullPrice() {
        assertThrows(NullPointerException.class, () -> new ProductPrice(null));
    }
    
    @Test
    void shouldThrowOnNegativePrice() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ProductPrice(new BigDecimal("-0.01")));
    }
    
    @Test
    void shouldThrowOnNegativeLargePrice() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ProductPrice(new BigDecimal("-999.99")));
    }
    
    @Test
    void shouldBeEqualByValue() {
        ProductPrice price1 = new ProductPrice(new BigDecimal("99.99"));
        ProductPrice price2 = new ProductPrice(new BigDecimal("99.99"));
        assertEquals(price1, price2);
    }
    
    @Test
    void shouldNotBeEqualForDifferentValues() {
        ProductPrice price1 = new ProductPrice(new BigDecimal("99.99"));
        ProductPrice price2 = new ProductPrice(new BigDecimal("100.00"));
        assertNotEquals(price1, price2);
    }
    
    @Test
    void shouldHaveSameHashCodeForEqualValues() {
        ProductPrice price1 = new ProductPrice(new BigDecimal("99.99"));
        ProductPrice price2 = new ProductPrice(new BigDecimal("99.99"));
        assertEquals(price1.hashCode(), price2.hashCode());
    }
}
