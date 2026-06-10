package com.example.backendapi.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @WithMockUser(username = "vijay@example.com")
    void createsProductAndReturnsLocation() throws Exception {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        when(productService.create(anyString(), any())).thenReturn(new ProductResponse(
                1L, "Mechanical Keyboard", "KEY-001", "Hot-swappable keyboard",
                new BigDecimal("89.99"), 25, now, now
        ));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mechanical Keyboard","sku":"KEY-001",
                                 "description":"Hot-swappable keyboard","price":89.99,"quantity":25}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/products/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("KEY-001"));
    }

    @Test
    @WithMockUser(username = "vijay@example.com")
    void rejectsInvalidProduct() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","sku":"","price":-1,"quantity":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.sku").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());
    }
}
