package org.trebol.product.domain.vo;

import org.junit.jupiter.api.Test;
import org.trebol.product.domain.exception.ProductValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductCodeTest {
    @Test
    void shouldAcceptValidCode() {
        ProductCode code = new ProductCode("P001");

        assertEquals("P001", code.value());
    }

    @Test
    void shouldRejectBlankCode() {
        assertThrows(ProductValidationException.class, () -> new ProductCode("   "));
    }
}