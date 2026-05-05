package org.trebol.product.adapter.inbound.web;

import org.trebol.product.adapter.inbound.dto.PagedProductResponse;
import org.trebol.product.adapter.inbound.dto.ProductRequest;
import org.trebol.product.adapter.inbound.dto.ProductResponse;
import org.trebol.product.application.command.CreateProductCommand;
import org.trebol.product.application.result.PagedProductResult;
import org.trebol.product.application.result.ProductResult;

import java.util.List;

public class ProductWebMapper {
    public CreateProductCommand toCreateCommand(ProductRequest request) {
        return new CreateProductCommand(request.code, request.name, request.price, request.isActive);
    }

    public ProductResponse toResponse(ProductResult result) {
        ProductResponse response = new ProductResponse();
        response.id = result.id();
        response.code = result.code();
        response.name = result.name();
        response.price = result.price();
        response.isActive = result.isActive();
        return response;
    }

    public PagedProductResponse toPagedResponse(PagedProductResult result) {
        List<ProductResponse> items = result.items().stream()
            .map(this::toResponse)
            .toList();

        PagedProductResponse response = new PagedProductResponse();
        response.items = items;
        response.totalCount = result.totalCount();
        return response;
    }
}
