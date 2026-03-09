package org.trebol.domain.product.model;

/**
 * Product domain entity (pure business logic, no framework dependencies).
 */
public class Product {
    private Long id;
    private final String name;
    private final Money price;
    
    public Product(Long id, String name, Money price) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }
        if (price == null) {
            throw new IllegalArgumentException("price cannot be null");
        }
        this.id = id;
        this.name = name;
        this.price = price;
    }
    
    public Product(String name, Money price) {
        this(null, name, price);
    }
    
    public Long id() {
        return id;
    }
    
    public String name() {
        return name;
    }
    
    public Money price() {
        return price;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
}
