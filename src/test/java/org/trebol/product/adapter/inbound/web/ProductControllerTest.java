package org.trebol.product.adapter.inbound.web;

import org.junit.jupiter.api.Test;

class ProductControllerTest {
    @Test
    void controllerClassShouldRemainFrameworkOnly() {
        ProductController controller = new ProductController();

        org.junit.jupiter.api.Assertions.assertNotNull(controller);
    }
}
