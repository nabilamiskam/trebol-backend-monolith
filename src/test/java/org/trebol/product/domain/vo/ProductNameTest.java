package org.trebol.product.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductNameTest {
    
    @Test
    void shouldCreateValidProductName() {
        ProductName name = new ProductName("Laptop");
        assertEquals("Laptop", name.value());
    }
    
    @Test
    void shouldCreateProductNameWithSpaces() {
        ProductName name = new ProductName("Professional Laptop 15 inch");
        assertEquals("Professional Laptop 15 inch", name.value());
    }
    
    @Test
    void shouldThrowOnNullName() {
        assertThrows(NullPointerException.class, () -> new ProductName(null));
    }
    
    @Test
    void shouldThrowOnBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new ProductName(""));
    }
    
    @Test
    void shouldThrowOnWhitespaceOnlyName() {
        assertThrows(IllegalArgumentException.class, () -> new ProductName("   "));
    }
    
    @Test
    void shouldThrowOnExcessiveLength() {
        String tooLong = "a".repeat(256);
        assertThrows(IllegalArgumentException.class, () -> new ProductName(tooLong));
    }
    
    @Test
    void shouldCreateValidNameAt255Chars() {
        String maxLength = "a".repeat(255);
        ProductName name = new ProductName(maxLength);
        assertEquals(maxLength, name.value());
    }
    
    @Test
    void shouldBeEqualByValue() {
        ProductName name1 = new ProductName("Laptop");
        ProductName name2 = new ProductName("Laptop");
        assertEquals(name1, name2);
    }
    
    @Test
    void shouldNotBeEqualForDifferentValues() {
        ProductName name1 = new ProductName("Laptop");
        ProductName name2 = new ProductName("Desktop");
        assertNotEquals(name1, name2);
    }
    
    @Test
    void shouldHaveSameHashCodeForEqualValues() {
        ProductName name1 = new ProductName("Laptop");
        ProductName name2 = new ProductName("Laptop");
        assertEquals(name1.hashCode(), name2.hashCode());
    }
}
