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
import org.springframework.data.domain.Example;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.trebol.api.ExceptionsControllerAdvice;
import org.trebol.api.models.ProductPojo;
import org.trebol.api.services.PaginationService;
import org.trebol.jpa.entities.Product;
import org.trebol.jpa.entities.ProductList;
import org.trebol.jpa.entities.ProductListItem;
import org.trebol.jpa.repositories.ProductListItemsRepository;
import org.trebol.jpa.repositories.ProductListsRepository;
import org.trebol.jpa.services.SortSpecParserService;
import org.trebol.jpa.services.conversion.ProductListItemsConverterService;
import org.trebol.jpa.services.crud.ProductsCrudService;
import org.trebol.jpa.services.predicates.ProductListItemsPredicateService;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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
class DataProductListContentsControllerContractTest {

    @Mock PaginationService paginationServiceMock;
    @Mock SortSpecParserService sortServiceMock;
    @Mock ProductListItemsRepository listItemsRepositoryMock;
    @Mock ProductListsRepository listsRepositoryMock;
    @Mock ProductListItemsPredicateService listItemsPredicateServiceMock;
    @Mock ProductsCrudService productsCrudServiceMock;
    @Mock ProductListItemsConverterService listItemConverterServiceMock;

    private MockMvc mockMvc;
    private ProductList listExample;
    private ProductListItem listItemExample;

    @BeforeEach
    void setUp() {
        DataProductListContentsController controller = new DataProductListContentsController(
            paginationServiceMock,
            sortServiceMock,
            listItemsRepositoryMock,
            listsRepositoryMock,
            listItemsPredicateServiceMock,
            productsCrudServiceMock,
            listItemConverterServiceMock
        );

        Product productExample = Product.builder().id(1L).barcode("barcode-1").name("Product").price(1000).build();
        listExample = ProductList.builder().id(1L).name("List").code("list-1").build();
        listItemExample = ProductListItem.builder().list(listExample).product(productExample).build();

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new ExceptionsControllerAdvice())
            .build();
    }

    @Test
    void get_product_list_contents_returns_ok_with_paged_shape() throws Exception {
        when(listsRepositoryMock.findOne(any(Predicate.class))).thenReturn(Optional.of(listExample));
        when(paginationServiceMock.determineRequestedPageIndex(anyMap())).thenReturn(0);
        when(paginationServiceMock.determineRequestedPageSize(anyMap())).thenReturn(10);
        when(listItemsPredicateServiceMock.parseMap(anyMap())).thenReturn(new BooleanBuilder());
        when(listItemsRepositoryMock.findAll(nullable(Predicate.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(listItemExample)));
        when(listItemConverterServiceMock.convertToPojo(any(ProductListItem.class)))
            .thenReturn(ProductPojo.builder().name("Product").barcode("barcode-1").price(1000).build());
        when(listItemsRepositoryMock.count(any(Predicate.class))).thenReturn(1L);

        mockMvc.perform(get("/data/product_list_contents").queryParam("listCode", "list-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items[0].barcode").value("barcode-1"))
            .andExpect(jsonPath("$.pageIndex").value(0))
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void post_product_list_contents_with_valid_body_returns_created() throws Exception {
        when(listsRepositoryMock.findOne(any(Predicate.class))).thenReturn(Optional.of(listExample));
        when(productsCrudServiceMock.getExisting(any(ProductPojo.class)))
            .thenReturn(Optional.of(listItemExample.getProduct()));
        when(listItemsRepositoryMock.exists(any(Example.class))).thenReturn(false);
        when(listItemsRepositoryMock.save(any(ProductListItem.class))).thenReturn(listItemExample);

        String body = """
            {
              "name": "Product",
              "barcode": "barcode-1",
              "price": 1000
            }
            """;

        mockMvc.perform(post("/data/product_list_contents")
                .queryParam("listCode", "list-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());
    }

    @Test
    void put_product_list_contents_with_valid_body_returns_no_content() throws Exception {
        when(listsRepositoryMock.findOne(any(Predicate.class))).thenReturn(Optional.of(listExample));
        when(productsCrudServiceMock.getExisting(any(ProductPojo.class)))
            .thenReturn(Optional.of(listItemExample.getProduct()));
        when(listItemsRepositoryMock.exists(any(Example.class))).thenReturn(false);
        doNothing().when(listItemsRepositoryMock).deleteByListId(eq(1L));

        String body = """
            [
              {
                "name": "Product",
                "barcode": "barcode-1",
                "price": 1000
              }
            ]
            """;

        mockMvc.perform(put("/data/product_list_contents")
                .queryParam("listCode", "list-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_product_list_contents_with_filters_returns_no_content() throws Exception {
        when(listsRepositoryMock.findOne(any(Predicate.class))).thenReturn(Optional.of(listExample));
        when(listItemsPredicateServiceMock.parseMap(anyMap())).thenReturn(new BooleanBuilder());
        when(listItemsRepositoryMock.findAll(any(Predicate.class))).thenReturn(List.of(listItemExample));
        doNothing().when(listItemsRepositoryMock).deleteAll(any(Iterable.class));

        mockMvc.perform(delete("/data/product_list_contents")
                .queryParam("listCode", "list-1")
                .queryParam("barcode", "barcode-1"))
            .andExpect(status().isNoContent());

        verify(listItemsRepositoryMock).deleteAll(any(Iterable.class));
    }

    @Test
    void missing_list_code_returns_bad_request_with_rejected_code() throws Exception {
        mockMvc.perform(get("/data/product_list_contents"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("REJECTED_01"));
    }
}
