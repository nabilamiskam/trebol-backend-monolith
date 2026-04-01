package org.trebol.product.application.result;

import java.util.List;

public record PagedProductResult(
    List<ProductResult> items,
    long totalCount
) {
}
