package org.trebol.product.application.query;

import java.util.Map;

public record ListProductsQuery(int pageIndex, int pageSize, Map<String, String> requestParams) {
	public ListProductsQuery {
		requestParams = requestParams == null ? Map.of() : Map.copyOf(requestParams);
	}
}
