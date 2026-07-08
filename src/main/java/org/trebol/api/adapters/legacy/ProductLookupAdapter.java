package org.trebol.api.adapters.legacy;

import org.springframework.stereotype.Service;
import org.trebol.product.application.query.GetProductQuery;
import org.trebol.product.application.query.ListProductsQuery;
import org.trebol.product.application.result.ProductResult;
import org.trebol.product.application.result.PagedProductResult;
import org.trebol.product.application.usecase.GetProductUseCase;
import org.trebol.product.application.usecase.ListProductsUseCase;
import org.trebol.api.models.ProductPojo;
import org.trebol.jpa.entities.Product;

import  java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ProductLookupAdapter implements ProductLookupService {

    private final GetProductUseCase getProductUseCase;
    private final ListProductsUseCase listProductsUseCase;

    public ProductLookupAdapter(
        GetProductUseCase getProductUseCase,
        ListProductsUseCase listProductsUseCase
    ) {
        this.getProductUseCase = getProductUseCase;
        this.listProductsUseCase = listProductsUseCase;
    }

    @Override
    public Optional<ProductPojo> findPojoById(Long id) {
        ProductResult r = getProductUseCase.execute(new GetProductQuery(id));
        return r == null ? Optional.empty() : Optional.of(toPojo(r));
    }

    @Override
    public Optional<ProductPojo> findPojoByBarcode(String barcode) {
        PagedProductResult pr = listProductsUseCase.execute(
            new ListProductsQuery(0, 1, Map.of("barcode", barcode))
        );
        return pr.items().stream().findFirst().map(this::toPojo);
    }

    @Override
    public Optional<Product> findAsJpaById(Long id) {
        ProductResult r = getProductUseCase.execute(new GetProductQuery(id));
        return r == null ? Optional.empty() : Optional.of(toTransientJpa(r));
    }

    private ProductPojo toPojo(ProductResult r) {
        ProductPojo p = new ProductPojo();
        p.setName(r.name());
        p.setBarcode(r.code());
        p.setPrice(r.price() == null ? 0 : r.price().intValue());
        p.setCurrentStock(r.currentStock() == null ? 0 : r.currentStock());
        p.setCriticalStock(r.criticalStock() == null ? 0 : r.criticalStock());
        return p;
    }

    private Product toTransientJpa(ProductResult r) {
        return Product.builder()
            .id(r.id())
            .name(r.name())
            .barcode(r.code())
            .price(r.price() == null ? 0 : r.price().intValue())
            .stockCurrent(r.currentStock() == null ? 0 : r.currentStock())
            .stockCritical(r.criticalStock() == null ? 0 : r.criticalStock())
            .build();
    }
}
