package org.trebol.product.application.result;

import java.util.List;

public record BulkPatchProductResult(
    List<ProductResult> items,
    long updatedCount
) {
    public BulkPatchProductResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}