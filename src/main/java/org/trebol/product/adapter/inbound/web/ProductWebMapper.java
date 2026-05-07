package org.trebol.product.adapter.inbound.web;

import org.trebol.product.adapter.inbound.dto.PagedProductResponse;
import org.trebol.product.adapter.inbound.dto.BulkPatchProductResponse;
import org.trebol.product.adapter.inbound.dto.ProductRequest;
import org.trebol.product.adapter.inbound.dto.ProductResponse;
import org.trebol.product.application.command.CreateProductCommand;
import org.trebol.product.application.command.DeleteProductCommand;
import org.trebol.product.application.command.UpdateProductCommand;
import org.trebol.product.application.result.BulkPatchProductResult;
import org.trebol.product.application.result.PagedProductResult;
import org.trebol.product.application.result.ProductResult;

import java.math.BigDecimal;
import java.util.List;

public class ProductWebMapper {
    public CreateProductCommand toCreateCommand(ProductRequest request) {
        BigDecimal price = request.price != null ? BigDecimal.valueOf(request.price) : BigDecimal.ZERO;
        return new CreateProductCommand(request.code, request.name, price, request.isActive != null && request.isActive);
    }

    public UpdateProductCommand toUpdateCommand(Long id, ProductRequest request) {
        BigDecimal price = request.price != null ? BigDecimal.valueOf(request.price) : null;
        return new UpdateProductCommand(id, request.name, price, request.isActive != null && request.isActive);
    }

    public DeleteProductCommand toDeleteCommand(Long id) {
        return new DeleteProductCommand(id);
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

    public BulkPatchProductResponse toBulkPatchResponse(BulkPatchProductResult result) {
        BulkPatchProductResponse response = new BulkPatchProductResponse();
        response.items = result.items().stream()
            .map(this::toResponse)
            .toList();
        response.updatedCount = result.updatedCount();
        return response;
    }
}
