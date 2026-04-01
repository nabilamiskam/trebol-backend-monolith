package org.trebol.product.domain.event;

import org.trebol.product.domain.vo.ProductId;

import java.time.Instant;

public record ProductUpdatedEvent(ProductId productId, Instant occurredAt) {
    public ProductUpdatedEvent(ProductId productId) {
        this(productId, Instant.now());
    }
}
