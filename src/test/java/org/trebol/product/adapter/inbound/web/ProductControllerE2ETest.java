package org.trebol.product.adapter.inbound.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.trebol.BackendApp;
import org.trebol.jpa.repositories.ProductListItemsRepository;
import org.trebol.product.adapter.outbound.persistence.ProductJpaRepository;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BackendApp.class)
@AutoConfigureMockMvc
class ProductControllerE2ETest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProductJpaRepository jpaRepository;

    @Autowired
    ProductListItemsRepository productListItemsRepository;

    @BeforeEach
    void clean() {
        productListItemsRepository.deleteAll();
        jpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void full_http_flow_create_get_list_update_delete() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("code", "E2E-1");
        request.put("name", "E2E Product");
        request.put("price", 19.95);
        request.put("isActive", true);

        // Create
        MvcResult createResult = mockMvc.perform(post("/product-module")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn();

        // List and read created id
        MvcResult listResult = mockMvc.perform(get("/product-module?pageIndex=0&pageSize=10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode listBody = objectMapper.readTree(listResult.getResponse().getContentAsString());
        JsonNode firstItem = listBody.path("items").get(0);
        long id = firstItem.path("id").asLong();
        assertThat(firstItem.path("code").asText()).isEqualTo("E2E-1");

        // Get single
        mockMvc.perform(get("/product-module/" + id).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        // Update
        Map<String, Object> update = new HashMap<>();
        update.put("name", "E2E Product Updated");
        update.put("price", 29.95);

        mockMvc.perform(put("/product-module/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update))
                .with(csrf()))
            .andExpect(status().isOk());

        // Delete
        mockMvc.perform(delete("/product-module/" + id).with(csrf()))
            .andExpect(status().isNoContent());

        // Ensure deleted
        mockMvc.perform(get("/product-module/" + id).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}
