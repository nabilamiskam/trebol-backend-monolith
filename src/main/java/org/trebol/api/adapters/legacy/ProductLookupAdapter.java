package org.trebol.api.adapters.legacy;

import org.springframework.stereotype.Service;
import org.trebol.product.application.query.GetProductQuery;
import org.trebol.product.application.query.ListProductsQuery;
import org.trebol.product.application.result.ProductResult;
import org.trebol.product.application.result.PagedProductResult;
import org.trebol.product.application.service.ProductApplicationService;
import org.trebol.api.models.ProductPojo;
import org.trebol.jpa.entities.Product;

import  java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ProductLookupAdapter implements ProductLookupService {

    private final ProductApplicationService productApplicationService;

    public ProductLookupAdapter(ProductApplicationService productApplicationService) {
        this.productApplicationService = productApplicationService;
    }

    @Override
    public Optional<ProductPojo> findPojoById(Long id) {
        ProductResult r = productApplicationService.execute(new GetProductQuery(id));
        return r == null ? Optional.empty() : Optional.of(toPojo(r));
    }

    @Override
    public Optional<ProductPojo> findPojoByBarcode(String barcode) {
        PagedProductResult pr = productApplicationService.execute(new ListProductsQuery(0, 1, Map.of("barcode", barcode)));
        return pr.items().stream().findFirst().map(this::toPojo);
    }

    @Override
    public Optional<Product> findAsJpaById(Long id) {
        ProductResult r = productApplicationService.execute(new GetProductQuery(id));
        return r == null ? Optional.empty() : Optional.of(toTransientJpa(r));
    }

    private ProductPojo toPojo(ProductResult r) {
        ProductPojo p = new ProductPojo();
        p.setName(r.name());
        p.setBarcode(r.code());
        p.setPrice(r.price() == null ? 0 : r.price().intValue());
        return p;
    }

    private Product toTransientJpa(ProductResult r) {
        return Product.builder()
            .id(r.id())
            .name(r.name())
            .barcode(r.code())
            .price(r.price() == null ? 0 : r.price().intValue())
            .build();
    }
}
