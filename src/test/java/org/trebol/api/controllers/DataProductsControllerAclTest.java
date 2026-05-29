package org.trebol.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.trebol.api.models.ProductPojo;
import org.trebol.api.adapters.legacy.ProductLookupService;
import org.trebol.jpa.entities.Product;
import org.trebol.jpa.repositories.ProductImagesRepository;
import org.trebol.jpa.repositories.ProductsRepository;
import org.trebol.jpa.services.SortSpecParserService;
import org.trebol.jpa.services.crud.ImagesCrudService;
import org.trebol.jpa.services.crud.ProductsCrudService;
import org.trebol.jpa.services.crud.impl.ProductsCrudServiceImpl;
import org.trebol.jpa.services.conversion.ProductsConverterService;
import org.trebol.jpa.services.patch.ProductsPatchService;
import org.trebol.api.services.PaginationService;
import org.trebol.jpa.services.predicates.ProductsPredicateService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(DataProductsController.class)
@Import(ProductsCrudServiceImpl.class)
class DataProductsControllerAclTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PaginationService paginationService;
    @MockBean
    SortSpecParserService sortService;
    @MockBean
    ProductsPredicateService predicateService;

    // Mocks for imported ProductsCrudServiceImpl
    @MockBean
    ProductsRepository productsRepository;
    @MockBean
    ProductsConverterService productsConverterService;
    @MockBean
    ProductsPatchService productsPatchService;
    @MockBean
    ProductImagesRepository productImagesRepository;
    @MockBean
    ImagesCrudService imagesCrudService;
    @MockBean
    ProductLookupService productLookupService;

    @Test
    @WithMockUser(authorities = "products:create")
    void create_returns400WhenAclReportsExisting() throws Exception {
        ProductPojo input = ProductPojo.builder().barcode("EXIST-1").name("Existing").price(100).build();

        when(productLookupService.findPojoByBarcode(anyString())).thenReturn(Optional.of(input));

        mockMvc.perform(post("/data/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(input))
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "products:create")
    void create_returns201WhenAclMisses() throws Exception {
        ProductPojo input = ProductPojo.builder().barcode("NEW-1").name("New").price(100).build();
        Product entity = Product.builder().id(1L).barcode("NEW-1").name("New").price(100).build();

        when(productLookupService.findPojoByBarcode(anyString())).thenReturn(Optional.empty());
        when(productsConverterService.convertToNewEntity(any(ProductPojo.class))).thenReturn(entity);
        when(productsRepository.saveAndFlush(any(Product.class))).thenReturn(entity);
        when(productsConverterService.convertToPojo(any(Product.class))).thenReturn(input);

        mockMvc.perform(post("/data/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(input))
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isCreated());
    }
}
