package org.trebol.jpa.services.crud.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.trebol.BackendApp;
import org.trebol.api.adapters.legacy.ProductLookupService;
import org.trebol.api.models.ImagePojo;
import org.trebol.api.models.ProductPojo;
import org.trebol.common.exceptions.BadInputException;
import org.trebol.jpa.entities.Image;
import org.trebol.jpa.entities.Product;
import org.trebol.jpa.repositories.ProductImagesRepository;
import org.trebol.jpa.repositories.ProductsRepository;
import org.trebol.jpa.services.conversion.ProductsConverterService;
import org.trebol.jpa.services.crud.ImagesCrudService;
import org.trebol.testing.ImagesTestHelper;
import org.trebol.testing.ProductsTestHelper;

import jakarta.persistence.EntityExistsException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = BackendApp.class)
class ProductCreateImageRollbackTest {

    @Autowired
    private ProductsCrudServiceImpl productsCrudService;

    @Autowired
    private ProductsRepository productsRepository;

    @MockBean
    private ProductsConverterService productsConverterService;
    @MockBean
    private ProductImagesRepository productImagesRepository;
    @MockBean
    private ImagesCrudService imagesCrudService;
    @MockBean
    private ProductLookupService productLookupService;

    private final ProductsTestHelper productsHelper = new ProductsTestHelper();
    private final ImagesTestHelper imagesHelper = new ImagesTestHelper();

    @BeforeEach
    void setUp() {
        productsHelper.resetProducts();
        imagesHelper.resetImages();
        productsRepository.deleteAll();
    }

    @Test
    void createRollsBackProductWhenImagePersistenceFails() throws BadInputException, EntityExistsException {
        ProductPojo input = productsHelper.productPojoBeforeCreationWithoutCategory();
        input.setImages(List.of(imagesHelper.imagePojoBeforeCreation()));

        Product preparedEntity = productsHelper.productEntityBeforeCreationWithoutCategory();
        Image linkedImage = imagesHelper.imageEntityAfterCreation();

        when(productLookupService.findPojoByBarcode(anyString())).thenReturn(Optional.empty());
        when(productsConverterService.convertToNewEntity(any(ProductPojo.class))).thenReturn(preparedEntity);
        when(productsConverterService.convertToPojo(any(Product.class))).thenReturn(input);
        when(imagesCrudService.getExisting(any(ImagePojo.class))).thenReturn(Optional.of(linkedImage));
        when(productImagesRepository.saveAll(anyCollection())).thenThrow(new RuntimeException("image persistence failed"));

        assertThrows(RuntimeException.class, () -> productsCrudService.create(input));

        assertTrue(productsRepository.findByBarcode(input.getBarcode()).isEmpty());
    }
}
