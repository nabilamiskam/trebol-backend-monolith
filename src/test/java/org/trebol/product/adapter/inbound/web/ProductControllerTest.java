package org.trebol.product.adapter.inbound.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.trebol.product.adapter.inbound.dto.PagedProductResponse;
import org.trebol.product.adapter.inbound.dto.ProductRequest;
import org.trebol.product.adapter.inbound.dto.ProductResponse;
import org.trebol.product.application.command.CreateProductCommand;
import org.trebol.product.application.command.UpdateProductCommand;
import org.trebol.product.application.command.DeleteProductCommand;
import org.trebol.product.application.query.ListProductsQuery;
import org.trebol.product.application.query.GetProductQuery;
import org.trebol.product.application.result.PagedProductResult;
import org.trebol.product.application.result.ProductResult;
import org.trebol.product.application.service.ProductApplicationService;
import org.trebol.product.domain.exception.ProductCodeAlreadyExistsException;
import org.trebol.product.domain.exception.ProductNotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    @WithMockUser
    void shouldCreateProductSuccessfully() throws Exception {
        ProductRequest request = new ProductRequest();
        request.code = "NEW-PROD";
        request.name = "New Product";
        request.price = 199.99;
        request.isActive = true;

        ProductResult result = new ProductResult(3L, "NEW-PROD", "New Product", 199.99, true);
        ProductResponse response = new ProductResponse();
        response.id = 3L;
        response.code = "NEW-PROD";
        response.name = "New Product";
        response.price = 199.99;
        response.isActive = true;

        when(productApplicationService.execute(any(CreateProductCommand.class))).thenReturn(result);
        when(productWebMapper.toResponse(result)).thenReturn(response);

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/product-module")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").value(3L))
            .andExpect(jsonPath("$.code").value("NEW-PROD"))
            .andExpect(jsonPath("$.name").value("New Product"));
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenValidationFails() throws Exception {
        ProductRequest request = new ProductRequest();
        request.code = ""; // Empty code - should trigger validation error
        request.name = "New Product";
        request.price = 199.99;
        request.isActive = true;

        when(productWebMapper.toCreateCommand(request))
            .thenThrow(new IllegalArgumentException("Code cannot be blank"));

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/product-module")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldReturn409WhenCodeAlreadyExists() throws Exception {
        ProductRequest request = new ProductRequest();
        request.code = "EXISTING-CODE";
        request.name = "New Product";
        request.price = 199.99;
        request.isActive = true;

        when(productWebMapper.toCreateCommand(request))
            .thenReturn(new CreateProductCommand("EXISTING-CODE", "New Product", BigDecimal.valueOf(199.99), true));
        when(productApplicationService.execute(any(CreateProductCommand.class)))
            .thenThrow(new ProductCodeAlreadyExistsException("Product code already exists: EXISTING-CODE"));

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/product-module")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .with(csrf()))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void shouldUpdateProductSuccessfully() throws Exception {
        ProductRequest request = new ProductRequest();
        request.name = "Updated Product";
        request.price = 249.99;
        request.isActive = false;

        ProductResult result = new ProductResult(1L, "PROD-1", "Updated Product", 249.99, false);
        ProductResponse response = new ProductResponse();
        response.id = 1L;
        response.code = "PROD-1";
        response.name = "Updated Product";
        response.price = 249.99;
        response.isActive = false;

        when(productWebMapper.toUpdateCommand(1L, request))
            .thenReturn(new UpdateProductCommand(1L, "Updated Product", BigDecimal.valueOf(249.99), false));
        when(productApplicationService.execute(any(UpdateProductCommand.class))).thenReturn(result);
        when(productWebMapper.toResponse(result)).thenReturn(response);

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/product-module/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Updated Product"))
            .andExpect(jsonPath("$.price").value(249.99))
            .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @WithMockUser
    void shouldReturn404OnUpdateWhenProductNotFound() throws Exception {
        ProductRequest request = new ProductRequest();
        request.name = "Updated Product";
        request.price = 249.99;
        request.isActive = false;

        when(productWebMapper.toUpdateCommand(999L, request))
            .thenReturn(new UpdateProductCommand(999L, "Updated Product", BigDecimal.valueOf(249.99), false));
        when(productApplicationService.execute(any(UpdateProductCommand.class)))
            .thenThrow(new ProductNotFoundException("Product not found with id: 999"));

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/product-module/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldDeleteProductSuccessfully() throws Exception {
        when(productWebMapper.toDeleteCommand(1L))
            .thenReturn(new DeleteProductCommand(1L));
        doNothing().when(productApplicationService).execute(any(DeleteProductCommand.class));

        mockMvc.perform(delete("/product-module/1")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void shouldReturn404OnDeleteWhenProductNotFound() throws Exception {
        when(productWebMapper.toDeleteCommand(999L))
            .thenReturn(new DeleteProductCommand(999L));
        doThrow(new ProductNotFoundException("Product not found with id: 999"))
            .when(productApplicationService).execute(any(DeleteProductCommand.class));

        mockMvc.perform(delete("/product-module/999")
                .with(csrf()))
            .andExpect(status().isNotFound());
    }
}
