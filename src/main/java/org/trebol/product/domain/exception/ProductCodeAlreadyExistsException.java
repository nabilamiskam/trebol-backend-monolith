package org.trebol.product.domain.exception;

public class ProductCodeAlreadyExistsException extends RuntimeException {
    public ProductCodeAlreadyExistsException(String message) {
        super(message);
    }
}
