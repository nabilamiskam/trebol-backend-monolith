package org.trebol.product.domain.vo;

import org.junit.jupiter.api.Test;
import org.trebol.product.domain.exception.ProductValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductNameTest {
    @Test
    void shouldAcceptValidName() {
        ProductName name = new ProductName("Product 1");

        assertEquals("Product 1", name.value());
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(ProductValidationException.class, () -> new ProductName("   "));
    }
}