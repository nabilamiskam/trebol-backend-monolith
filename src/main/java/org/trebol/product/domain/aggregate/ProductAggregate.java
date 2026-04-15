package org.trebol.product.domain.aggregate;

import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;
import org.trebol.product.domain.vo.ProductName;
import org.trebol.product.domain.vo.ProductPrice;
import org.trebol.product.domain.vo.ProductStatus;

import java.util.Objects;

public class ProductAggregate {
    private final ProductId id;
    private final ProductCode code;
    private ProductName name;
    private ProductPrice price;
    private ProductStatus status;

    public ProductAggregate(ProductId id, ProductCode code, ProductName name, ProductPrice price) {
        this(id, code, name, price, ProductStatus.ACTIVE);
    }

    private ProductAggregate(
        ProductId id,
        ProductCode code,
        ProductName name,
        ProductPrice price,
        ProductStatus status
    ) {
        this.id = Objects.requireNonNull(id);
        this.code = Objects.requireNonNull(code);
        this.name = Objects.requireNonNull(name);
        this.price = Objects.requireNonNull(price);
        this.status = Objects.requireNonNull(status);
    }

    public static ProductAggregate create(
        ProductId id,
        ProductCode code,
        ProductName name,
        ProductPrice price
    ) {
        return new ProductAggregate(id, code, name, price, ProductStatus.ACTIVE);
    }

    public static ProductAggregate restore(
        ProductId id,
        ProductCode code,
        ProductName name,
        ProductPrice price,
        ProductStatus status
    ) {
        return new ProductAggregate(id, code, name, price, status);
    }

    public ProductId getId() {
        return id;
    }

    public ProductCode getCode() {
        return code;
    }

    public ProductName getName() {
        return name;
    }

    public ProductPrice getPrice() {
        return price;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void rename(ProductName name) {
        this.name = Objects.requireNonNull(name);
    }

    public void reprice(ProductPrice price) {
        this.price = Objects.requireNonNull(price);
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }
}
