package com.example.backendapi.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        String description,
        BigDecimal price,
        Integer quantity,
        Instant createdAt,
        Instant updatedAt
) {
    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(), product.getName(), product.getSku(), product.getDescription(),
                product.getPrice(), product.getQuantity(), product.getCreatedAt(), product.getUpdatedAt()
        );
    }
}
