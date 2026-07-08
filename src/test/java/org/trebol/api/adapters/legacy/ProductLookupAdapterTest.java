package org.trebol.api.adapters.legacy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trebol.product.application.query.GetProductQuery;
import org.trebol.product.application.query.ListProductsQuery;
import org.trebol.product.application.result.PagedProductResult;
import org.trebol.product.application.result.ProductResult;
import org.trebol.product.application.usecase.GetProductUseCase;
import org.trebol.product.application.usecase.ListProductsUseCase;
import org.trebol.api.models.ProductPojo;
import org.trebol.jpa.entities.Product;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductLookupAdapterTest {
    @InjectMocks
    ProductLookupAdapter adapter;

    @Mock
    GetProductUseCase getProductUseCase;

    @Mock
    ListProductsUseCase listProductsUseCase;

    @Test
    void returnsEmptyWhenServiceReturnsNull() {
        when(getProductUseCase.execute(any(GetProductQuery.class))).thenReturn(null);

        Optional<ProductPojo> found = adapter.findPojoById(1L);

        assertTrue(found.isEmpty());
    }

    @Test
    void mapsPojoFieldsFromProductResult() {
        ProductResult r = new ProductResult(1L, "CODE-1", "Product One", 123.45, true, 0, 0);
        when(getProductUseCase.execute(any(GetProductQuery.class))).thenReturn(r);

        Optional<ProductPojo> found = adapter.findPojoById(1L);

        assertTrue(found.isPresent());
        ProductPojo p = found.get();
        assertEquals("Product One", p.getName());
        assertEquals("CODE-1", p.getBarcode());
        assertEquals(123, p.getPrice());
    }

    @Test
    void findPojoByBarcodeReturnsFirstPagedItem() {
        ProductResult first = new ProductResult(1L, "C1", "First", 10.0, true, 0, 0);
        PagedProductResult pr = new PagedProductResult(List.of(first), 1L);
        when(listProductsUseCase.execute(any(ListProductsQuery.class))).thenReturn(pr);

        Optional<ProductPojo> found = adapter.findPojoByBarcode("C1");

        assertTrue(found.isPresent());
        assertEquals("C1", found.get().getBarcode());
    }

    @Test
    void findPojoByBarcodeReturnsEmptyWhenNoProductsFound() {
        PagedProductResult pr = new PagedProductResult(List.of(), 0L);
        when(listProductsUseCase.execute(any(ListProductsQuery.class))).thenReturn(pr);

        Optional<ProductPojo> found = adapter.findPojoByBarcode("UNKNOWN");

        assertTrue(found.isEmpty());
    }

    @Test
    void findAsJpaByIdReturnsTransientEntity() {
        ProductResult r = new ProductResult(5L, "C5", "Five", 50.0, true, 0, 0);
        when(getProductUseCase.execute(any(GetProductQuery.class))).thenReturn(r);

        Optional<Product> entity = adapter.findAsJpaById(5L);

        assertTrue(entity.isPresent());
        Product p = entity.get();
        assertEquals(5L, p.getId());
        assertEquals("C5", p.getBarcode());
        assertEquals("Five", p.getName());
        assertEquals(50, p.getPrice());
    }
}
