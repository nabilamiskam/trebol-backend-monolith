package org.trebol.product.application.result;

public record ProductResult(
    Long id,
    String code,
    String name,
    Double price,
    Boolean isActive,
    Integer currentStock,
    Integer criticalStock
) {
}


