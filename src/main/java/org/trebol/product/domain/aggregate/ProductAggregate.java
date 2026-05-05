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
        this.id = id;
        this.code = Objects.requireNonNull(code);
        this.name = Objects.requireNonNull(name);
        this.price = Objects.requireNonNull(price);
        this.status = ProductStatus.ACTIVE;
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

    public void updateName(ProductName name) {
        this.name = Objects.requireNonNull(name);
    }

    public void updatePrice(ProductPrice price) {
        this.price = Objects.requireNonNull(price);
    }

    public void updateStatus(ProductStatus status) {
        this.status = Objects.requireNonNull(status);
    }
}
