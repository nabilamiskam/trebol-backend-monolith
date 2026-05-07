package org.trebol.product.adapter.inbound.dto;

import java.util.List;

public class BulkPatchProductResponse {
    public List<ProductResponse> items;
    public long updatedCount;
}