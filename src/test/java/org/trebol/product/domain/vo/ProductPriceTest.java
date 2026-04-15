package org.trebol.product.domain.vo;

import org.junit.jupiter.api.Test;
import org.trebol.product.domain.exception.ProductValidationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductPriceTest {
    @Test
    void shouldAcceptNonNegativePrice() {
        ProductPrice price = new ProductPrice(BigDecimal.valueOf(10));

        assertEquals(BigDecimal.valueOf(10), price.value());
    }

    @Test
    void shouldRejectNegativePrice() {
        assertThrows(ProductValidationException.class, () -> new ProductPrice(BigDecimal.valueOf(-1)));
    }
}