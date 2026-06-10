package com.example.backendapi.product;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable public representation of a product.
 *
 * <p>It excludes the owner entity and persistence details, giving clients a stable API contract
 * while allowing the internal database model to evolve.</p>
 */
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
    /** Maps a persistence entity to its safe external representation. */
    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(), product.getName(), product.getSku(), product.getDescription(),
                product.getPrice(), product.getQuantity(), product.getCreatedAt(), product.getUpdatedAt()
        );
    }
}
