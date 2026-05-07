package org.trebol.product.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductCodeTest {
    
    @Test
    void shouldCreateValidProductCode() {
        ProductCode code = new ProductCode("PROD-001");
        assertEquals("PROD-001", code.value());
    }
    
    @Test
    void shouldCreateProductCodeWithSpecialChars() {
        ProductCode code = new ProductCode("PROD_001-ABC");
        assertEquals("PROD_001-ABC", code.value());
    }
    
    @Test
    void shouldThrowOnNullCode() {
        assertThrows(NullPointerException.class, () -> new ProductCode(null));
    }
    
    @Test
    void shouldThrowOnBlankCode() {
        assertThrows(IllegalArgumentException.class, () -> new ProductCode(""));
    }
    
    @Test
    void shouldThrowOnWhitespaceOnlyCode() {
        assertThrows(IllegalArgumentException.class, () -> new ProductCode("   "));
    }
    
    @Test
    void shouldBeEqualByValue() {
        ProductCode code1 = new ProductCode("PROD-001");
        ProductCode code2 = new ProductCode("PROD-001");
        assertEquals(code1, code2);
    }
    
    @Test
    void shouldNotBeEqualForDifferentValues() {
        ProductCode code1 = new ProductCode("PROD-001");
        ProductCode code2 = new ProductCode("PROD-002");
        assertNotEquals(code1, code2);
    }
    
    @Test
    void shouldHaveSameHashCodeForEqualValues() {
        ProductCode code1 = new ProductCode("PROD-001");
        ProductCode code2 = new ProductCode("PROD-001");
        assertEquals(code1.hashCode(), code2.hashCode());
    }
}
