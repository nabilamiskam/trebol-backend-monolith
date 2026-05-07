package org.trebol.product.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductIdTest {
    
    @Test
    void shouldCreateValidProductId() {
        ProductId id = new ProductId(1L);
        assertEquals(1L, id.value());
    }
    
    @Test
    void shouldCreateProductIdWithLargeValue() {
        ProductId id = new ProductId(999999999L);
        assertEquals(999999999L, id.value());
    }
    
    @Test
    void shouldThrowOnNullValue() {
        assertThrows(NullPointerException.class, () -> new ProductId(null));
    }
    
    @Test
    void shouldThrowOnZeroValue() {
        assertThrows(IllegalArgumentException.class, () -> new ProductId(0L));
    }
    
    @Test
    void shouldThrowOnNegativeValue() {
        assertThrows(IllegalArgumentException.class, () -> new ProductId(-1L));
    }
    
    @Test
    void shouldBeEqualByValue() {
        ProductId id1 = new ProductId(42L);
        ProductId id2 = new ProductId(42L);
        assertEquals(id1, id2);
    }
    
    @Test
    void shouldNotBeEqualForDifferentValues() {
        ProductId id1 = new ProductId(42L);
        ProductId id2 = new ProductId(43L);
        assertNotEquals(id1, id2);
    }
    
    @Test
    void shouldHaveSameHashCodeForEqualValues() {
        ProductId id1 = new ProductId(42L);
        ProductId id2 = new ProductId(42L);
        assertEquals(id1.hashCode(), id2.hashCode());
    }
}
