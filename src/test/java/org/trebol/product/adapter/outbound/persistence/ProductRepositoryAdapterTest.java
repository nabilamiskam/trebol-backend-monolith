package org.trebol.product.adapter.outbound.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.trebol.BackendApp;
import org.trebol.product.domain.aggregate.ProductAggregate;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;
import org.trebol.product.domain.vo.ProductName;
import org.trebol.product.domain.vo.ProductPrice;
import org.trebol.product.domain.vo.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BackendApp.class)
class ProductRepositoryAdapterTest {

    @Autowired
    private ProductRepositoryAdapter productRepository;

    @Autowired
    private ProductJpaRepository jpaRepository;

    @BeforeEach
    void cleanup() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldSaveNewProduct() {
        ProductAggregate product = createAggregate(null, "LAP001", "Laptop", 999);

        ProductAggregate saved = productRepository.save(product);

        assertNotNull(saved.getId());
        assertTrue(jpaRepository.existsById(saved.getId().value()));
        assertEquals("LAP001", saved.getCode().value());
        assertEquals("Laptop", saved.getName().value());
        assertEquals(new BigDecimal("999"), saved.getPrice().value());
    }

    @Test
    void shouldFindProductById() {
        ProductJpaEntity entity = createEntity("LAP001", "Laptop", 999);
        ProductJpaEntity saved = jpaRepository.save(entity);

        Optional<ProductAggregate> found = productRepository.findById(new ProductId(saved.getId()));

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId().value());
        assertEquals("LAP001", found.get().getCode().value());
        assertEquals("Laptop", found.get().getName().value());
        assertEquals(new BigDecimal("999"), found.get().getPrice().value());
    }

    @Test
    void shouldReturnEmptyWhenNotFoundById() {
        Optional<ProductAggregate> found = productRepository.findById(new ProductId(999L));

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindProductByCode() {
        ProductJpaEntity entity = createEntity("LAP001", "Laptop", 999);
        jpaRepository.save(entity);

        Optional<ProductAggregate> found = productRepository.findByCode(new ProductCode("LAP001"));

        assertTrue(found.isPresent());
        assertEquals("LAP001", found.get().getCode().value());
        assertEquals("Laptop", found.get().getName().value());
    }

    @Test
    void shouldReturnEmptyWhenNotFoundByCode() {
        Optional<ProductAggregate> found = productRepository.findByCode(new ProductCode("UNKNOWN"));

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindAllWithoutFilters() {
        createTestProducts(3);

        List<ProductAggregate> products = productRepository.findAll(Map.of());

        assertEquals(3, products.size());
    }

    @Test
    void shouldCountAllWithoutFilters() {
        createTestProducts(3);

        long count = productRepository.countAll(Map.of());

        assertEquals(3L, count);
    }

    @Test
    void shouldFindAllWithPagination() {
        createTestProducts(25);

        List<ProductAggregate> products = productRepository.findAll(0, 10, Map.of());

        assertEquals(10, products.size());
        assertEquals("CODE0", products.get(0).getCode().value());
    }

    @Test
    void shouldCountAllWithPaginationFiltersApplied() {
        createTestProducts(25);

        long count = productRepository.countAll(Map.of());

        assertEquals(25L, count);
    }

    @Test
    void shouldFilterByNameExactMatch() {
        createTestProduct("LAP001", "Gaming Laptop", 1000);
        createTestProduct("MON001", "Monitor", 500);

        List<ProductAggregate> products = productRepository.findAll(Map.of("name", "Gaming Laptop"));

        assertEquals(1, products.size());
        assertEquals("Gaming Laptop", products.get(0).getName().value());
    }

    @Test
    void shouldFilterByNameLike() {
        createTestProduct("LAP001", "Gaming Laptop", 1000);
        createTestProduct("MON001", "Monitor", 500);

        List<ProductAggregate> products = productRepository.findAll(Map.of("nameLike", "gaming"));

        assertEquals(1, products.size());
        assertEquals("Gaming Laptop", products.get(0).getName().value());
    }

    @Test
    void shouldFilterByCodeAlias() {
        createTestProduct("LAP001", "Gaming Laptop", 1000);
        createTestProduct("MON001", "Monitor", 500);

        List<ProductAggregate> products = productRepository.findAll(Map.of("code", "LAP001"));

        assertEquals(1, products.size());
        assertEquals("LAP001", products.get(0).getCode().value());
    }

    @Test
    void shouldFilterByBarcodeLike() {
        createTestProduct("LAP001", "Gaming Laptop", 1000);
        createTestProduct("MON001", "Monitor", 500);

        List<ProductAggregate> products = productRepository.findAll(Map.of("barcodeLike", "lap"));

        assertEquals(1, products.size());
        assertEquals("LAP001", products.get(0).getCode().value());
    }

    @Test
    void shouldFilterByPriceExactMatch() {
        createTestProduct("LAP001", "Gaming Laptop", 1000);
        createTestProduct("MON001", "Monitor", 500);

        List<ProductAggregate> products = productRepository.findAll(Map.of("price", "500"));

        assertEquals(1, products.size());
        assertEquals(new BigDecimal("500"), products.get(0).getPrice().value());
    }

    @Test
    void shouldSortByPriceAscending() {
        createTestProduct("LAP001", "Laptop", 1000);
        createTestProduct("LAP002", "Cheap Laptop", 500);
        createTestProduct("LAP003", "Premium Laptop", 2000);

        List<ProductAggregate> products = productRepository.findAll(0, 10, Map.of("sortBy", "price", "order", "asc"));

        assertEquals(3, products.size());
        assertEquals(new BigDecimal("500"), products.get(0).getPrice().value());
        assertEquals(new BigDecimal("1000"), products.get(1).getPrice().value());
        assertEquals(new BigDecimal("2000"), products.get(2).getPrice().value());
    }

    @Test
    void shouldSortByPriceDescending() {
        createTestProduct("LAP001", "Laptop", 1000);
        createTestProduct("LAP002", "Cheap Laptop", 500);
        createTestProduct("LAP003", "Premium Laptop", 2000);

        List<ProductAggregate> products = productRepository.findAll(0, 10, Map.of("sortBy", "price", "order", "desc"));

        assertEquals(3, products.size());
        assertEquals(new BigDecimal("2000"), products.get(0).getPrice().value());
        assertEquals(new BigDecimal("1000"), products.get(1).getPrice().value());
        assertEquals(new BigDecimal("500"), products.get(2).getPrice().value());
    }

    @Test
    void shouldDeleteProduct() {
        ProductAggregate saved = productRepository.save(createAggregate(null, "TEST001", "Test Product", 99));

        productRepository.deleteById(saved.getId());

        assertFalse(jpaRepository.existsById(saved.getId().value()));
    }

    private void createTestProducts(int count) {
        for (int index = 0; index < count; index++) {
            createTestProduct("CODE" + index, "Product " + index, 100 + index);
        }
    }

    private void createTestProduct(String code, String name, int price) {
        jpaRepository.save(createEntity(code, name, price));
    }

    private ProductJpaEntity createEntity(String code, String name, int price) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setCode(code);
        entity.setName(name);
        entity.setPrice(price);
        entity.setDescription("");
        entity.setCurrentStock(0);
        entity.setCriticalStock(0);
        return entity;
    }

    private ProductAggregate createAggregate(Long id, String code, String name, int price) {
        ProductAggregate aggregate = new ProductAggregate(
            id == null ? null : new ProductId(id),
            new ProductCode(code),
            new ProductName(name),
            new ProductPrice(new BigDecimal(price)),
            0,
            0
        );
        aggregate.updateStatus(ProductStatus.ACTIVE);
        return aggregate;
    }
}
