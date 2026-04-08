/*
 * Copyright (c) 2020-2024 The Trebol eCommerce Project
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.trebol.api.controllers;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.trebol.api.ExceptionsControllerAdvice;
import org.trebol.api.models.DataPagePojo;
import org.trebol.api.models.ProductPojo;
import org.trebol.api.services.PaginationService;
import org.trebol.jpa.services.SortSpecParserService;
import org.trebol.jpa.services.crud.ProductsCrudService;
import org.trebol.jpa.services.predicates.ProductsPredicateService;
import org.trebol.product.application.usecase.ListProductsUseCase;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DataProductsControllerContractTest {

    @Mock PaginationService paginationServiceMock;
    @Mock SortSpecParserService sortServiceMock;
    @Mock ProductsCrudService crudServiceMock;
    @Mock ProductsPredicateService predicateServiceMock;
    @Mock ListProductsUseCase listProductsUseCaseMock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DataProductsController controller = new DataProductsController(
            paginationServiceMock,
            sortServiceMock,
            crudServiceMock,
            predicateServiceMock,
            listProductsUseCaseMock
        );

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new ExceptionsControllerAdvice())
            .build();
    }

    @Test
    void get_products_returns_ok_with_paged_shape() throws Exception {
        when(paginationServiceMock.determineRequestedPageIndex(anyMap())).thenReturn(0);
        when(paginationServiceMock.determineRequestedPageSize(anyMap())).thenReturn(10);
        
        // Mock the new ListProductsUseCase
        org.trebol.product.application.result.PagedProductResult emptyResult = 
            new org.trebol.product.application.result.PagedProductResult(List.of(), 0);
        when(listProductsUseCaseMock.execute(any(org.trebol.product.application.query.ListProductsQuery.class)))
            .thenReturn(emptyResult);

        mockMvc.perform(get("/data/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.pageIndex").value(0))
            .andExpect(jsonPath("$.totalCount").value(0))
            .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void post_products_with_valid_body_returns_created() throws Exception {
        ProductPojo created = ProductPojo.builder()
            .name("Product")
            .barcode("123456789")
            .price(1000)
            .build();
        when(crudServiceMock.create(any(ProductPojo.class))).thenReturn(created);

        String body = """
            {
              "name": "Product",
              "barcode": "123456789",
              "price": 1000
            }
            """;

        mockMvc.perform(post("/data/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());
    }

    @Test
    void put_products_with_filters_returns_no_content() throws Exception {
        Predicate predicate = new BooleanBuilder();
        when(predicateServiceMock.parseMap(anyMap())).thenReturn(predicate);
        when(crudServiceMock.update(any(ProductPojo.class), eq(predicate)))
            .thenReturn(Optional.of(ProductPojo.builder().build()));

        String body = """
            {
              "name": "Updated",
              "barcode": "123456789",
              "price": 1200
            }
            """;

        mockMvc.perform(put("/data/products")
                .queryParam("barcode", "123456789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNoContent());
    }

    @Test
    void put_products_without_filters_returns_bad_request_with_rejected_code() throws Exception {
        String body = """
            {
              "name": "Updated",
              "barcode": "123456789",
              "price": 1200
            }
            """;

        mockMvc.perform(put("/data/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("REJECTED_01"));
    }

    @Test
    void delete_products_with_filters_returns_no_content() throws Exception {
        Predicate predicate = new BooleanBuilder();
        when(predicateServiceMock.parseMap(anyMap())).thenReturn(predicate);
        doNothing().when(crudServiceMock).delete(eq(predicate));

        mockMvc.perform(delete("/data/products").queryParam("barcode", "123456789"))
            .andExpect(status().isNoContent());

        verify(crudServiceMock).delete(eq(predicate));
    }
}
