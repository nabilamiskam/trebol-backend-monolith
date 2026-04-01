package org.trebol.product.application.usecase;

import org.trebol.product.application.query.GetProductQuery;
import org.trebol.product.application.result.ProductResult;

public interface GetProductUseCase {
    ProductResult execute(GetProductQuery query);
}
