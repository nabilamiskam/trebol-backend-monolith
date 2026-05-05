package org.trebol.product.adapter.inbound.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.trebol.product.adapter.inbound.dto.PagedProductResponse;
import org.trebol.product.adapter.inbound.dto.ProductResponse;
import org.trebol.product.application.query.ListProductsQuery;
import org.trebol.product.application.query.GetProductQuery;
import org.trebol.product.application.result.PagedProductResult;
import org.trebol.product.application.result.ProductResult;
import org.trebol.product.application.service.ProductApplicationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductApplicationService productApplicationService;

    @MockBean
    private ProductWebMapper productWebMapper;

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void shouldReturnProductWhenFound() throws Exception {
        ProductResult result = new ProductResult(1L, "PROD-1", "Product 1", 99.99, true);
        ProductResponse response = new ProductResponse();
        response.id = 1L;
        response.code = "PROD-1";
        response.name = "Product 1";
        response.price = 99.99;
        response.isActive = true;

        when(productApplicationService.execute(any(GetProductQuery.class))).thenReturn(result);
        when(productWebMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(get("/product-module/1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.code").value("PROD-1"))
            .andExpect(jsonPath("$.name").value("Product 1"))
            .andExpect(jsonPath("$.price").value(99.99))
            .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(productApplicationService.execute(any(GetProductQuery.class))).thenReturn(null);

        mockMvc.perform(get("/product-module/999").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void shouldReturnPagedProducts() throws Exception {
        ProductResult first = new ProductResult(1L, "PROD-1", "Product 1", 99.99, true);
        ProductResult second = new ProductResult(2L, "PROD-2", "Product 2", 49.99, false);
        PagedProductResult result = new PagedProductResult(List.of(first, second), 2L);

        ProductResponse firstResponse = new ProductResponse();
        firstResponse.id = 1L;
        firstResponse.code = "PROD-1";
        firstResponse.name = "Product 1";
        firstResponse.price = 99.99;
        firstResponse.isActive = true;

        ProductResponse secondResponse = new ProductResponse();
        secondResponse.id = 2L;
        secondResponse.code = "PROD-2";
        secondResponse.name = "Product 2";
        secondResponse.price = 49.99;
        secondResponse.isActive = false;

        PagedProductResponse pagedResponse = new PagedProductResponse();
        pagedResponse.items = List.of(firstResponse, secondResponse);
        pagedResponse.totalCount = 2L;

        when(productApplicationService.execute(any(ListProductsQuery.class))).thenReturn(result);
        when(productWebMapper.toPagedResponse(result)).thenReturn(pagedResponse);

        mockMvc.perform(get("/product-module?pageIndex=0&pageSize=10").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items[0].id").value(1L))
            .andExpect(jsonPath("$.items[1].code").value("PROD-2"))
            .andExpect(jsonPath("$.totalCount").value(2L));
    }
}
