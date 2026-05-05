package org.trebol.product.adapter.inbound.dto;

import java.util.List;

public class PagedProductResponse {
    public List<ProductResponse> items;
    public long totalCount;
}