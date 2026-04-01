package org.trebol.product.domain.event;

import org.trebol.product.domain.vo.ProductId;

import java.time.Instant;

public record ProductCreatedEvent(ProductId productId, Instant occurredAt) {
    public ProductCreatedEvent(ProductId productId) {
        this(productId, Instant.now());
    }
}
