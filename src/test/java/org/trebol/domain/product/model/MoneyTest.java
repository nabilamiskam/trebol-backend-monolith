package org.trebol.domain.product.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> new Money(-1));
    }

    @Test
    void shouldAcceptPositiveAmount() {
        Money money = new Money(1000);
        assertEquals(1000, money.amount());
    }
}
