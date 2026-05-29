package org.trebol.jpa.services.crud.impl;

import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trebol.api.models.ProductPojo;
import org.trebol.api.adapters.legacy.ProductLookupService;
import org.trebol.common.exceptions.BadInputException;
import org.trebol.jpa.entities.Product;
import org.trebol.jpa.repositories.ProductImagesRepository;
import org.trebol.jpa.repositories.ProductsRepository;
import org.trebol.jpa.services.conversion.ProductsConverterService;
import org.trebol.jpa.services.crud.ImagesCrudService;
import org.trebol.jpa.services.patch.ProductsPatchService;
import org.trebol.testing.ProductsTestHelper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductsCrudServiceImplAclTest {
    @InjectMocks
    ProductsCrudServiceImpl instance;

    @Mock
    ProductsRepository productsRepository;
    @Mock
    ProductsConverterService productsConverterService;
    @Mock
    ProductsPatchService productsPatchService;
    @Mock
    ProductImagesRepository productImagesRepository;
    @Mock
    ImagesCrudService imagesCrudService;
    @Mock
    ProductLookupService productLookupService;

    final ProductsTestHelper productsHelper = new ProductsTestHelper();

    @BeforeEach
    void setUp() {
        productsHelper.resetProducts();
    }

    @Test
    void readOne_prefersAclPojo() {
        Product foundEntity = productsHelper.productEntityAfterCreationWithoutCategory();
        ProductPojo aclPojo = productsHelper.productPojoAfterCreationWithoutCategory();

        when(productsRepository.findOne(any(Predicate.class))).thenReturn(Optional.of(foundEntity));
        when(productLookupService.findPojoById(foundEntity.getId())).thenReturn(Optional.of(aclPojo));

        ProductPojo result = instance.readOne(org.mockito.Mockito.mock(Predicate.class));

        assertNotNull(result);
        assertEquals(aclPojo.getBarcode(), result.getBarcode());
        assertEquals(aclPojo.getName(), result.getName());
    }

    @Test
    void getExisting_prefersAclBarcode() throws BadInputException {
        ProductPojo input = productsHelper.productPojoForFetch();
        ProductPojo aclPojo = productsHelper.productPojoAfterCreationWithoutCategory();

        when(productLookupService.findPojoByBarcode(anyString())).thenReturn(Optional.of(aclPojo));

        Optional<Product> result = instance.getExisting(input);

        assertTrue(result.isPresent());
        assertEquals(aclPojo.getBarcode(), result.get().getBarcode());
    }

    @Test
    void getExisting_fallsBackToRepositoryWhenAclMisses() throws BadInputException {
        ProductPojo input = productsHelper.productPojoForFetch();
        Product repoEntity = productsHelper.productEntityAfterCreationWithoutCategory();

        when(productLookupService.findPojoByBarcode(anyString())).thenReturn(Optional.empty());
        when(productsRepository.findByBarcode(anyString())).thenReturn(Optional.of(repoEntity));

        Optional<Product> result = instance.getExisting(input);

        assertTrue(result.isPresent());
        assertEquals(repoEntity, result.get());
    }
}
