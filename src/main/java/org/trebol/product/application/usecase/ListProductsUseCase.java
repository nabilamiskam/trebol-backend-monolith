package org.trebol.product.application.usecase;

import org.trebol.product.application.query.ListProductsQuery;
import org.trebol.product.application.result.PagedProductResult;

public interface ListProductsUseCase {
    PagedProductResult execute(ListProductsQuery query);
}
