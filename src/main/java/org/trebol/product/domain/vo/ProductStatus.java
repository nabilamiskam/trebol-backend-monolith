package org.trebol.product.domain.vo;

public enum ProductStatus {
    ACTIVE,
    INACTIVE;

    public static ProductStatus fromBoolean(Boolean active) {
        return Boolean.FALSE.equals(active) ? INACTIVE : ACTIVE;
    }

    public boolean asBoolean() {
        return this == ACTIVE;
    }
}
