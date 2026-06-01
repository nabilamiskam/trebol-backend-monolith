package org.trebol.api.controllers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DataOrdersControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = {"orders:create", "orders:read", "orders:update"})
    void startPayment_success_returnsToken() throws Exception {
        long buyOrder = createOrderAndGetBuyOrder();

        mockMvc.perform(post("/data/orders/payment/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "buyOrder": %d }
                    """.formatted(buyOrder)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.buyOrder", is((int) buyOrder)))
            .andExpect(jsonPath("$.status", is("Payment Started")))
            .andExpect(jsonPath("$.token", not(isEmptyOrNullString())));
    }

    @Test
    @WithMockUser(authorities = {"orders:create", "orders:read", "orders:update"})
    void confirm_success_afterPaidUnconfirmed_returns204() throws Exception {
        long buyOrder = createOrderAndGetBuyOrder();

        // start payment -> Payment Started
        mockMvc.perform(post("/data/orders/payment/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "buyOrder": %d }
                    """.formatted(buyOrder)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", not(isEmptyOrNullString())));

        // mark as paid (bypass gateway) -> Paid, Unconfirmed
        mockMvc.perform(post("/data/orders/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "buyOrder": %d }
                    """.formatted(buyOrder)))
            .andExpect(status().isNoContent());

        // confirm -> 204
        mockMvc.perform(post("/data/orders/confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "buyOrder": %d }
                    """.formatted(buyOrder)))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = {"orders:create", "orders:read", "orders:update"})
    void confirm_invalidTransition_returns400() throws Exception {
        long buyOrder = createOrderAndGetBuyOrder();

        // invalid because order is NOT Paid, Unconfirmed yet
        mockMvc.perform(post("/data/orders/confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "buyOrder": %d }
                    """.formatted(buyOrder)))
            .andExpect(status().isBadRequest());
    }
    
@Test
@WithMockUser(authorities = {"orders:update"})
void startPayment_nonExistingOrder_returns404() throws Exception {
    mockMvc.perform(post("/data/orders/payment/start")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ \"buyOrder\": 999999999 }"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is("NOTFOUND_01")));
}

    // ----- helpers -----

    private long createOrderAndGetBuyOrder() throws Exception {
        // If this barcode doesn't exist in your seed data, replace it with one that does.
        String createOrderJson = """
            {
              "paymentType": "WebPay Plus",
              "billingType": "Bill",
              "customer": {
                "firstName": "Ana",
                "lastName": "Perez",
                "idNumber": "12345678-9",
                "email": "ana+it@rocketmail.com",
                "phone1": "912345678"
              },
              "details": [
                {
                  "units": 1,
                  "product": { "barcode": "MOU001" }
                }
              ]
            }
            """;

        mockMvc.perform(post("/data/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createOrderJson))
            .andExpect(status().isCreated());

        String latestResponse = mockMvc.perform(get("/data/orders")
                .queryParam("sortBy", "buyOrder")
                .queryParam("order", "desc")
                .queryParam("pageIndex", "0")
                .queryParam("pageSize", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].buyOrder", notNullValue()))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return extractFirstLong(latestResponse, "buyOrder");
    }

    private static long extractFirstLong(String json, String fieldName) {
        Pattern p = Pattern.compile("\\\"" + fieldName + "\\\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        if (!m.find()) {
            throw new AssertionError("Could not find numeric field '" + fieldName + "' in: " + json);
        }
        return Long.parseLong(m.group(1));
    }
}