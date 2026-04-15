package org.trebol.product.domain.vo;

import org.junit.jupiter.api.Test;
import org.trebol.product.domain.exception.ProductValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductIdTest {
    @Test
    void shouldAcceptPositiveId() {
        ProductId id = new ProductId(1L);

        assertEquals(1L, id.value());
    }

    @Test
    void shouldRejectNonPositiveId() {
        assertThrows(ProductValidationException.class, () -> new ProductId(0L));
    }
}